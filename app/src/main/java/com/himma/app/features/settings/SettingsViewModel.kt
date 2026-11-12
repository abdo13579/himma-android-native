package com.himma.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.UserProfileRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeightUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val displayNameInput: String = "",
    val bodyWeightInput: String = "",
    val bodyWeightError: Boolean = false,
    val isInitialized: Boolean = false,
)

sealed interface SettingsEvent {
    data object ProfileSaved : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val profileRepository: UserProfileRepository,
) : ViewModel() {

    private data class DraftInputs(
        val displayName: String? = null,
        val bodyWeight: String? = null,
        val bodyWeightError: Boolean = false,
    )

    private val drafts = MutableStateFlow(DraftInputs())
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        profileRepository.profile,
        drafts,
    ) { prefs, profile, draft ->
        val defaultWeightText = profile?.bodyWeightKg?.let { kg ->
            WeightUnit.toDisplayWeight(kg, prefs.weightUnit).toString()
        }.orEmpty()

        SettingsUiState(
            preferences = prefs,
            displayNameInput = draft.displayName ?: profile?.displayName.orEmpty(),
            bodyWeightInput = draft.bodyWeight ?: defaultWeightText,
            bodyWeightError = draft.bodyWeightError,
            isInitialized = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            preferencesRepository.setWeightUnit(unit)
            drafts.update { it.copy(bodyWeight = null, bodyWeightError = false) }
        }
    }

    fun setDefaultRestSeconds(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultRestSeconds(seconds)
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseDynamicColor(enabled)
        }
    }

    fun updateDisplayName(name: String) {
        drafts.update { it.copy(displayName = name) }
    }

    fun updateBodyWeightInput(input: String) {
        drafts.update { it.copy(bodyWeight = input, bodyWeightError = false) }
    }

    fun saveProfile() {
        val draftSnapshot = drafts.value
        val stateSnapshot = uiState.value
        val rawName = draftSnapshot.displayName ?: stateSnapshot.displayNameInput
        val rawWeight = draftSnapshot.bodyWeight ?: stateSnapshot.bodyWeightInput

        val parsedResult = WeightUnit.parseOptionalPositiveWeight(rawWeight)
        if (parsedResult.isFailure) {
            drafts.update { it.copy(bodyWeightError = true) }
            return
        }

        viewModelScope.launch {
            val activeUnit = preferencesRepository.preferences.first().weightUnit
            val weightKg = parsedResult.getOrNull()?.let { entered ->
                WeightUnit.toStorageKg(entered, activeUnit)
            }
            profileRepository.saveProfile(
                displayName = rawName,
                bodyWeightKg = weightKg,
            )
            drafts.value = DraftInputs()
            _events.emit(SettingsEvent.ProfileSaved)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
        }
    }
}
