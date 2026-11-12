package com.himma.app.domain.model

enum class MuscleGroup {
    CHEST,
    BACK,
    LEGS,
    SHOULDERS,
    ARMS,
    CORE;

    companion object {
        fun fromNameOrNull(raw: String?): MuscleGroup? {
            return entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
        }
    }
}

enum class EquipmentType {
    BARBELL,
    DUMBBELL,
    CABLE,
    MACHINE,
    BODYWEIGHT,
    OTHER;

    companion object {
        val coreTypes: List<EquipmentType> = listOf(
            BARBELL,
            DUMBBELL,
            CABLE,
            MACHINE,
            BODYWEIGHT,
        )

        fun fromNameOrNull(raw: String?): EquipmentType? {
            return entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
        }
    }
}

data class Exercise(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: EquipmentType,
    val instructionsEn: String,
    val instructionsAr: String,
    val localMediaKey: String?,
    val defaultRestSeconds: Int,
    val isCustom: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun primaryDisplayName(language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC -> nameAr.ifBlank { nameEn }
        AppLanguage.ENGLISH -> nameEn.ifBlank { nameAr }
    }

    fun secondaryDisplayName(language: AppLanguage): String? = when (language) {
        AppLanguage.ARABIC -> nameEn.takeIf { it.isNotBlank() && !it.equals(nameAr, ignoreCase = true) }
        AppLanguage.ENGLISH -> nameAr.takeIf { it.isNotBlank() && !it.equals(nameEn, ignoreCase = true) }
    }

    fun instructionsList(language: AppLanguage): List<String> {
        val primaryText = when (language) {
            AppLanguage.ARABIC -> instructionsAr.ifBlank { instructionsEn }
            AppLanguage.ENGLISH -> instructionsEn.ifBlank { instructionsAr }
        }
        return primaryText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    /**
     * Returns the bundled asset URI for frame [frameIndex] (`0` = start position, `1` = end position),
     * or null when this exercise has no bundled local images (e.g. user-created custom exercises).
     */
    fun frameAssetUri(frameIndex: Int): String? {
        val key = localMediaKey?.takeIf { it.isNotBlank() } ?: return null
        val safeFrame = frameIndex.coerceIn(0, 1)
        return "file:///android_asset/exercises/$key/$safeFrame.webp"
    }
}
