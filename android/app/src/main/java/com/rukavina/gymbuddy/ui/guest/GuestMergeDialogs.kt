package com.rukavina.gymbuddy.ui.guest

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.data.sync.GuestMergeState
import com.rukavina.gymbuddy.ui.design.AppChoiceDialog
import com.rukavina.gymbuddy.ui.design.DialogAction
import com.rukavina.gymbuddy.ui.design.DialogActionTone
import kotlinx.coroutines.launch

/**
 * Guest → account with data: Figma "13 · Guest data into an account with
 * data" asks first (Cancel stays a guest); "14 · Receipt" is a snackbar
 * over Home (node 114-20520). A failed merge says so the same way.
 */
@Composable
fun GuestMergeDialogs(
    state: GuestMergeState,
    snackbar: SnackbarHostState,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val failed = stringResource(R.string.merge_failed)
    // The snackbar outlives the state: dismissing it ends the LaunchedEffect.
    val scope = rememberCoroutineScope()
    when (state) {
        is GuestMergeState.Asking -> AppChoiceDialog(
            title = stringResource(R.string.merge_title),
            body = askText(state),
            actions = listOf(
                DialogAction(stringResource(R.string.merge_add), onAdd, DialogActionTone.Lime),
                DialogAction(stringResource(R.string.merge_stay), onCancel),
            ),
            onDismiss = null, // a choice is needed: Add or stay signed out
            stacked = true,
        )
        is GuestMergeState.Done -> LaunchedEffect(state) {
            onDismiss()
            scope.launch { snackbar.showSnackbar(receiptText(state.receipt)) }
        }
        GuestMergeState.Failed -> LaunchedEffect(state) {
            onDismiss()
            scope.launch { snackbar.showSnackbar(failed) }
        }
        GuestMergeState.Idle, GuestMergeState.Merging -> Unit
    }
}
