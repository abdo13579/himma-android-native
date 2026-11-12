package com.himma.app.domain.model

import kotlin.math.roundToInt

enum class WeightUnit(val code: String) {
    KG("kg"),
    LBS("lbs");

    companion object {
        private const val KG_TO_LBS = 2.2046226

        fun fromCode(code: String?): WeightUnit {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: KG
        }

        /**
         * Converts a stored weight in kilograms to the user's preferred display unit,
         * rounded to 1 decimal place.
         */
        fun toDisplayWeight(weightKg: Double, targetUnit: WeightUnit): Double {
            val raw = when (targetUnit) {
                KG -> weightKg
                LBS -> weightKg * KG_TO_LBS
            }
            return (raw * 10.0).roundToInt() / 10.0
        }

        /**
         * Converts a user-entered weight in [sourceUnit] to canonical kilograms for Room storage.
         */
        fun toStorageKg(displayWeight: Double, sourceUnit: WeightUnit): Double {
            return when (sourceUnit) {
                KG -> displayWeight
                LBS -> displayWeight / KG_TO_LBS
            }
        }

        /**
         * Parses an optional weight input string (supporting both Latin and Arabic decimal separators).
         * Returns null when blank, or Result.failure if invalid/non-positive.
         */
        fun parseOptionalPositiveWeight(rawInput: String): Result<Double?> {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) return Result.success(null)
            val normalized = trimmed
                .replace('٫', '.')
                .replace(',', '.')
            val parsed = normalized.toDoubleOrNull()
            return if (parsed != null && parsed.isFinite() && parsed > 0.0 && parsed <= 500.0) {
                Result.success(parsed)
            } else {
                Result.failure(IllegalArgumentException("Invalid body weight"))
            }
        }
    }
}
