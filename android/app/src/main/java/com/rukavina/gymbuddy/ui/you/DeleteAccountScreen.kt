package com.rukavina.gymbuddy.ui.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.DangerFilledButton
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** True when [typed] is the confirm word, ignoring case and outer spaces. */
internal fun isDeleteWordMatch(typed: String, word: String): Boolean =
    word.isNotBlank() && typed.trim().equals(word.trim(), ignoreCase = true)

/** Delete is possible once the word matches, counts have loaded, and nothing is running. */
internal fun canConfirmDelete(typed: String, word: String, countsLoaded: Boolean, busy: Boolean): Boolean =
    !busy && countsLoaded && isDeleteWordMatch(typed, word)

/**
 * F13: Delete account as its own screen. Lists what is lost, typing the
 * (localised) confirm word enables the button, then Google or password
 * re-auth runs and [DeletionProgressDialogs] take over.
 */
@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onLeft: () -> Unit, viewModel: YouViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val unsynced by viewModel.deletionUnsyncedLines.collectAsStateWithLifecycle()
    val deletion by viewModel.accountDeletionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    var askPassword by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadDeletionWarning() }
    LaunchedEffect(Unit) { viewModel.logoutEvent.collect { onLeft() } }

    DeleteAccountContent(
        counts = counts,
        unsyncedLines = unsynced,
        isGoogleOnly = state.isGoogleOnly,
        busy = busy || deletion == AccountDeletionUiState.InProgress,
        error = error,
        onBack = onBack,
        onDelete = {
            error = null
            if (state.isGoogleOnly) {
                busy = true
                viewModel.deleteAccountWithGoogle(context, webClientId) { ok, message ->
                    busy = false
                    if (!ok) error = message
                }
            } else {
                askPassword = true
            }
        },
    )
    if (askPassword) DeletePasswordDialog(viewModel, onClose = { askPassword = false })
    DeletionProgressDialogs(
        state = deletion,
        onRetry = viewModel::retryAccountDeletion,
        onDismiss = viewModel::dismissAccountDeletion,
    )
}

@Composable
internal fun DeleteAccountContent(
    counts: LocalDataCounts?,
    unsyncedLines: List<String>,
    isGoogleOnly: Boolean,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val word = stringResource(R.string.you_delete_word)
    var typed by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
            BackTopBar(onBack = onBack, title = stringResource(R.string.you_delete_account))
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = d.insetScreen, vertical = d.stack),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                Text(
                    stringResource(R.string.you_delete_heading),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(stringResource(R.string.you_delete_intro), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.you_delete_lost),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = d.group),
                )
                lostLines(counts).forEach { line ->
                    Text("•  $line", style = MaterialTheme.typography.bodyMedium)
                }
                if (unsyncedLines.isNotEmpty()) {
                    Text(
                        stringResource(R.string.you_delete_unsynced, joinLines(unsyncedLines)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                AppTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = stringResource(R.string.you_delete_type_label, word),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth().padding(top = d.section),
                )
            }
            // Pinned under the scroll so it stays above the keyboard while typing.
            Column(
                Modifier.fillMaxWidth().padding(horizontal = d.insetScreen, vertical = d.stack),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                DangerFilledButton(
                    stringResource(R.string.you_delete_account),
                    onDelete,
                    Modifier.fillMaxWidth(),
                    enabled = canConfirmDelete(typed, word, countsLoaded = counts != null, busy = busy),
                    loading = busy,
                )
                Text(
                    stringResource(if (isGoogleOnly) R.string.you_delete_confirm_google else R.string.you_delete_confirm_password),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun lostLines(counts: LocalDataCounts?): List<String> {
    val lines = mutableListOf(stringResource(R.string.you_delete_lost_account))
    if (counts == null) return lines
    if (counts.workouts > 0) lines += pluralStringResource(R.plurals.you_delete_lost_workouts, counts.workouts, counts.workouts)
    if (counts.templates > 0) lines += pluralStringResource(R.plurals.you_delete_lost_templates, counts.templates, counts.templates)
    if (counts.exercises > 0) lines += pluralStringResource(R.plurals.you_delete_lost_exercises, counts.exercises, counts.exercises)
    return lines
}
