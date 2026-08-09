package com.rukavina.gymbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.rukavina.gymbuddy.utils.validation.ValidatedFieldState

/**
 * Text field with integrated validation and character counter.
 *
 * Features:
 * - Shows error on blur (after user leaves field)
 * - Displays character counter when maxLength is set
 * - Error message on left, character count on right
 * - Uses Material3 supportingText and isError patterns
 *
 * @param state ValidatedFieldState managing value and validation
 * @param label Label text for the field
 * @param modifier Modifier for the text field
 * @param placeholder Placeholder text when empty
 * @param singleLine Whether to limit to single line
 * @param minLines Minimum lines for multiline fields
 * @param maxLines Maximum lines for multiline fields
 * @param keyboardType Type of keyboard to show
 * @param capitalization Text capitalization behavior
 * @param imeAction IME action button
 * @param onImeAction Callback when IME action is triggered
 */
@Composable
fun ValidatedTextField(
    state: ValidatedFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = { state.onValueChange(it) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = state.shouldShowError,
        supportingText = {
            SupportingTextRow(
                errorMessage = state.errorMessage,
                currentLength = state.value.length,
                maxLength = state.maxLength
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction?.invoke() },
            onNext = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() }
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                state.onFocusChanged(focusState.isFocused)
            }
    )
}

/**
 * Supporting text row showing error message on left and character count on right.
 */
@Composable
private fun SupportingTextRow(
    errorMessage: String?,
    currentLength: Int,
    maxLength: Int?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Error message (left)
        Text(
            text = errorMessage ?: "",
            color = if (errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.bodySmall
        )

        // Character counter (right)
        if (maxLength != null) {
            val remaining = maxLength - currentLength
            Text(
                text = "$currentLength/$maxLength",
                color = if (remaining < 0) {
                    MaterialTheme.colorScheme.error
                } else if (remaining <= 10) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Convenience function to create and remember a ValidatedFieldState.
 */
@Composable
fun rememberValidatedFieldState(
    initialValue: String = "",
    validators: List<com.rukavina.gymbuddy.utils.validation.Validator> = emptyList(),
    maxLength: Int? = null
): ValidatedFieldState {
    return remember {
        ValidatedFieldState(
            initialValue = initialValue,
            validators = validators,
            maxLength = maxLength
        )
    }
}
