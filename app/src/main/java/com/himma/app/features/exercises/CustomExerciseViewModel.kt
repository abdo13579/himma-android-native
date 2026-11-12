package com.himma.app.features.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomExerciseUiState(
    val editingId: String? = null,
    val nameAr: String = "",
    val nameEn: String = "",
    val primaryMuscle: MuscleGroup = MuscleGroup.CHEST,
    val secondaryMuscles: Set<MuscleGroup> = emptySet(),
    val equipment: EquipmentType = EquipmentType.DUMBBELL,
    val defaultRestSeconds: Int = UserPreferences.DEFAULT_REST_SECONDS,
    val instructionsAr: String = "",
    val instructionsEn: String = "",
    val nameError: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface CustomExerciseEvent {
    data class Saved(val exerciseId: String) : CustomExerciseEvent
}

@HiltViewModel
class CustomExerciseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val editingExerciseId: String? = savedStateHandle.get<String>("exerciseId")
        ?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(CustomExerciseUiState(editingId = editingExerciseId))
    val uiState: StateFlow<CustomExerciseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CustomExerciseEvent>()
    val events: SharedFlow<CustomExerciseEvent> = _events.asSharedFlow()

    init {
        if (editingExerciseId != null) {
            viewModelScope.launch {
                val existing = exerciseRepository.getExerciseById(editingExerciseId)
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            editingId = existing.id,
                            nameAr = existing.nameAr,
                            nameEn = existing.nameEn,
                            primaryMuscle = existing.primaryMuscle,
                            secondaryMuscles = existing.secondaryMuscles.toSet(),
                            equipment = existing.equipment,
                            defaultRestSeconds = existing.defaultRestSeconds,
                            instructionsAr = existing.instructionsAr,
                            instructionsEn = existing.instructionsEn,
                        )
                    }
                }
            }
        }
    }

    fun updateNameAr(value: String) {
        _uiState.update { it.copy(nameAr = value, nameError = false) }
    }

    fun updateNameEn(value: String) {
        _uiState.update { it.copy(nameEn = value, nameError = false) }
    }

    fun selectPrimaryMuscle(muscle: MuscleGroup) {
        _uiState.update { state ->
            state.copy(
                primaryMuscle = muscle,
                secondaryMuscles = state.secondaryMuscles - muscle,
            )
        }
    }

    fun toggleSecondaryMuscle(muscle: MuscleGroup) {
        _uiState.update { state ->
            if (muscle == state.primaryMuscle) return@update state
            val updated = if (muscle in state.secondaryMuscles) {
                state.secondaryMuscles - muscle
            } else {
                state.secondaryMuscles + muscle
            }
            state.copy(secondaryMuscles = updated)
        }
    }

    fun selectEquipment(equipment: EquipmentType) {
        _uiState.update { it.copy(equipment = equipment) }
    }

    fun selectRestSeconds(seconds: Int) {
        _uiState.update { it.copy(defaultRestSeconds = seconds) }
    }

    fun updateInstructionsAr(value: String) {
        _uiState.update { it.copy(instructionsAr = value) }
    }

    fun updateInstructionsEn(value: String) {
        _uiState.update { it.copy(instructionsEn = value) }
    }

    fun saveExercise() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.nameAr.isBlank() && state.nameEn.isBlank()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true, nameError = false) }
        viewModelScope.launch {
            val savedId = exerciseRepository.saveCustomExercise(
                existingId = state.editingId,
                nameAr = state.nameAr,
                nameEn = state.nameEn,
                primaryMuscle = state.primaryMuscle,
                secondaryMuscles = state.secondaryMuscles.toList(),
                equipment = state.equipment,
                defaultRestSeconds = state.defaultRestSeconds,
                instructionsAr = state.instructionsAr,
                instructionsEn = state.instructionsEn,
            )
            _uiState.update { it.copy(isSaving = false) }
            _events.emit(CustomExerciseEvent.Saved(savedId))
        }
    }
}
