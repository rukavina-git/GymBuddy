package com.rukavina.gymbuddy.utils.validation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State holder for validated text input fields.
 * Manages value, touch state, focus state, and validation.
 *
 * Errors are shown only after the user has touched and left the field (on blur),
 * following Instagram/WhatsApp patterns for better UX.
 *
 * Usage:
 * ```
 * val nameState = remember {
 *     ValidatedFieldState(
 *         initialValue = currentName,
 *         validators = listOf(
 *             Validators.required("Name"),
 *             Validators.maxLength(50)
 *         ),
 *         maxLength = 50
 *     )
 * }
 * ```
 *
 * @param initialValue Starting value for the field
 * @param validators List of validators to run against the value
 * @param maxLength Optional max length (for character counter display)
 */
class ValidatedFieldState(
    initialValue: String = "",
    private val validators: List<Validator> = emptyList(),
    val maxLength: Int? = null
) {
    /**
     * Current text value of the field.
     */
    var value by mutableStateOf(initialValue)
        private set

    /**
     * Whether the user has interacted with this field.
     */
    var isTouched by mutableStateOf(false)
        private set

    /**
     * Whether the field currently has focus.
     */
    var isFocused by mutableStateOf(false)
        private set

    /**
     * Runs all validators and returns the first failure, or Valid if all pass.
     */
    val validationResult: ValidationResult
        get() {
            for (validator in validators) {
                val result = validator.validate(value)
                if (result is ValidationResult.Invalid) {
                    return result
                }
            }
            return ValidationResult.Valid
        }

    /**
     * Whether the current value passes all validators.
     */
    val isValid: Boolean
        get() = validationResult is ValidationResult.Valid

    /**
     * Whether to show the error state.
     * - Shows "required" errors only after blur (so we don't nag before they type)
     * - Shows other validation errors immediately while typing (if save is disabled, explain why)
     */
    val shouldShowError: Boolean
        get() = when {
            // After blur, show all errors
            isTouched && !isFocused -> !isValid
            // While typing with content, show errors immediately (except "required")
            value.isNotEmpty() && !isValid -> true
            else -> false
        }

    /**
     * Error message to display, or null if no error should be shown.
     */
    val errorMessage: String?
        get() = if (shouldShowError) {
            (validationResult as? ValidationResult.Invalid)?.message
        } else {
            null
        }

    /**
     * Remaining characters if maxLength is set, null otherwise.
     */
    val remainingCharacters: Int?
        get() = maxLength?.let { it - value.length }

    /**
     * Updates the value. If maxLength is set, enforces the limit.
     */
    fun onValueChange(newValue: String) {
        value = if (maxLength != null && newValue.length > maxLength) {
            newValue.take(maxLength)
        } else {
            newValue
        }
    }

    /**
     * Call when field focus changes.
     * Marks the field as touched when focus is lost.
     */
    fun onFocusChanged(focused: Boolean) {
        if (isFocused && !focused) {
            // Lost focus - mark as touched
            isTouched = true
        }
        isFocused = focused
    }

    /**
     * Manually marks the field as touched.
     * Useful for form submission validation.
     */
    fun touch() {
        isTouched = true
    }

    /**
     * Resets the state to the given value.
     */
    fun reset(newValue: String = "") {
        value = newValue
        isTouched = false
        isFocused = false
    }
}
