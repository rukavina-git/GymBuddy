package com.rukavina.gymbuddy.utils

import com.rukavina.gymbuddy.data.model.PreferredUnits

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
     * Convert weight from metric (kg) to user's preferred units
     */
    fun weightToDisplayUnit(kg: Float?, preferredUnits: PreferredUnits): String {
        if (kg == null) return ""
        return when (preferredUnits) {
            PreferredUnits.METRIC -> String.format("%.1f", kg)
            PreferredUnits.IMPERIAL -> String.format("%.1f", kgToLbs(kg))
        }
    }

    /**
     * Convert height from metric (cm) to user's preferred units
     */
    fun heightToDisplayUnit(cm: Float?, preferredUnits: PreferredUnits): String {
        if (cm == null) return ""
        return when (preferredUnits) {
            PreferredUnits.METRIC -> String.format("%.0f", cm)
            PreferredUnits.IMPERIAL -> String.format("%.1f", cmToInches(cm))
        }
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
