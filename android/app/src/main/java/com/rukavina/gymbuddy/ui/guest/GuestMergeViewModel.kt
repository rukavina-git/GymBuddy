package com.rukavina.gymbuddy.ui.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.sync.DataOwnerStore
import com.rukavina.gymbuddy.data.sync.FirebaseAuthSession
import com.rukavina.gymbuddy.data.sync.GuestMergeCoordinator
import com.rukavina.gymbuddy.data.sync.GuestMergeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainScreen's view of the account switch: whether the open file belongs
 * to whoever is signed in, and the guest → account question (ADR 0014).
 */
@HiltViewModel
class GuestMergeViewModel @Inject constructor(
    private val coordinator: GuestMergeCoordinator,
    holder: DatabaseHolder,
    auth: FirebaseAuthSession,
    owners: DataOwnerStore
) : ViewModel() {

    /**
     * False between a sign-in (or a release) and the database switch. The
     * expected file is the signed-in uid's, or, with nobody signed in, the
     * data owner's (a dead session keeps its data, AuthGate). Screens wait, so
     * they never show the previous owner's file to the new one.
     */
    val ownerReady: StateFlow<Boolean> =
        combine(holder.ownerFlow, auth.signedInUid, owners.ownerFlow) { open, uid, owner -> open == (uid ?: owner) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, holder.owner == (auth.signedInUid.value ?: owners.owner))

    val state: StateFlow<GuestMergeState> = coordinator.state

    fun add() {
        viewModelScope.launch { coordinator.add() }
    }

    fun cancel() {
        viewModelScope.launch { coordinator.cancel() }
    }

    fun dismiss() = coordinator.dismiss()
}
