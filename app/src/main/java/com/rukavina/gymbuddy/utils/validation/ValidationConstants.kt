package com.rukavina.gymbuddy.utils.validation

/**
 * Central place for all validation constants, regex patterns, and input limits.
 */
object ValidationConstants {
    // Text field limits
    const val MAX_NAME_LENGTH = 50
    const val MAX_TITLE_LENGTH = 50
    const val MAX_TITLE_LINES = 2

    // Numeric input limits
    const val MAX_REPS_DIGITS = 2          // 0-99
    const val MAX_WEIGHT_WHOLE_DIGITS = 3  // 0-999
    const val MAX_WEIGHT_DECIMAL_DIGITS = 2 // .00-.99
    const val MAX_HOURS = 23
    const val MAX_MINUTES = 59
    const val MAX_SECONDS = 59

    // Regex patterns
    val WEIGHT_REGEX = Regex("""^\d{0,3}(\.\d{0,2})?$""")
    val REPS_REGEX = Regex("""^\d{0,2}$""")
    val DIGITS_ONLY_REGEX = Regex("""^\d*$""")
}

/**
 * Validation helper functions for input fields.
 */
object InputValidation {

    /**
     * Validates and formats reps input.
     * Allows only digits, max 2 characters (0-99).
     */
    fun validateReps(input: String): String {
        return input.filter { it.isDigit() }.take(ValidationConstants.MAX_REPS_DIGITS)
    }

    /**
     * Validates weight input.
     * Allows max 3 digits before decimal, 2 after (e.g., 999.99).
     * Returns null if invalid, otherwise returns the valid input.
     */
    fun validateWeight(input: String): String? {
        return if (input.isEmpty() || input.matches(ValidationConstants.WEIGHT_REGEX)) {
            input
        } else {
            null
        }
    }

    /**
     * Validates hour input (0-23).
     */
    fun validateHours(input: String): Int? {
        val value = input.filter { it.isDigit() }.toIntOrNull() ?: return null
        return if (value <= ValidationConstants.MAX_HOURS) value else null
    }

    /**
     * Validates minute/second input (0-59).
     */
    fun validateMinutesOrSeconds(input: String): Int? {
        val value = input.filter { it.isDigit() }.toIntOrNull() ?: return null
        return if (value <= ValidationConstants.MAX_MINUTES) value else null
    }

    /**
     * Validates name/title input.
     * Limits to MAX_NAME_LENGTH characters.
     */
    fun validateName(input: String): String {
        return input.take(ValidationConstants.MAX_NAME_LENGTH)
    }

    /**
     * Validates title input.
     * Limits to MAX_TITLE_LENGTH characters.
     */
    fun validateTitle(input: String): String {
        return input.take(ValidationConstants.MAX_TITLE_LENGTH)
    }
}
