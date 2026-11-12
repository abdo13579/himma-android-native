package com.himma.app.core.datastore

import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeightUnit

data class UserPreferences(
    val language: AppLanguage = AppLanguage.ARABIC,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS,
    val themeMode: ThemeMode = ThemeMode.AMOLED_DARK,
    val useDynamicColor: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val lastAutoBackupAt: Long? = null,
) {
    companion object {
        const val DEFAULT_REST_SECONDS = 90
        val REST_PRESETS = listOf(60, 90, 120, 180)
    }
}
