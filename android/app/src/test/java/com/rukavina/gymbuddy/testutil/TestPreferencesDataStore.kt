package com.rukavina.gymbuddy.testutil

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File

/**
 * A plain JVM, temp-file-backed DataStore for tests - no Context needed.
 * AppPreferencesRepository takes a DataStore<Preferences> directly rather
 * than a Context precisely so this works: pulling in Robolectric just to
 * satisfy this one dependency would make JaCoCo blind to every class the
 * test touches (Robolectric's own classloader isn't visible to the
 * coverage agent - see ActiveWorkoutViewModelTest's file comment).
 */
fun testPreferencesDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        produceFile = { File.createTempFile("test_preferences", ".preferences_pb").apply { deleteOnExit() } }
    )
