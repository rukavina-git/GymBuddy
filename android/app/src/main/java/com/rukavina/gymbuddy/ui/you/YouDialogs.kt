package com.rukavina.gymbuddy.ui.you

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppChoiceDialog
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.AppTextFieldDefaults
import com.rukavina.gymbuddy.ui.design.DialogAction
import com.rukavina.gymbuddy.ui.design.DialogActionTone
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.guest.countsText
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Export (R15-11), JSON only (ADR 0015 amendment): one file, no checkboxes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportSheet(counts: LocalDataCounts?, saving: Boolean, onSave: () -> Unit, onDismiss: () -> Unit) {
    val d = GymBuddyTheme.dimens
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.you_export), subtitle = exportSummary(counts)) {
        Column {
            Text(stringResource(R.string.you_export_file), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.you_export_file_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(
            stringResource(R.string.you_export_save),
            onSave,
            Modifier.fillMaxWidth().padding(top = d.group),
            loading = saving,
            enabled = !saving,
        )
    }
}

/** Guest erase (R15-10): Export first is the one to press; Erase all is red. */
@Composable
internal fun EraseDialog(counts: LocalDataCounts?, onExportFirst: () -> Unit, onErase: () -> Unit, onDismiss: () -> Unit) {
    val body = if (counts == null || counts.isEmpty) {
        stringResource(R.string.you_erase_body_empty)
    } else {
        stringResource(R.string.you_erase_body, countsText(counts))
    }
    AppChoiceDialog(
        title = stringResource(R.string.you_erase_title),
        body = body,
        actions = listOf(
            DialogAction(stringResource(R.string.you_export_first), onExportFirst, DialogActionTone.Lime),
            DialogAction(stringResource(R.string.you_erase_all), onErase, DialogActionTone.Danger, enabled = counts != null),
            DialogAction(stringResource(R.string.you_cancel), onDismiss),
        ),
        onDismiss = onDismiss,
        stacked = true,
    )
}

/** Sign out (R15-8, R15-13) and its progress / failure states. */
@Composable
internal fun SignOutDialogs(
    state: LogoutUiState,
    onBackUp: () -> Unit,
    onAnyway: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val title = stringResource(R.string.you_sign_out_title)
    when (state) {
        is LogoutUiState.Confirm -> SignOutQuestion(
            title = title,
            lines = state.lines,
            note = if (state.offline) stringResource(R.string.you_sign_out_offline) else null,
            canBackUp = !state.offline,
            onBackUp = onBackUp,
            onAnyway = onAnyway,
            onCancel = onCancel,
        )
        is LogoutUiState.UnsyncedWarning -> SignOutQuestion(
            title = title,
            lines = state.lines,
            note = stringResource(R.string.you_sign_out_failed_backup),
            canBackUp = true,
            onBackUp = onBackUp,
            onAnyway = onAnyway,
            onCancel = onCancel,
        )
        LogoutUiState.Flushing -> AppChoiceDialog(title = title, body = stringResource(R.string.you_signing_out), actions = emptyList())
        is LogoutUiState.Failed -> AppChoiceDialog(
            title = title,
            body = state.message,
            actions = listOf(
                DialogAction(stringResource(R.string.you_cancel), onCancel),
                DialogAction(stringResource(R.string.you_retry), onRetry, DialogActionTone.Lime),
            ),
            onDismiss = onCancel,
        )
        LogoutUiState.Idle -> Unit
    }
}

@Composable
private fun SignOutQuestion(
    title: String,
    lines: List<String>,
    note: String?,
    canBackUp: Boolean,
    onBackUp: () -> Unit,
    onAnyway: () -> Unit,
    onCancel: () -> Unit,
) {
    val body = stringResource(R.string.you_sign_out_body, joinLines(lines)).let { if (note != null) "$it\n\n$note" else it }
    AppChoiceDialog(
        title = title,
        body = body,
        actions = listOf(
            DialogAction(stringResource(R.string.you_back_up_and_sign_out), onBackUp, DialogActionTone.Lime, enabled = canBackUp),
            DialogAction(stringResource(R.string.you_sign_out_anyway), onAnyway, DialogActionTone.Danger),
            DialogAction(stringResource(R.string.you_cancel), onCancel),
        ),
        onDismiss = onCancel,
        stacked = true,
    )
}

/** "2 workout sessions and 1 workout template". */
internal fun joinLines(lines: List<String>): String = when (lines.size) {
    0 -> ""
    1 -> lines[0]
    else -> lines.dropLast(1).joinToString(", ") + " and " + lines.last()
}

/** Delete account re-auth for password accounts (after the F13 screen). */
@Composable
internal fun DeletePasswordDialog(viewModel: YouViewModel, onClose: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AppChoiceDialog(
        title = stringResource(R.string.you_delete_password_title),
        body = stringResource(R.string.you_delete_password_body),
        actions = listOf(
            DialogAction(stringResource(R.string.you_cancel), onClose, enabled = !busy),
            DialogAction(
                stringResource(R.string.you_delete_account),
                {
                    busy = true
                    viewModel.deleteAccountWithPassword(password) { ok, message ->
                        busy = false
                        if (ok) onClose() else error = message
                    }
                },
                DialogActionTone.Danger,
                enabled = !busy && password.isNotEmpty(),
            ),
        ),
        onDismiss = { if (!busy) onClose() },
    ) {
        AppTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = stringResource(R.string.you_password),
            isError = error != null,
            helper = error,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            container = AppTextFieldDefaults.onDialog,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun DeletionProgressDialogs(state: AccountDeletionUiState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(R.string.you_delete_account)
    when (state) {
        AccountDeletionUiState.InProgress -> AppChoiceDialog(title = title, body = stringResource(R.string.you_deleting), actions = emptyList())
        is AccountDeletionUiState.Failed -> AppChoiceDialog(
            title = title,
            body = state.message,
            actions = listOfNotNull(
                DialogAction(stringResource(R.string.you_close), onDismiss),
                if (state.canRetry) DialogAction(stringResource(R.string.you_retry), onRetry, DialogActionTone.Lime) else null,
            ),
            onDismiss = onDismiss,
        )
        AccountDeletionUiState.Idle -> Unit
    }
}

/** Change password, or set one on a Google-only account. */
@Composable
internal fun PasswordDialog(isSetup: Boolean, viewModel: YouViewModel, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var show by rememberSaveable { mutableStateOf(false) }
    val fill = stringResource(R.string.you_password_fill)
    val mismatch = stringResource(R.string.you_password_mismatch)
    val weak = stringResource(R.string.you_password_weak)
    val doneText = stringResource(if (isSetup) R.string.you_password_set else R.string.you_password_changed)
    val save = {
        error = when {
            (!isSetup && current.isEmpty()) || new.isEmpty() || repeat.isEmpty() -> fill
            new != repeat -> mismatch
            !viewModel.isPasswordStrong(new) -> weak
            else -> null
        }
        if (error == null) {
            busy = true
            val result = { ok: Boolean, message: String? ->
                busy = false
                if (ok) onDone(doneText) else error = message
            }
            if (isSetup) viewModel.setupPassword(new, result) else viewModel.changePassword(current, new, result)
        }
    }
    AppChoiceDialog(
        title = stringResource(if (isSetup) R.string.you_set_password else R.string.you_change_password),
        body = stringResource(R.string.you_password_rule),
        actions = listOf(
            DialogAction(stringResource(R.string.you_cancel), onDismiss, enabled = !busy),
            DialogAction(stringResource(R.string.you_save), save, DialogActionTone.Lime, enabled = !busy),
        ),
        onDismiss = { if (!busy) onDismiss() },
    ) {
        val field = @Composable { value: String, label: Int, onChange: (String) -> Unit ->
            AppTextField(
                value = value,
                onValueChange = { onChange(it); error = null },
                label = stringResource(label),
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                // F11: one eye shows or hides all fields.
                trailing = {
                    AppIconButton(
                        if (show) AppIcons.Ui.Hide else AppIcons.Ui.Show,
                        stringResource(if (show) R.string.cd_hide_password else R.string.cd_show_password),
                        { show = !show },
                    )
                },
                container = AppTextFieldDefaults.onDialog,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!isSetup) field(current, R.string.you_password_current) { current = it }
        field(new, R.string.you_password_new) { new = it }
        field(repeat, R.string.you_password_repeat) { repeat = it }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
