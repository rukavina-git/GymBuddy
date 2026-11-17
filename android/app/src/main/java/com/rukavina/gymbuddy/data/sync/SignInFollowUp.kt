package com.rukavina.gymbuddy.data.sync

import com.rukavina.gymbuddy.data.repository.ConsentRecorder

/** What [AuthGate] runs once an account owns the data (its `afterSignIn`). */
class SignInFollowUp(
    private val signedOutPrompt: SignedOutPrompt,
    private val coordinator: GuestMergeCoordinator,
    private val consent: ConsentRecorder
) {
    suspend fun run(uid: String) {
        signedOutPrompt.reset() // Plan 9 step 04a: signed in again, the dialog count starts over
        coordinator.onOwnerChanged(uid)
        // While the merge question is open the account's profile may still
        // come from the guest; afterMerge records the consent then.
        if (coordinator.state.value !is GuestMergeState.Asking) consent.applyTo(uid)
    }
}
