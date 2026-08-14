package com.rukavina.gymbuddy.utils

import com.rukavina.gymbuddy.domain.model.PreferredUnits

/**
 * Utility object for converting between metric and imperial units.
 *
 * Database Storage Convention:
 * - All weights stored in kilograms (kg)
 * - All heights stored in centimeters (cm)
 * - Conversion to imperial units happens only at the UI layer
 */
object UnitConverter {

    private const val KG_TO_LBS_FACTOR = 2.20462f
    private const val CM_TO_INCHES_FACTOR = 2.54f

    // Weight conversions
    fun kgToLbs(kg: Float): Float = kg * KG_TO_LBS_FACTOR
    fun lbsToKg(lbs: Float): Float = lbs / KG_TO_LBS_FACTOR

    // Height conversions
    fun cmToInches(cm: Float): Float = cm / CM_TO_INCHES_FACTOR
    fun inchesToCm(inches: Float): Float = inches * CM_TO_INCHES_FACTOR

    /**
     * Format a value with a fixed number of decimals, then trim trailing
     * zeros (and a trailing decimal point) so "20.00" reads as "20" and
     * "22.0500" reads as "22.05". Rounding here is display-only - the
     * underlying stored value is never touched.
     */
    private fun formatTrimmed(value: Float, decimals: Int): String {
        val formatted = String.format("%.${decimals}f", value)
        return if (formatted.contains('.')) {
            formatted.trimEnd('0').trimEnd('.')
        } else {
            formatted
        }
    }

    /**
     * Convert weight from metric (kg) to user's preferred units
     */
    fun weightToDisplayUnit(kg: Float?, preferredUnits: PreferredUnits): String {
        if (kg == null) return ""
        val value = when (preferredUnits) {
            PreferredUnits.METRIC -> kg
            PreferredUnits.IMPERIAL -> kgToLbs(kg)
        }
        return formatTrimmed(value, 2)
    }

    /**
     * Convert height from metric (cm) to user's preferred units
     */
    fun heightToDisplayUnit(cm: Float?, preferredUnits: PreferredUnits): String {
        if (cm == null) return ""
        val value = when (preferredUnits) {
            PreferredUnits.METRIC -> cm
            PreferredUnits.IMPERIAL -> cmToInches(cm)
        }
        return formatTrimmed(value, 2)
    }

    /**
     * Convert weight from display units to metric (kg) for storage
     */
    fun weightToMetric(value: String, preferredUnits: PreferredUnits): Float? {
        val weight = value.toFloatOrNull() ?: return null
        return when (preferredUnits) {
            PreferredUnits.METRIC -> weight
            PreferredUnits.IMPERIAL -> lbsToKg(weight)
        }
    }

    /**
     * Convert height from display units to metric (cm) for storage
     */
    fun heightToMetric(value: String, preferredUnits: PreferredUnits): Float? {
        val height = value.toFloatOrNull() ?: return null
        return when (preferredUnits) {
            PreferredUnits.METRIC -> height
            PreferredUnits.IMPERIAL -> inchesToCm(height)
        }
    }

    /**
     * Get weight unit label for display
     */
    fun getWeightUnitLabel(preferredUnits: PreferredUnits): String {
        return when (preferredUnits) {
            PreferredUnits.METRIC -> "kg"
            PreferredUnits.IMPERIAL -> "lbs"
        }
    }

    /**
     * Get height unit label for display
     */
    fun getHeightUnitLabel(preferredUnits: PreferredUnits): String {
        return when (preferredUnits) {
            PreferredUnits.METRIC -> "cm"
            PreferredUnits.IMPERIAL -> "inches"
        }
    }
}
