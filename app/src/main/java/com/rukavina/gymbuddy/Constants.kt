package com.rukavina.gymbuddy

/**
 * Central location for all app-wide constants.
 * Keep all limits, sizes, and configuration values here.
 */
object Constants {

    // Exercise Form Limits
    object Exercise {
        const val MAX_NAME_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 500
        const val MAX_INSTRUCTIONS = 10
        const val MAX_INSTRUCTION_LENGTH = 250
        const val MAX_TIPS = 5
        const val MAX_TIP_LENGTH = 150
    }

    // Profile Field Limits
    object Profile {
        const val MAX_NAME_LENGTH = 50
        const val MAX_BIO_LENGTH = 200
    }

    // Body Measurement Limits (stored in metric)
    object Measurements {
        // Height in cm (102cm = 3'4", 229cm = 7'6")
        const val MIN_HEIGHT_CM = 102
        const val MAX_HEIGHT_CM = 229

        // Weight in kg (30kg = 66lbs, 300kg = 661lbs)
        const val MIN_WEIGHT_KG = 30
        const val MAX_WEIGHT_KG = 300
    }
}
