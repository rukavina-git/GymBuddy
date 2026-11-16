package com.rukavina.gymbuddy.ui.guest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogProperties
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.ui.design.AppChoiceDialog
import com.rukavina.gymbuddy.ui.design.AppDialog
import com.rukavina.gymbuddy.ui.design.AppTextButton
import com.rukavina.gymbuddy.ui.design.DialogAction
import com.rukavina.gymbuddy.ui.design.DialogActionTone
import com.rukavina.gymbuddy.ui.design.DialogWidth
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlinx.coroutines.delay

/*
 * "You're signed out" dialog and Home chip, Figma "Signed out · locked
 * 2026-11-16" (node 271-7257, Plan 9 step 04a), and the account-deleted
 * dialog, Figma "20 · Account deleted elsewhere" (node 114-20520).
 */

/** Taps in the first moment are ignored: a tap meant for Done must not land on Continue. */
private const val INPUT_GUARD_MS = 500L

/**
 * Over everything, a running workout too. Only the two buttons close it:
 * no Back, no tap outside (owner, 2026-11-16).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignedOutDialog(onSignIn: () -> Unit, onContinue: () -> Unit) {
    val d = GymBuddyTheme.dimens
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(INPUT_GUARD_MS)
        armed = true
    }
    BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = DialogWidth),
        ) {
            Column(
                modifier = Modifier.padding(start = d.insetDialog, end = d.insetDialog, top = d.insetDialog, bottom = d.group),
                verticalArrangement = Arrangement.spacedBy(d.group),
            ) {
                Text(
                    stringResource(R.string.signed_out_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.signed_out_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = d.stackTight),
                    verticalArrangement = Arrangement.spacedBy(d.stackTight),
                ) {
                    PrimaryButton(
                        stringResource(R.string.signed_out_sign_in),
                        onClick = { if (armed) onSignIn() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppTextButton(
                        stringResource(R.string.signed_out_continue),
                        onClick = { if (armed) onContinue() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Home, next to the wordmark while signed out (R22-4). Tap = the dialog. */
@Composable
fun SignedOutChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = FullShape,
        color = cs.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = d.group, vertical = d.stackTight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
        ) {
            Icon(painterResource(AppIcons.Ui.CloudOff), null, Modifier.size(d.iconSmall), tint = cs.onSurface)
            Text(stringResource(R.string.signed_out_chip), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Keep as guest (lime) or Erase, which always shows the counts and asks again. */
@Composable
fun AccountDeletedDialog(
    email: String?,
    counts: LocalDataCounts?,
    onKeep: () -> Unit,
    onErase: () -> Unit
) {
    var confirmErase by remember { mutableStateOf(false) }
    if (!confirmErase) {
        AppChoiceDialog(
            title = stringResource(R.string.deleted_title),
            body = deletedText(email, counts),
            actions = listOf(
                DialogAction(stringResource(R.string.deleted_erase), { confirmErase = true }, DialogActionTone.Danger),
                DialogAction(stringResource(R.string.deleted_keep), onKeep, DialogActionTone.Lime),
            ),
            onDismiss = null, // a choice is needed
        )
    } else {
        AppDialog(
            title = eraseTitle(counts),
            body = stringResource(R.string.erase_body),
            confirmLabel = stringResource(R.string.erase_confirm),
            onConfirm = onErase,
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = { confirmErase = false },
            destructive = true,
        )
    }
}
