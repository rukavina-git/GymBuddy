package com.rukavina.gymbuddy.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.ButtonSize
import com.rukavina.gymbuddy.ui.design.OutlineButton
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.onboarding.SetupProgress
import com.rukavina.gymbuddy.ui.onboarding.SETUP_STEPS_WITH_ACCOUNT
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.fadeThroughIn
import com.rukavina.gymbuddy.ui.theme.fadeThroughOut
import kotlinx.coroutines.launch

/**
 * Create account in three short screens (Figma R18-3 … R18-6): method,
 * email (email path only), consent. From setup it is step 5, with the
 * progress bar and "Not now".
 */
@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel,
    onExit: () -> Unit,
    onSignIn: () -> Unit,
    onCreated: (newAccount: Boolean) -> Unit,
    onNotNow: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val scope = rememberCoroutineScope()

    fun google() {
        scope.launch {
            when (val result = openGoogleSheet(context, webClientId, authorizedFirst = false)) {
                is GoogleSheetResult.Token -> viewModel.onGoogleToken(result.token.idToken)
                GoogleSheetResult.Cancelled -> Unit
                is GoogleSheetResult.Failed -> viewModel.onGoogleFailed(result.offline)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateAccountEvent.Created -> onCreated(event.newAccount)
                CreateAccountEvent.NotNow -> onNotNow()
                CreateAccountEvent.RetryGoogle -> google()
            }
        }
    }
    BackHandler(enabled = state.step != CreateStep.METHOD) { viewModel.back() }

    CreateAccountContent(
        state = state,
        onBack = { if (!viewModel.back()) onExit() },
        onGoogle = ::google,
        onChooseEmail = viewModel::chooseEmail,
        onSignIn = onSignIn,
        onEmail = viewModel::onEmail,
        onPassword = viewModel::onPassword,
        onToggleShow = viewModel::toggleShowPassword,
        onContinue = viewModel::continueWithEmail,
        onAgree = viewModel::agree,
        onNotNow = viewModel::notNow,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun CreateAccountContent(
    state: CreateAccountUiState,
    onBack: () -> Unit,
    onGoogle: () -> Unit,
    onChooseEmail: () -> Unit,
    onSignIn: () -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onToggleShow: () -> Unit,
    onContinue: () -> Unit,
    onAgree: () -> Unit,
    onNotNow: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeThroughIn togetherWith fadeThroughOut },
            label = "create-account",
        ) { step ->
            StepFrame(state, step, onBack) {
                when (step) {
                    CreateStep.METHOD -> MethodStep(state, onGoogle, onChooseEmail, onSignIn, onNotNow, onRetry)
                    CreateStep.EMAIL -> EmailStep(state, onEmail, onPassword, onToggleShow, onContinue)
                    CreateStep.CONSENT -> ConsentStep(state, onAgree, onNotNow, onRetry)
                }
            }
        }
    }
}

/** Close (×) on the first screen from Welcome / Sign in / You; back arrow elsewhere and in setup. */
@Composable
private fun StepFrame(state: CreateAccountUiState, step: CreateStep, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val d = GymBuddyTheme.dimens
    val inSetup = state.from == CreateFrom.SETUP
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = d.hairline)) {
            if (step == CreateStep.METHOD && !inSetup) {
                AppIconButton(AppIcons.Ui.Close, stringResource(R.string.cd_close), onBack)
            } else {
                AppIconButton(AppIcons.Ui.Back, stringResource(R.string.cd_back), onBack)
            }
        }
        if (inSetup) SetupProgress(SETUP_STEPS_WITH_ACCOUNT, SETUP_STEPS_WITH_ACCOUNT)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = d.insetScreen, end = d.insetScreen, bottom = d.insetScreen + d.section),
            verticalArrangement = Arrangement.spacedBy(d.group),
            content = content,
        )
    }
}

@Composable
private fun ColumnScope.MethodStep(
    state: CreateAccountUiState,
    onGoogle: () -> Unit,
    onChooseEmail: () -> Unit,
    onSignIn: () -> Unit,
    onNotNow: () -> Unit,
    onRetry: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    AuthTitle(stringResource(R.string.create_title))
    Text(stringResource(R.string.create_line), style = MaterialTheme.typography.bodyMedium, color = GymBuddyTheme.colors.textSoft)
    Spacer(Modifier.height(d.stackTight))
    if (state.offline) OfflineBox(onRetry)
    GoogleButton(onGoogle, enabled = !state.busy, loading = false)
    OutlineButton(
        stringResource(R.string.create_with_email),
        onChooseEmail,
        size = ButtonSize.Form,
        modifier = Modifier.fillMaxWidth(),
    )
    state.error?.let { ErrorLine(formErrorText(it)) }
    if (state.from == CreateFrom.SETUP) {
        Spacer(Modifier.weight(1f))
        QuietLink(stringResource(R.string.create_not_now), onNotNow, Modifier.align(Alignment.CenterHorizontally))
    } else {
        SwitchLink(
            stringResource(R.string.create_have_account),
            stringResource(R.string.create_sign_in),
            onSignIn,
            Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ColumnScope.EmailStep(
    state: CreateAccountUiState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onToggleShow: () -> Unit,
    onContinue: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    AuthTitle(stringResource(R.string.create_title))
    Spacer(Modifier.height(d.stackTight))
    val error = state.error?.let { formErrorText(it) }
    EmailPasswordFields(
        email = state.email,
        password = state.password,
        showPassword = state.showPassword,
        newPassword = true,
        helper = error ?: stringResource(R.string.create_password_helper),
        isError = error != null,
        onEmail = onEmail,
        onPassword = onPassword,
        onToggleShow = onToggleShow,
        onDone = onContinue,
    )
    PrimaryButton(stringResource(R.string.create_continue), onContinue, Modifier.fillMaxWidth(), size = ButtonSize.Form)
}

@Composable
private fun ColumnScope.ConsentStep(
    state: CreateAccountUiState,
    onAgree: () -> Unit,
    onNotNow: () -> Unit,
    onRetry: () -> Unit,
) {
    AuthTitle(stringResource(R.string.consent_title))
    Text(stringResource(R.string.consent_body), style = MaterialTheme.typography.bodyLarge, color = GymBuddyTheme.colors.textSoft)
    Spacer(Modifier.weight(1f))
    if (state.offline) OfflineBox(onRetry)
    state.error?.let { ErrorLine(formErrorText(it)) }
    // Plain text until the policy pages exist (roadmap 9.2 makes them links).
    Text(
        stringResource(R.string.create_terms),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton(
        stringResource(R.string.consent_agree),
        onAgree,
        Modifier.fillMaxWidth(),
        loading = state.busy,
    )
    QuietLink(stringResource(R.string.create_not_now), onNotNow, Modifier.align(Alignment.CenterHorizontally))
}

@Composable
private fun ErrorLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
