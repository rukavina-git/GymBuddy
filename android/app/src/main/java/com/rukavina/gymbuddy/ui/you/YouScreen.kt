package com.rukavina.gymbuddy.ui.you

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.data.export.DataExporter
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import com.rukavina.gymbuddy.ui.design.AppSwitch
import com.rukavina.gymbuddy.ui.design.AppTextButton
import com.rukavina.gymbuddy.ui.design.LargeTitleTopBar
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.focusRing
import com.rukavina.gymbuddy.ui.design.rememberLargeTitleState
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlinx.coroutines.launch
import java.time.Clock

/** Raw size from R15-2 (DESIGN.md §4): avatar on the account card. */
private val CardAvatar = 48.dp

/** What the You page can open. Routes live in MainScreen. */
data class YouNav(
    val onProfile: () -> Unit,
    val onCreateAccount: () -> Unit,
    val onSignIn: () -> Unit,
    val onHiddenExercises: () -> Unit,
    val onHiddenTemplates: () -> Unit,
    val onAbout: () -> Unit,
    /** Delete account screen (F13). */
    val onDeleteAccount: () -> Unit,
    /** Signed out, erased or deleted. */
    val onLeft: () -> Unit,
)

/** You tab (7.10a): one page, R15-1 guest, R15-2 signed in. */
@Composable
fun YouScreen(nav: YouNav, viewModel: YouViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sheet by rememberSaveable { mutableStateOf<YouSheet?>(null) }

    LaunchedEffect(Unit) { viewModel.logoutEvent.collect { nav.onLeft() } }

    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val exportDone = stringResource(R.string.you_export_done)
    val exportFailed = stringResource(R.string.you_export_failed)
    LaunchedEffect(exportState) {
        val message = when (exportState) {
            ExportUiState.Done -> exportDone
            ExportUiState.Failed -> exportFailed
            else -> null
        } ?: return@LaunchedEffect
        sheet = null
        viewModel.clearExportState()
        // Not in this effect: clearing the state restarts it.
        // F9: long enough to read, with a close button.
        scope.launch { snackbar.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Long) }
    }

    val open = { s: YouSheet ->
        if (s == YouSheet.Export || s == YouSheet.Erase) viewModel.loadCounts()
        sheet = s
    }
    val actions = YouActions(
        nav = nav,
        onUnits = { open(YouSheet.Units) },
        onRestBuzz = viewModel::setRestBuzz,
        onNotification = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
        onExport = { open(YouSheet.Export) },
        onErase = { open(YouSheet.Erase) },
        onRetryBackup = viewModel::retryBackup,
        onPassword = { open(YouSheet.Password) },
        onSignOut = viewModel::signOut,
        onDelete = nav.onDeleteAccount,
    )

    Box(Modifier.fillMaxSize()) {
        YouContent(state, actions, Clock.systemDefaultZone())
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)) { AppSnackbar(it) }
    }

    val counts by viewModel.counts.collectAsStateWithLifecycle()
    when (sheet) {
        YouSheet.Units -> UnitsSheet(state.units, { viewModel.setUnits(it); sheet = null }, { sheet = null })
        YouSheet.Export -> ExportSheet(
            counts = counts,
            saving = exportState == ExportUiState.Saving,
            onSave = { exportLauncher.launch(DataExporter.fileName(Clock.systemDefaultZone())) },
            onDismiss = { sheet = null },
        )
        YouSheet.Erase -> EraseDialog(
            counts = counts,
            onExportFirst = { open(YouSheet.Export) },
            onErase = { sheet = null; viewModel.eraseGuestData() },
            onDismiss = { sheet = null },
        )
        YouSheet.Password -> PasswordDialog(
            isSetup = state.isGoogleOnly,
            viewModel = viewModel,
            onDone = { message -> sheet = null; scope.launch { snackbar.showSnackbar(message) } },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }

    val logoutState by viewModel.logoutState.collectAsStateWithLifecycle()
    SignOutDialogs(
        state = logoutState,
        onBackUp = viewModel::logout,
        onAnyway = viewModel::confirmLogoutAndDelete,
        onRetry = viewModel::retryLogout,
        onCancel = viewModel::cancelLogout,
    )
}

internal enum class YouSheet { Units, Export, Erase, Password }

internal data class YouActions(
    val nav: YouNav,
    val onUnits: () -> Unit,
    val onRestBuzz: (Boolean) -> Unit,
    val onNotification: () -> Unit,
    val onExport: () -> Unit,
    val onErase: () -> Unit,
    val onRetryBackup: () -> Unit,
    val onPassword: () -> Unit,
    val onSignOut: () -> Unit,
    val onDelete: () -> Unit,
)

