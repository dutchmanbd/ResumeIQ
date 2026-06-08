package com.dutchman.resumeiq.domain.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File
import java.security.KeyStore

class SharedPref(
    private val context: Context,
    private val storageName: String = context.packageName
) {

    private val sharedPref: SharedPreferences
        get() = context.getSharedPreferences(
            storageName,
            Activity.MODE_PRIVATE
        )

    private val encryptedPref: SharedPreferences = createEncryptedPrefs()

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            buildEncryptedPrefs()
        } catch (e: Exception) {
            // Common causes: AEADBadTagException when the master key in the
            // Android Keystore no longer matches the encrypted prefs file
            // (e.g. after a backup/restore, keystore reset, or data corruption).
            // Recover by wiping the corrupted prefs + master key and recreating.
            Log.e(TAG, "Failed to open EncryptedSharedPreferences, resetting: ${e.message}", e)
            resetEncryptedPrefs()
            buildEncryptedPrefs()
        }
    }

    private fun buildEncryptedPrefs(): SharedPreferences {
        val mainKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            storageName,
            mainKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun resetEncryptedPrefs() {
        try {
            context.deleteSharedPreferences(storageName)
        } catch (e: Exception) {
            Log.w(TAG, "deleteSharedPreferences failed, removing file manually", e)
            val prefsFile = File(
                context.applicationInfo.dataDir,
                "shared_prefs/$storageName.xml"
            )
            if (prefsFile.exists()) {
                prefsFile.delete()
            }
        }
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete master key from keystore", e)
        }
    }

    fun read(key: String, defValue: String) = encryptedPref.getString(key, defValue)!!

    fun write(key: String, value: String) {
        encryptedPref.edit {
            putString(key, value)
        }
    }

    fun read(key: String, defValue: Boolean) =
        encryptedPref.getBoolean(key, defValue)

    fun write(key: String, value: Boolean) {
        encryptedPref.edit {
            putBoolean(key, value)
        }
    }

    fun read(key: String, defValue: Int) =
        encryptedPref.getInt(key, defValue)

    fun write(key: String, value: Int) {
        encryptedPref.edit {
            putInt(key, value)
        }
    }

    fun read(key: String, defValue: Long) =
        encryptedPref.getLong(key, defValue)

    fun write(key: String, value: Long) {
        encryptedPref.edit {
            putLong(key, value)
        }
    }

    fun read(key: String, defValue: Float) =
        encryptedPref.getFloat(key, defValue)

    fun write(key: String, value: Float) {
        encryptedPref.edit {
            putFloat(key, value)
        }
    }

    fun clear() {
        try {
            sharedPref.edit {
                clear()
            }
        } catch (e: Exception) {
            Log.e(TAG, "clear: ${e.localizedMessage}")
        }
    }

    private companion object {
        const val TAG = "SharedPref"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"

        // Default alias used by androidx.security MasterKeys.AES256_GCM_SPEC
        const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
    }
}
