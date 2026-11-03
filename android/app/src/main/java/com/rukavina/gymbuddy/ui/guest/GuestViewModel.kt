package com.rukavina.gymbuddy.ui.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.DataOwnerStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where Splash goes, and the guest flag for Login (ADR 0014). */
@HiltViewModel
class GuestViewModel @Inject constructor(
    private val preferences: AppPreferencesRepository,
    private val owners: DataOwnerStore
) : ViewModel() {

    /**
     * Main if an account owns the data (signed in, or a dead session that
     * keeps its data) or the user chose guest mode; else Login.
     */
    suspend fun opensMain(): Boolean = startsInMain(owners.owner, preferences.isGuest.first())

    /** "Continue without account": the guest's file is already open while signed out. */
    fun continueAsGuest(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setGuest(true)
            onDone()
        }
    }
}

/** Splash's rule, apart for tests. */
fun startsInMain(owner: String?, isGuest: Boolean): Boolean = owner != null || isGuest
