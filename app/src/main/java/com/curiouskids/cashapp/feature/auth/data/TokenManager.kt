package com.curiouskids.cashapp.feature.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val DEVICE_ID = stringPreferencesKey("device_id")

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "auth_keyset", "auth_pref")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://auth_master_key")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    private fun encrypt(data: String): String {
        val encrypted = aead.encrypt(data.toByteArray(Charsets.UTF_8), null)
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    private fun decrypt(data: String?): String? {
        if (data == null) return null
        return try {
            val decoded = Base64.decode(data, Base64.DEFAULT)
            val decrypted = aead.decrypt(decoded, null)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveTokens(access: String, refresh: String) {
        context.authDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = encrypt(access)
            prefs[REFRESH_TOKEN] = encrypt(refresh)
        }
    }

    fun getAccessToken(): Flow<String?> = context.authDataStore.data.map { prefs ->
        decrypt(prefs[ACCESS_TOKEN])
    }

    fun getRefreshToken(): Flow<String?> = context.authDataStore.data.map { prefs ->
        decrypt(prefs[REFRESH_TOKEN])
    }

    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        var deviceId: String? = null
        context.authDataStore.edit { prefs ->
            val encryptedId = prefs[DEVICE_ID]
            deviceId = decrypt(encryptedId)
            if (deviceId == null) {
                deviceId = UUID.randomUUID().toString()
                prefs[DEVICE_ID] = encrypt(deviceId!!)
            }
        }
        return deviceId!!
    }
}
