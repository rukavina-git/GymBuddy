package com.rukavina.gymbuddy.utils.validation

/**
 * Functional interface for validating string input.
 * Use with [Validators] object for common validation rules.
 */
fun interface Validator {
    fun validate(value: String): ValidationResult
}

/**
 * Result of a validation check.
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * Collection of reusable validators for input fields.
 * Chain multiple validators for comprehensive validation.
 *
 * Usage:
 * ```
 * val validators = listOf(
 *     Validators.required("Name"),
 *     Validators.maxLength(50),
 *     Validators.noSpecialChars()
 * )
 * ```
 */
object Validators {

    /**
     * Validates that the field is not blank.
     * @param fieldName Name to display in error message
     */
    fun required(fieldName: String = "Field") = Validator { value ->
        if (value.isBlank()) ValidationResult.Invalid("$fieldName is required")
        else ValidationResult.Valid
    }

    /**
     * Validates that the value does not exceed maximum length.
     * @param max Maximum allowed characters
     */
    fun maxLength(max: Int) = Validator { value ->
        if (value.length > max) ValidationResult.Invalid("Maximum $max characters")
        else ValidationResult.Valid
    }

    /**
     * Validates that the value meets minimum length.
     * @param min Minimum required characters
     */
    fun minLength(min: Int) = Validator { value ->
        if (value.length < min) ValidationResult.Invalid("Minimum $min characters")
        else ValidationResult.Valid
    }

    /**
     * Validates that the value contains only alphanumeric characters and spaces.
     * Note: This blocks international characters. For names, use [nameCharsOnly] instead.
     */
    fun noSpecialChars() = Validator { value ->
        if (value.contains(Regex("[^a-zA-Z0-9\\s]")))
            ValidationResult.Invalid("Special characters not allowed")
        else ValidationResult.Valid
    }

    /**
     * Validates name input - allows Unicode letters, spaces, hyphens, and apostrophes.
     * Blocks dangerous characters that could be used in injection attacks.
     * Supports international names: Müller, François, Čović, O'Brien, Mary-Jane
     */
    fun nameCharsOnly() = Validator { value ->
        // Block dangerous/injection characters
        val dangerousChars = Regex("[<>\"\\\\/@$%^&*(){}\\[\\]|;:=+~`#!?]")
        if (value.contains(dangerousChars))
            ValidationResult.Invalid("Contains invalid characters")
        else ValidationResult.Valid
    }

    /**
     * Validates that the value contains no emoji characters.
     */
    fun noEmoji() = Validator { value ->
        val emojiPattern = Regex("[\\p{So}\\p{Cn}]|[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]")
        if (value.contains(emojiPattern))
            ValidationResult.Invalid("Emojis not allowed")
        else ValidationResult.Valid
    }

    /**
     * Validates against a custom regex pattern.
     * @param regex The pattern to match
     * @param message Error message if validation fails
     */
    fun pattern(regex: Regex, message: String) = Validator { value ->
        if (!value.matches(regex)) ValidationResult.Invalid(message)
        else ValidationResult.Valid
    }

    /**
     * Validates that the value contains only letters and spaces.
     * Useful for names where numbers aren't allowed.
     */
    fun lettersOnly() = Validator { value ->
        if (value.contains(Regex("[^a-zA-Z\\s]")))
            ValidationResult.Invalid("Only letters allowed")
        else ValidationResult.Valid
    }

    /**
     * Validates email format.
     */
    fun email() = Validator { value ->
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (value.isNotBlank() && !value.matches(emailPattern))
            ValidationResult.Invalid("Invalid email format")
        else ValidationResult.Valid
    }

    /**
     * Validates password strength.
     * Requirements: 8+ chars, uppercase, lowercase, digit
     */
    fun password() = Validator { value ->
        when {
            value.length < PASSWORD_MIN_LENGTH ->
                ValidationResult.Invalid("Password must be at least $PASSWORD_MIN_LENGTH characters")
            !value.any { it.isUpperCase() } ->
                ValidationResult.Invalid("Password must contain an uppercase letter")
            !value.any { it.isLowerCase() } ->
                ValidationResult.Invalid("Password must contain a lowercase letter")
            !value.any { it.isDigit() } ->
                ValidationResult.Invalid("Password must contain a digit")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates username format.
     * Requirements: 4+ chars, lowercase letters and numbers only
     */
    fun username() = Validator { value ->
        when {
            value.length < USERNAME_MIN_LENGTH ->
                ValidationResult.Invalid("Username must be at least $USERNAME_MIN_LENGTH characters")
            !value.matches(Regex("^[a-z0-9]+$")) ->
                ValidationResult.Invalid("Username can only contain lowercase letters and numbers")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Checks if two values match (for password confirmation).
     */
    fun matches(other: String, fieldName: String = "Passwords") = Validator { value ->
        if (value != other) ValidationResult.Invalid("$fieldName do not match")
        else ValidationResult.Valid
    }

    // Constants
    const val PASSWORD_MIN_LENGTH = 8
    const val USERNAME_MIN_LENGTH = 4
}

/**
 * Extension function to check if password is strong (simple boolean check).
 */
fun String.isStrongPassword(): Boolean {
    return length >= Validators.PASSWORD_MIN_LENGTH &&
            any { it.isUpperCase() } &&
            any { it.isLowerCase() } &&
            any { it.isDigit() }
}

/**
 * Extension function to check if username is valid (simple boolean check).
 */
fun String.isValidUsername(): Boolean {
    return length >= Validators.USERNAME_MIN_LENGTH &&
            matches(Regex("^[a-z0-9]+$"))
}
