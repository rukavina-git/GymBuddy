package com.rukavina.gymbuddy.ui.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.sync.AuthGate
import com.rukavina.gymbuddy.data.sync.AuthGateState
import com.rukavina.gymbuddy.data.sync.GuestMerge
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** MainScreen's banner and dialog for a dead session (AuthGate, step 03). */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val gate: AuthGate,
    private val merge: GuestMerge
) : ViewModel() {

    val state: StateFlow<AuthGateState> = gate.state

    private val _counts = MutableStateFlow<LocalDataCounts?>(null)
    /** What's on the phone for the deleted account, for the dialog. */
    val counts: StateFlow<LocalDataCounts?> = _counts

    private val _erased = MutableSharedFlow<Unit>()
    /** After Erase: back to Login. */
    val erased: SharedFlow<Unit> = _erased.asSharedFlow()

    fun loadCounts() {
        viewModelScope.launch { _counts.value = merge.openCounts() }
    }

    fun keep() {
        viewModelScope.launch { gate.keepAsGuest() }
    }

    fun erase() {
        viewModelScope.launch {
            gate.eraseDeletedAccount()
            _erased.emit(Unit)
        }
    }
}
