package com.rukavina.gymbuddy.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppChoiceDialog
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import com.rukavina.gymbuddy.ui.design.AppTextButton
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.DialogAction
import com.rukavina.gymbuddy.ui.design.DialogActionTone
import com.rukavina.gymbuddy.ui.design.OutlineButton
import com.rukavina.gymbuddy.ui.guest.countsText
import com.rukavina.gymbuddy.ui.guest.maskEmail
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlinx.coroutines.launch

/** Raw sizes from the Figma frames (DESIGN.md §4). */
private val BrandMarkSmall = 64.dp
private val WordmarkSmall = 31.dp
private val GoogleLogo = 20.dp

/**
 * Google's own dark sign-in button (DESIGN.md §10: Google's brand
 * rules, never recoloured or tokenised), so these colours are Google's.
 */
private val GoogleFill = Color(0xFF131314)
private val GoogleStroke = Color(0xFF8E918F)
private val GoogleText = Color(0xFFE3E3E3)

/**
 * "I have an account" and "Back up your workouts" (Figma 114:20520):
 * Google first, then one email + password form. Errors sit under the
 * field; offline shows a box with Retry.
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
        scope.launch {
            when (val result = openGoogleSheet(context, webClientId, authorizedFirst)) {
                is GoogleSheetResult.Token -> viewModel.onGoogleToken(result.token.idToken, result.token.email)
                GoogleSheetResult.Cancelled -> Unit
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
        onGoogle = { if (viewModel.canStartGoogle()) google(authorizedFirst = state.mode == SignInMode.SIGN_IN) },
        onEmail = viewModel::onEmail,
        onPassword = viewModel::onPassword,
        onToggleShow = viewModel::toggleShowPassword,
        onConsent = viewModel::onConsent,
        onWhy = viewModel::showWhy,
        onSubmit = viewModel::submitEmail,
        onForgot = viewModel::forgotPassword,
        onRetry = viewModel::retry,
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
        SignInDialog.WhyConsent -> AppChoiceDialog(
            title = stringResource(R.string.why_title),
            body = stringResource(R.string.why_body),
            actions = listOf(DialogAction(stringResource(R.string.ok), viewModel::dismissDialog)),
            onDismiss = viewModel::dismissDialog,
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
    onConsent: (Boolean) -> Unit,
    onWhy: () -> Unit,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
    onRetry: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val d = GymBuddyTheme.dimens
    val create = state.mode == SignInMode.CREATE
    Scaffold(
        topBar = {
            if (create) {
                Row(Modifier.statusBarsPadding().padding(horizontal = d.hairline)) {
                    AppIconButton(AppIcons.Ui.Close, stringResource(R.string.cd_close), onBack)
                }
            } else {
                BackTopBar(onBack = onBack)
            }
        },
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
            if (create) CreateHeader(state, onConsent, onWhy) else SignInHeader()
            GoogleButton(onGoogle, enabled = !state.busy)
            OrDivider()
            if (state.offline) OfflineBox(onRetry)
            AppTextField(
                value = state.email,
                onValueChange = onEmail,
                label = stringResource(R.string.signin_email),
                placeholder = stringResource(R.string.signin_email_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.semantics { contentType = ContentType.EmailAddress + ContentType.Username },
            )
            val error = state.error?.let { formErrorText(it) }
            AppTextField(
                value = state.password,
                onValueChange = onPassword,
                label = stringResource(R.string.signin_password),
                helper = error ?: if (create) stringResource(R.string.create_password_helper) else null,
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    AppTextButton(
                        stringResource(if (state.showPassword) R.string.signin_hide else R.string.signin_show),
                        onToggleShow,
                    )
                },
                modifier = Modifier.semantics {
                    contentType = if (create) ContentType.NewPassword else ContentType.Password
                },
            )
            OutlineButton(
                text = stringResource(if (create) R.string.create_submit else R.string.signin_submit),
                onClick = onSubmit,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            if (create) {
                Text(
                    stringResource(R.string.create_terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                AppTextButton(stringResource(R.string.signin_forgot), onForgot, Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun SignInHeader() {
    val d = GymBuddyTheme.dimens
    // Figma R16-4: 66 above the mark, 16 to the wordmark, 23 to the title, 28 to Google.
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = d.section + d.section + d.group, bottom = d.stack),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(AppIcons.Brand.AppIcon), null, Modifier.size(BrandMarkSmall))
        Spacer(Modifier.height(d.group))
        Image(painterResource(AppIcons.Brand.Wordmark), stringResource(R.string.cd_app_name), Modifier.height(WordmarkSmall))
        Spacer(Modifier.height(d.section))
        Text(
            stringResource(R.string.signin_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun CreateHeader(state: SignInUiState, onConsent: (Boolean) -> Unit, onWhy: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Text(
        stringResource(R.string.create_title),
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(top = d.stack).semantics { heading() },
    )
    val counts = state.counts
    Text(
        if (counts == null || counts.isEmpty) stringResource(R.string.create_moves_empty)
        else stringResource(R.string.create_moves, countsText(counts)),
        style = MaterialTheme.typography.bodyMedium,
        color = GymBuddyTheme.colors.textSoft,
    )
    Column(verticalArrangement = Arrangement.spacedBy(d.hairline)) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = cs.surfaceContainer,
            border = if (state.consentMissing) BorderStroke(d.strokeEmphasis, cs.error) else null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(state.consent, role = Role.Checkbox, onValueChange = onConsent)
                    .padding(end = d.group, top = d.stackTight, bottom = d.stackTight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.consent,
                    onCheckedChange = null,
                    modifier = Modifier.padding(d.stack),
                    colors = CheckboxDefaults.colors(
                        checkedColor = GymBuddyTheme.colors.cobalt,
                        uncheckedColor = if (state.consentMissing) cs.error else cs.outline,
                        checkmarkColor = cs.onSurface,
                    ),
                )
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.create_consent), style = MaterialTheme.typography.bodyMedium)
                    AppLinkButton(stringResource(R.string.create_why), onWhy)
                }
            }
        }
        if (state.consentMissing) {
            Text(
                stringResource(R.string.create_consent_missing),
                style = MaterialTheme.typography.bodySmall,
                color = cs.error,
            )
        }
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit, enabled: Boolean) {
    val d = GymBuddyTheme.dimens
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = FullShape,
        color = GoogleFill,
        contentColor = GoogleText,
        border = BorderStroke(d.strokeOutline, GoogleStroke),
        modifier = Modifier.fillMaxWidth().heightIn(min = d.control),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(d.stack, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = d.group),
        ) {
            Icon(painterResource(R.drawable.ic_google), null, Modifier.size(GoogleLogo), tint = Color.Unspecified)
            Text(stringResource(R.string.signin_google), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OrDivider() {
    val d = GymBuddyTheme.dimens
    // Figma: about 32dp of air either side of "or".
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.section + d.stackTight)) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(stringResource(R.string.signin_or), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Placeholder offline box; the error round (7.11) gives it final wording. */
@Composable
private fun OfflineBox(onRetry: () -> Unit) {
    val d = GymBuddyTheme.dimens
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(start = d.group, end = d.hairline, top = d.stack, bottom = d.stack), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.signin_offline), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            AppTextButton(stringResource(R.string.signin_retry), onRetry)
        }
    }
}

@Composable
private fun formErrorText(error: FormError): String = stringResource(
    when (error) {
        FormError.EMPTY -> R.string.signin_error_empty
        FormError.WRONG_CREDENTIALS -> R.string.signin_error_wrong
        FormError.INVALID_EMAIL -> R.string.signin_error_invalid_email
        FormError.WEAK_PASSWORD -> R.string.signin_error_weak
        FormError.EMAIL_IN_USE -> R.string.signin_error_in_use
        FormError.NEED_EMAIL_FOR_RESET -> R.string.signin_error_reset_email
        FormError.OTHER -> R.string.signin_error_other
    }
)
