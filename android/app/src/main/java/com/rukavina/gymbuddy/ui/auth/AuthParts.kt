package com.rukavina.gymbuddy.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.AppTextButton
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/*
 * Parts shared by Sign in and Create account (Figma R18-2 … R18-5).
 */

private val GoogleLogo = 20.dp
private val GoogleHeight = 48.dp

/**
 * Google's own dark sign-in button (DESIGN.md §10: Google's brand
 * rules, never recoloured or tokenised), so these colours are Google's.
 */
private val GoogleFill = Color(0xFF131314)
private val GoogleStroke = Color(0xFF8E918F)
private val GoogleText = Color(0xFFE3E3E3)

/** The big left title every auth and setup screen opens with. */
@Composable
internal fun AuthTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge,
        modifier = modifier.padding(top = GymBuddyTheme.dimens.stackTight).semantics { heading() },
    )
}

@Composable
internal fun GoogleButton(onClick: () -> Unit, enabled: Boolean, loading: Boolean) {
    val d = GymBuddyTheme.dimens
    val working = stringResource(R.string.state_working)
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = FullShape,
        color = GoogleFill,
        contentColor = GoogleText,
        border = BorderStroke(d.strokeOutline, GoogleStroke),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GoogleHeight)
            .semantics { if (loading) stateDescription = working },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(d.stack, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = d.group),
        ) {
            if (loading) {
                CircularProgressIndicator(color = GoogleText, strokeWidth = d.strokeEmphasis, modifier = Modifier.size(GoogleLogo))
            } else {
                Icon(painterResource(R.drawable.ic_google), null, Modifier.size(GoogleLogo), tint = Color.Unspecified)
            }
            Text(stringResource(R.string.signin_google), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun OrDivider() {
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
internal fun OfflineBox(onRetry: () -> Unit) {
    val d = GymBuddyTheme.dimens
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(start = d.group, end = d.hairline, top = d.stack, bottom = d.stack), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.signin_offline), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            AppTextButton(stringResource(R.string.signin_retry), onRetry)
        }
    }
}

/** Email + password with Show; autofill and paste work. [helper] sits under the password. */
@Composable
internal fun EmailPasswordFields(
    email: String,
    password: String,
    showPassword: Boolean,
    newPassword: Boolean,
    helper: String?,
    isError: Boolean,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onToggleShow: () -> Unit,
    onDone: () -> Unit,
) {
    AppTextField(
        value = email,
        onValueChange = onEmail,
        label = stringResource(R.string.signin_email),
        placeholder = stringResource(R.string.signin_email_hint),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.semantics { contentType = ContentType.EmailAddress + ContentType.Username },
    )
    AppTextField(
        value = password,
        onValueChange = onPassword,
        label = stringResource(R.string.signin_password),
        helper = helper,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            AppTextButton(stringResource(if (showPassword) R.string.signin_hide else R.string.signin_show), onToggleShow)
        },
        modifier = Modifier.semantics { contentType = if (newPassword) ContentType.NewPassword else ContentType.Password },
    )
}

/** Small muted text link: "Forgot password?", "Not now". 48dp tap height. */
@Composable
internal fun QuietLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = GymBuddyTheme.dimens.target)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = GymBuddyTheme.dimens.stack),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "No account yet? Create one": muted question, lime action. */
@Composable
internal fun SwitchLink(question: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(question, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppLinkButton(action, onClick)
    }
}

@Composable
internal fun formErrorText(error: FormError): String = stringResource(
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
