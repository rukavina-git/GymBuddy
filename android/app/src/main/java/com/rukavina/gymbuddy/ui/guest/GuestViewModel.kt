package com.rukavina.gymbuddy.ui.guest

import androidx.lifecycle.ViewModel
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.DataOwnerStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Where Splash goes (ADR 0014). Setup sets the guest flag (OnboardingViewModel). */
@HiltViewModel
class GuestViewModel @Inject constructor(
    private val preferences: AppPreferencesRepository,
    private val owners: DataOwnerStore
) : ViewModel() {

    /**
     * Main if an account owns the data (signed in, or a dead session that
     * keeps its data) or the user chose guest mode; else Welcome.
     */
    suspend fun opensMain(): Boolean = startsInMain(owners.owner, preferences.isGuest.first())
}

/** Splash's rule, apart for tests. */
fun startsInMain(owner: String?, isGuest: Boolean): Boolean = owner != null || isGuest
