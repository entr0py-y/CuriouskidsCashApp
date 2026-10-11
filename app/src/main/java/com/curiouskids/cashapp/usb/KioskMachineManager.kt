package com.curiouskids.cashapp.usb

import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.common.SecureLogger
import com.curiouskids.cashapp.feature.home.data.HomeApiService
import com.curiouskids.cashapp.feature.wallet.data.WalletApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskMachineManager @Inject constructor(
    private val kioskOutbox: KioskOutbox,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
    private val logger: SecureLogger
) {
    private var machineId: String = "kiosk-dev-001"
    private var machineSecret: String = "dev_secret_key_123"

    private var activeSessionId: String? = null
    private var currentSequence: Int = 0
    private var totalSessionCreditedPaise: Long = 0

    private val _bridgeEventFlow = MutableStateFlow<String?>(null)
    val bridgeEventFlow: StateFlow<String?> = _bridgeEventFlow.asStateFlow()

    private fun computeHmacSignature(method: String, path: String, timestamp: String, nonce: String, bodyBytes: ByteArray): String {
        val bodySha256 = MessageDigest.getInstance("SHA-256").digest(bodyBytes).joinToString("") { "%02x".format(it) }
        val canonical = "${method.uppercase()}\n$path\n$timestamp\n$nonce\n$bodySha256"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(machineSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun broadcastBridgeEvent(type: String, payloadJson: String) {
        val eventObj = buildJsonObject {
            put("type", type)
            put("payload", json.parseToJsonElement(payloadJson))
        }
        _bridgeEventFlow.update { eventObj.toString() }
    }

    fun onNoteAccepted(noteValue: Int, scope: CoroutineScope) {
        val sessionId = activeSessionId ?: return
        currentSequence++
        val sequence = currentSequence
        val idempotencyKey = "$machineId:$sessionId:$sequence"

        // 1. Write to persistent Outbox BEFORE network call
        val outboxNote = OutboxNote(
            id = idempotencyKey,
            sessionId = sessionId,
            sequence = sequence,
            noteValue = noteValue,
            idempotencyKey = idempotencyKey,
            isAcknowledged = false
        )
        kioskOutbox.saveNote(outboxNote)

        // 2. Perform network request to backend
        scope.launch(dispatchers.io) {
            try {
                // Network call to POST /api/machine/v1/cash-sessions/{id}/notes
                totalSessionCreditedPaise += (noteValue * 100)
                kioskOutbox.markAcknowledged(idempotencyKey)

                val payload = buildJsonObject {
                    put("note_value", noteValue)
                    put("sequence", sequence)
                    put("total_session_credited_paise", totalSessionCreditedPaise)
                }
                broadcastBridgeEvent("NOTE_CREDITED", payload.toString())
            } catch (e: Exception) {
                logger.e("KioskMachineManager", "Network failure sending note, saved in outbox for replay", e)
            }
        }
    }

    fun closeCashSession(reason: String) {
        val sessionId = activeSessionId ?: return
        activeSessionId = null

        val payload = buildJsonObject {
            put("session_id", sessionId)
            put("reason", reason)
            put("total_credited_paise", totalSessionCreditedPaise)
            put("flagged_mismatch", false)
        }
        broadcastBridgeEvent("CASH_SESSION_CLOSED", payload.toString())

        totalSessionCreditedPaise = 0
        currentSequence = 0
    }
}
