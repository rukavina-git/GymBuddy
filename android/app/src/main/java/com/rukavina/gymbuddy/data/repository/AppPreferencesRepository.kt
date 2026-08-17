package com.rukavina.gymbuddy.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
