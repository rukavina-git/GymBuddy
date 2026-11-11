package com.rukavina.gymbuddy.ui.guest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.ui.design.AppChoiceDialog
import com.rukavina.gymbuddy.ui.design.AppDialog
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.DialogAction
import com.rukavina.gymbuddy.ui.design.DialogActionTone
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/*
 * Session expired banner and account-deleted dialog, Figma "16 · Session
 * expired" and "20 · Account deleted elsewhere" (node 114-20520).
 */

/** "Backup paused": logging goes on, changes back up after signing in again. */
@Composable
fun SessionExpiredBanner(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceContainer,
        modifier = modifier.fillMaxWidth().padding(horizontal = d.insetScreen, vertical = d.stackTight),
    ) {
        Row(
            modifier = Modifier.padding(start = d.group, end = d.hairline),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.stack),
        ) {
            Icon(painterResource(AppIcons.Ui.CloudOff), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
            Text(
                stringResource(R.string.expired_banner),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(vertical = d.stack),
            )
            AppLinkButton(stringResource(R.string.expired_sign_in), onSignIn)
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
