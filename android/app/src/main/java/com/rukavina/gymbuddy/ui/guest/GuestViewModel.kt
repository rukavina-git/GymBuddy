package com.rukavina.gymbuddy.ui.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The guest flag for Splash and Login (ADR 0014). */
@HiltViewModel
class GuestViewModel @Inject constructor(
    private val preferences: AppPreferencesRepository
) : ViewModel() {

    suspend fun isGuest(): Boolean = preferences.isGuest.first()

    /** "Continue without account": the guest's file is already open while signed out. */
    fun continueAsGuest(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setGuest(true)
            onDone()
        }
    }
}
