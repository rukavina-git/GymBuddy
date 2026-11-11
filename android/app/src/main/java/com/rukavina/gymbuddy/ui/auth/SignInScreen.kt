package com.rukavina.gymbuddy.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppChoiceDialog
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.ButtonSize
import com.rukavina.gymbuddy.ui.design.DialogAction
import com.rukavina.gymbuddy.ui.design.DialogActionTone
import com.rukavina.gymbuddy.ui.design.OutlineButton
import com.rukavina.gymbuddy.ui.guest.maskEmail
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlinx.coroutines.launch

/**
 * "Welcome back" (Figma R18-2): left title, Google first, then one email
 * + password form. Errors sit under the field; offline shows a box with
 * Retry; the tapped button spins while Firebase answers.
 */
@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenCreateAccount: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val resetSent = stringResource(R.string.signin_reset_sent)

    fun google(authorizedFirst: Boolean) {
        if (!viewModel.onGoogleStarted()) return
        scope.launch {
            when (val result = openGoogleSheet(context, webClientId, authorizedFirst)) {
                is GoogleSheetResult.Token -> viewModel.onGoogleToken(result.token.idToken, result.token.email)
                GoogleSheetResult.Cancelled -> viewModel.onGoogleCancelled()
                is GoogleSheetResult.Failed -> viewModel.onGoogleFailed(result.offline)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SignInEvent.Done -> onDone()
                SignInEvent.OpenCreateAccount -> onOpenCreateAccount()
                SignInEvent.PickAnotherGoogleAccount -> google(authorizedFirst = false)
                is SignInEvent.ResetSent -> snackbar.showSnackbar(resetSent.format(event.email))
            }
        }
    }

    SignInContent(
        state = state,
        onBack = onBack,
        onGoogle = { google(authorizedFirst = true) },
        onEmail = viewModel::onEmail,
        onPassword = viewModel::onPassword,
        onToggleShow = viewModel::toggleShowPassword,
        onSubmit = viewModel::submitEmail,
        onForgot = viewModel::forgotPassword,
        onRetry = viewModel::retry,
        onCreateAccount = onOpenCreateAccount,
        snackbar = snackbar,
    )

    when (val dialog = state.dialog) {
        is SignInDialog.NoAccount -> AppChoiceDialog(
            title = stringResource(R.string.no_account_title),
            body = stringResource(R.string.no_account_body, dialog.email?.let(::maskEmail) ?: ""),
            actions = listOf(
                DialogAction(stringResource(R.string.no_account_create), viewModel::noAccountCreate, DialogActionTone.Lime),
                DialogAction(stringResource(R.string.no_account_other), viewModel::noAccountOther),
            ),
            stacked = true,
        )
        is SignInDialog.OtherAccount -> AppChoiceDialog(
            title = stringResource(R.string.other_account_title),
            body = stringResource(
                R.string.other_account_body,
                pluralStringResource(R.plurals.other_account_changes, dialog.changes, dialog.changes),
                maskEmail(dialog.ownerEmail),
                dialog.newEmail,
            ),
            actions = listOf(
                DialogAction(stringResource(R.string.other_account_continue, dialog.newEmail), viewModel::continueAsNew, DialogActionTone.Lime),
                DialogAction(stringResource(R.string.other_account_switch, maskEmail(dialog.ownerEmail)), viewModel::switchToOwner),
            ),
            onDismiss = viewModel::dismissDialog,
            stacked = true,
        )
        null -> Unit
    }
}

@Composable
internal fun SignInContent(
    state: SignInUiState,
    onBack: () -> Unit,
    onGoogle: () -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onToggleShow: () -> Unit,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
    onRetry: () -> Unit,
    onCreateAccount: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val d = GymBuddyTheme.dimens
    val idle = state.busy == null
    Scaffold(
        topBar = { BackTopBar(onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) { AppSnackbar(it) } },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.insetScreen)
                .padding(bottom = d.section),
            verticalArrangement = Arrangement.spacedBy(d.group),
        ) {
            AuthTitle(stringResource(R.string.signin_title))
            Spacer(Modifier.height(d.stackTight))
            GoogleButton(onGoogle, enabled = idle, loading = state.busy == SignInAction.GOOGLE)
            OrDivider()
            if (state.offline) OfflineBox(onRetry)
            val error = state.error?.let { formErrorText(it) }
            EmailPasswordFields(
                email = state.email,
                password = state.password,
                showPassword = state.showPassword,
                newPassword = false,
                helper = error,
                isError = error != null,
                onEmail = onEmail,
                onPassword = onPassword,
                onToggleShow = onToggleShow,
                onDone = onSubmit,
            )
            OutlineButton(
                text = stringResource(R.string.signin_submit),
                onClick = onSubmit,
                size = ButtonSize.Form,
                enabled = idle,
                loading = state.busy == SignInAction.EMAIL,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                QuietLink(stringResource(R.string.signin_forgot), onForgot)
                SwitchLink(stringResource(R.string.signin_no_account), stringResource(R.string.signin_create_one), onCreateAccount)
            }
        }
    }
}
