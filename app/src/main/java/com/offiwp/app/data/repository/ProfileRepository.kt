package com.offiwp.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.offiwp.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_profile_prefs")

class ProfileRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_STATUS = stringPreferencesKey("user_status")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
        val MAC_ADDRESS = stringPreferencesKey("mac_address")
        val IS_DISCOVERABLE = booleanPreferencesKey("is_discoverable")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.USER_NAME] ?: "Offi User",
            status = prefs[Keys.USER_STATUS] ?: "Hey there! I am using Offi Wp offline.",
            avatarUri = prefs[Keys.AVATAR_URI],
            macAddress = prefs[Keys.MAC_ADDRESS] ?: "02:00:00:00:00:00",
            isDiscoverable = prefs[Keys.IS_DISCOVERABLE] ?: true
        )
    }

    suspend fun updateProfile(name: String, status: String, avatarUri: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_STATUS] = status
            if (avatarUri != null) {
                prefs[Keys.AVATAR_URI] = avatarUri
            }
        }
    }

    suspend fun setDiscoverable(discoverable: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DISCOVERABLE] = discoverable
        }
    }

    suspend fun updateMacAddress(mac: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAC_ADDRESS] = mac
        }
    }
}
