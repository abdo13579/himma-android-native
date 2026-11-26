package com.himma.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeekScheduleHelper
import com.himma.app.domain.model.WeightUnit
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun completeOnboarding(
        language: AppLanguage,
        weightUnit: WeightUnit,
        defaultRestSeconds: Int,
    )

    suspend fun setLanguage(language: AppLanguage)
    suspend fun setWeightUnit(weightUnit: WeightUnit)
    suspend fun setDefaultRestSeconds(seconds: Int)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setUseDynamicColor(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setLastAutoBackupAt(timestampMillis: Long?)
    suspend fun setAutoCheckUpdates(enabled: Boolean)
    suspend fun setAnimationsEnabled(enabled: Boolean)
    suspend fun setWeekStartDay(dayOfWeek: DayOfWeek)
    suspend fun setSeedCatalogVersion(version: Int)
    suspend fun restorePreferences(
        language: AppLanguage,
        weightUnit: WeightUnit,
        defaultRestSeconds: Int,
        themeMode: ThemeMode,
        useDynamicColor: Boolean,
        weekStartDay: DayOfWeek = WeekScheduleHelper.DEFAULT_WEEK_START,
    )
}

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_AUTO_BACKUP_AT = longPreferencesKey("last_auto_backup_at")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val WEEK_START_DAY = stringPreferencesKey("week_start_day")
        val SEED_CATALOG_VERSION = intPreferencesKey("seed_catalog_version")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserPreferences(
                language = AppLanguage.fromTag(prefs[Keys.LANGUAGE]),
                weightUnit = WeightUnit.fromCode(prefs[Keys.WEIGHT_UNIT]),
                defaultRestSeconds = (prefs[Keys.DEFAULT_REST_SECONDS] ?: UserPreferences.DEFAULT_REST_SECONDS)
                    .coerceIn(15, 600),
                themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
                useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
                lastAutoBackupAt = prefs[Keys.LAST_AUTO_BACKUP_AT],
                autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
                animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
                weekStartDay = WeekScheduleHelper.dayFromNameOrDefault(prefs[Keys.WEEK_START_DAY]),
                seedCatalogVersion = prefs[Keys.SEED_CATALOG_VERSION] ?: 0,
            )
        }

    override suspend fun completeOnboarding(
        language: AppLanguage,
        weightUnit: WeightUnit,
        defaultRestSeconds: Int,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.tag
            prefs[Keys.WEIGHT_UNIT] = weightUnit.code
            prefs[Keys.DEFAULT_REST_SECONDS] = defaultRestSeconds.coerceIn(15, 600)
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.tag }
    }

    override suspend fun setWeightUnit(weightUnit: WeightUnit) {
        dataStore.edit { it[Keys.WEIGHT_UNIT] = weightUnit.code }
    }

    override suspend fun setDefaultRestSeconds(seconds: Int) {
        dataStore.edit { it[Keys.DEFAULT_REST_SECONDS] = seconds.coerceIn(15, 600) }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.key }
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    override suspend fun setLastAutoBackupAt(timestampMillis: Long?) {
        dataStore.edit { prefs ->
            if (timestampMillis == null) {
                prefs.remove(Keys.LAST_AUTO_BACKUP_AT)
            } else {
                prefs[Keys.LAST_AUTO_BACKUP_AT] = timestampMillis
            }
        }
    }

    override suspend fun setAutoCheckUpdates(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_CHECK_UPDATES] = enabled }
    }

    override suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    }

    override suspend fun setWeekStartDay(dayOfWeek: DayOfWeek) {
        dataStore.edit { it[Keys.WEEK_START_DAY] = dayOfWeek.name }
    }

    override suspend fun setSeedCatalogVersion(version: Int) {
        dataStore.edit { it[Keys.SEED_CATALOG_VERSION] = version }
    }

    override suspend fun restorePreferences(
        language: AppLanguage,
        weightUnit: WeightUnit,
        defaultRestSeconds: Int,
        themeMode: ThemeMode,
        useDynamicColor: Boolean,
        weekStartDay: DayOfWeek,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.tag
            prefs[Keys.WEIGHT_UNIT] = weightUnit.code
            prefs[Keys.DEFAULT_REST_SECONDS] = defaultRestSeconds.coerceIn(15, 600)
            prefs[Keys.THEME_MODE] = themeMode.key
            prefs[Keys.USE_DYNAMIC_COLOR] = useDynamicColor
            prefs[Keys.WEEK_START_DAY] = weekStartDay.name
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }
}
