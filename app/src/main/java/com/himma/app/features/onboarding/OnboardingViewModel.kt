package com.himma.app.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.UserProfileRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.WeightUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = UserPreferences.DEFAULT_REST_SECONDS,
    val displayName: String = "",
    val bodyWeightInput: String = "",
    val bodyWeightError: Boolean = false,
    val isSubmitting: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val profileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val profile = profileRepository.profile.first()
            val formattedWeight = profile?.bodyWeightKg?.let { kg ->
                WeightUnit.toDisplayWeight(kg, prefs.weightUnit).toString()
            }.orEmpty()

            _uiState.update {
                it.copy(
                    language = prefs.language,
                    weightUnit = prefs.weightUnit,
                    defaultRestSeconds = prefs.defaultRestSeconds,
                    displayName = profile?.displayName.orEmpty(),
                    bodyWeightInput = formattedWeight,
                )
            }
        }
    }

    fun selectLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun selectWeightUnit(unit: WeightUnit) {
        _uiState.update { it.copy(weightUnit = unit) }
    }

    fun selectRestSeconds(seconds: Int) {
        _uiState.update { it.copy(defaultRestSeconds = seconds) }
    }

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun updateBodyWeightInput(input: String) {
        _uiState.update { it.copy(bodyWeightInput = input, bodyWeightError = false) }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val parsedWeightResult = WeightUnit.parseOptionalPositiveWeight(state.bodyWeightInput)
        if (parsedWeightResult.isFailure) {
            _uiState.update { it.copy(bodyWeightError = true) }
            return
        }

        val weightKg = parsedWeightResult.getOrNull()?.let { entered ->
            WeightUnit.toStorageKg(entered, state.weightUnit)
        }

        _uiState.update { it.copy(isSubmitting = true, bodyWeightError = false) }
        viewModelScope.launch {
            profileRepository.saveProfile(
                displayName = state.displayName,
                bodyWeightKg = weightKg,
            )
            preferencesRepository.completeOnboarding(
                language = state.language,
                weightUnit = state.weightUnit,
                defaultRestSeconds = state.defaultRestSeconds,
            )
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }
}
