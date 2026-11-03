package com.rukavina.gymbuddy.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Takes the DataStore instance itself, not a Context - AppModule provides
 * it via PreferenceDataStoreFactory.create(). This is what makes this
 * repository (and every ViewModel that depends on it) testable on the JVM
 * with a plain temp-file-backed DataStore, no Robolectric/Context required.
 */
@Singleton
class AppPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val SHOW_QUOTE_OF_THE_DAY = booleanPreferencesKey("show_quote_of_the_day")
        val PREFERRED_UNITS = stringPreferencesKey("preferred_units")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val LAST_SIGN_IN_EMAIL = stringPreferencesKey("last_sign_in_email")
        val PENDING_CONSENT_AT = longPreferencesKey("pending_consent_at")
    }

    /** The address of the last email sign-in, filled in next time (Figma 114:20520). Phone only. */
    val lastSignInEmail: Flow<String?> = dataStore.data.map { it[PreferencesKeys.LAST_SIGN_IN_EMAIL] }

    suspend fun setLastSignInEmail(email: String) {
        dataStore.edit { it[PreferencesKeys.LAST_SIGN_IN_EMAIL] = email }
    }

    /**
     * When the user ticked the health-data box on Create account, until
     * ConsentRecorder writes it onto the new account's profile.
     */
    val pendingConsentAt: Flow<Long?> = dataStore.data.map { it[PreferencesKeys.PENDING_CONSENT_AT] }

    suspend fun setPendingConsentAt(at: Long?) {
        dataStore.edit {
            if (at == null) it.remove(PreferencesKeys.PENDING_CONSENT_AT) else it[PreferencesKeys.PENDING_CONSENT_AT] = at
        }
    }

    /**
     * The user chose "Continue without account" (ADR 0014). Cleared when
     * the guest's data merges into an account or is erased.
     */
    val isGuest: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.IS_GUEST] ?: false }

    suspend fun setGuest(guest: Boolean) {
        dataStore.edit { it[PreferencesKeys.IS_GUEST] = guest }
    }

    val showQuoteOfTheDay: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_QUOTE_OF_THE_DAY] ?: true // Default: enabled
        }

    suspend fun setShowQuoteOfTheDay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_QUOTE_OF_THE_DAY] = enabled
        }
    }

    /**
     * Display units are a device preference, not account data: the same
     * account on two devices can reasonably want different units, and the
     * underlying stored measurements are always metric regardless of this
     * setting - see UserProfile.weight/height/targetWeight.
     */
    val preferredUnits: Flow<PreferredUnits> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PREFERRED_UNITS]?.let {
                try {
                    PreferredUnits.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    PreferredUnits.METRIC
                }
            } ?: PreferredUnits.METRIC
        }

    suspend fun setPreferredUnits(units: PreferredUnits) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_UNITS] = units.name
        }
    }
}
