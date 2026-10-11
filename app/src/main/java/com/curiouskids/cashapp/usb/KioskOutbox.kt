package com.curiouskids.cashapp.usb

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OutboxNote(
    val id: String,
    val sessionId: String,
    val sequence: Int,
    val noteValue: Int, // 5, 10, 20, 50, 100, 200, 500
    val idempotencyKey: String,
    val isAcknowledged: Boolean = false
)

@Singleton
class KioskOutbox @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kiosk_outbox_prefs", Context.MODE_PRIVATE)

    @Synchronized
    fun saveNote(note: OutboxNote) {
        val currentList = getUnacknowledgedNotes().toMutableList()
        currentList.removeAll { it.idempotencyKey == note.idempotencyKey }
        currentList.add(note)

        val jsonStr = json.encodeToString(currentList)
        prefs.edit().putString("unacknowledged_notes", jsonStr).apply()
    }

    @Synchronized
    fun markAcknowledged(idempotencyKey: String) {
        val currentList = getUnacknowledgedNotes().toMutableList()
        currentList.removeAll { it.idempotencyKey == idempotencyKey }

        val jsonStr = json.encodeToString(currentList)
        prefs.edit().putString("unacknowledged_notes", jsonStr).apply()
    }

    @Synchronized
    fun getUnacknowledgedNotes(): List<OutboxNote> {
        val jsonStr = prefs.getString("unacknowledged_notes", null) ?: return emptyList()
        return try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
