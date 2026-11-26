package com.himma.app.core.datastore

import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeekScheduleHelper
import com.himma.app.domain.model.WeightUnit
import java.time.DayOfWeek

data class UserPreferences(
    val language: AppLanguage = AppLanguage.ARABIC,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS,
    val themeMode: ThemeMode = ThemeMode.AMOLED_DARK,
    val useDynamicColor: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val lastAutoBackupAt: Long? = null,
    val autoCheckUpdates: Boolean = true,
    val animationsEnabled: Boolean = true,
    val weekStartDay: DayOfWeek = WeekScheduleHelper.DEFAULT_WEEK_START,
    val seedCatalogVersion: Int = 0,
) {
    companion object {
        const val DEFAULT_REST_SECONDS = 90
        const val CURRENT_SEED_CATALOG_VERSION = 3
        val REST_PRESETS = listOf(60, 90, 120, 180)
    }
}