@Composable
internal fun YouContent(state: YouUiState, actions: YouActions, clock: Clock) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val titleState = rememberLargeTitleState()
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            LargeTitleTopBar(title = stringResource(R.string.tab_you), state = titleState)
            if (state.isLoading) return@Column
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(titleState.nestedScrollConnection),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                item(key = "card") {
                    if (state.isGuest) GuestCard(actions.nav) else AccountCard(state, clock, actions)
                }
                item(key = "workout") {
                    Group(stringResource(R.string.you_group_workout)) {
                        ValueRow(
                            stringResource(R.string.you_units),
                            stringResource(if (state.units == PreferredUnits.IMPERIAL) R.string.you_units_imperial else R.string.you_units_metric),
                            actions.onUnits,
                        )
                        RowDivider()
                        SwitchRow(stringResource(R.string.you_rest_buzz), state.restBuzz, actions.onRestBuzz)
                        RowDivider()
                        ValueRow(
                            stringResource(R.string.you_notification),
                            null,
                            actions.onNotification,
                            subtitle = stringResource(R.string.you_notification_sub),
                        )
                    }
                }
                item(key = "lists") {
                    Group(stringResource(R.string.you_group_lists)) {
                        ValueRow(stringResource(R.string.you_hidden_exercises), state.hiddenExercises.toString(), actions.nav.onHiddenExercises)
                        RowDivider()
                        ValueRow(stringResource(R.string.you_hidden_templates), state.hiddenTemplates.toString(), actions.nav.onHiddenTemplates)
                    }
                }
                item(key = "data") {
                    Group(stringResource(R.string.you_group_data)) {
                        ValueRow(stringResource(R.string.you_export), null, actions.onExport, subtitle = stringResource(R.string.you_export_sub))
                        if (state.isGuest) {
                            RowDivider()
                            ValueRow(stringResource(R.string.you_erase), null, actions.onErase, danger = true)
                        }
                    }
                }
                item(key = "help") {
                    Group(stringResource(R.string.you_group_help)) {
                        ValueRow(stringResource(R.string.you_about), null, actions.nav.onAbout)
                    }
                }
                if (!state.isGuest) item(key = "account") {
                    Group(stringResource(R.string.you_group_account)) {
                        ValueRow(
                            stringResource(if (state.isGoogleOnly) R.string.you_set_password else R.string.you_change_password),
                            null,
                            actions.onPassword,
                        )
                        RowDivider()
                        ValueRow(stringResource(R.string.you_sign_out), null, actions.onSignOut)
                        RowDivider()
                        ValueRow(stringResource(R.string.you_delete_account), null, actions.onDelete, danger = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestCard(nav: YouNav) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
            Text(stringResource(R.string.you_guest_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.you_guest_body), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            Row(
                Modifier.padding(top = d.stackTight),
                horizontalArrangement = Arrangement.spacedBy(d.stack),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryButton(stringResource(R.string.you_create_account), nav.onCreateAccount, Modifier.weight(1f))
                AppTextButton(stringResource(R.string.you_sign_in), nav.onSignIn)
            }
        }
    }
}

@Composable
private fun AccountCard(state: YouUiState, clock: Clock, actions: YouActions) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier.padding(start = d.insetCard, end = d.insetCard, top = d.insetCard),
                horizontalArrangement = Arrangement.spacedBy(d.stack),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(state.avatarId, state.userName.ifBlank { state.userEmail }, CardAvatar)
                Column(Modifier.weight(1f)) {
                    if (state.userName.isNotBlank()) Text(state.userName, style = MaterialTheme.typography.titleSmall)
                    Text(state.userEmail, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            BackupRow(state.backup, clock, actions.onRetryBackup)
            RowDivider()
            ValueRow(stringResource(R.string.you_profile), null, actions.nav.onProfile)
        }
    }
}

@Composable
private fun BackupRow(line: BackupLine, clock: Clock, onRetry: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val (icon, text, tint) = when (line) {
        is BackupLine.BackedUp -> Triple(
            AppIcons.Ui.Check,
            line.at?.let { stringResource(R.string.you_backed_up_at, agoText(it, clock.millis())) }
                ?: stringResource(R.string.you_backed_up),
            cs.onSurfaceVariant,
        )
        is BackupLine.Waiting -> Triple(
            AppIcons.Ui.CloudOff,
            pluralStringResource(R.plurals.you_changes_waiting, line.changes, line.changes),
            cs.onSurfaceVariant,
        )
        is BackupLine.Failed -> Triple(AppIcons.Ui.Alert, stringResource(R.string.you_backup_failed) + " · " +
            pluralStringResource(R.plurals.you_changes_waiting, line.changes, line.changes), cs.error)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = d.target).padding(horizontal = d.insetCard),
        horizontalArrangement = Arrangement.spacedBy(d.stackTight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(d.iconSmall), tint = tint)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tint, modifier = Modifier.weight(1f))
        if (line is BackupLine.Failed) AppTextButton(stringResource(R.string.you_retry), onRetry)
    }
}

@Composable
private fun agoText(at: Long, now: Long): String {
    val minutes = ((now - at).coerceAtLeast(0) / 60_000).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.you_just_now)
        minutes < 60 -> stringResource(R.string.you_min_ago, minutes)
        else -> stringResource(R.string.you_hours_ago, minutes / 60)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val d = GymBuddyTheme.dimens
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = d.control)
            .padding(horizontal = d.insetCard, vertical = d.stackTight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        AppSwitch(checked = checked, onCheckedChange = onChange)
    }
}

/** One choice for now (decision 3): metric or imperial for weight and distance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitsSheet(current: PreferredUnits, onPick: (PreferredUnits) -> Unit, onDismiss: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val c = GymBuddyTheme.colors
    val cs = MaterialTheme.colorScheme
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.you_units)) {
        listOf(
            PreferredUnits.METRIC to R.string.you_units_metric_long,
            PreferredUnits.IMPERIAL to R.string.you_units_imperial_long,
        ).forEach { (units, label) ->
            val selected = units == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = d.control)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(units) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.stack),
            ) {
                RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.cobalt, unselectedColor = cs.outline))
                Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
