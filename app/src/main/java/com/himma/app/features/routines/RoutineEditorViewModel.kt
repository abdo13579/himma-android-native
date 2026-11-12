package com.himma.app.features.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.database.RoutineRepository
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.RoutineExercise
import com.himma.app.domain.model.RoutineExerciseDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineEditorFormState(
    val routineId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val notes: String = "",
    val titleError: Boolean = false,
    val exercises: List<RoutineExerciseDraft> = emptyList(),
    val isPickerOpen: Boolean = false,
)

data class RoutinePickerFilterState(
    val query: String = "",
    val selectedMuscle: MuscleGroup? = null,
    val selectedEquipment: EquipmentType? = null,
)

data class RoutineEditorUiState(
    val form: RoutineEditorFormState = RoutineEditorFormState(),
    val language: AppLanguage = AppLanguage.ARABIC,
    val pickerFilter: RoutinePickerFilterState = RoutinePickerFilterState(),
    val pickerExercises: List<Exercise> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val editingRoutineId: String? = savedStateHandle.get<String>("routineId")
        ?.takeIf { it.isNotBlank() }

    private val formState = MutableStateFlow(
        RoutineEditorFormState(
            routineId = editingRoutineId,
            isLoading = editingRoutineId != null,
        ),
    )

    private val pickerFilterState = MutableStateFlow(RoutinePickerFilterState())

    private val pickerExercisesFlow = pickerFilterState.flatMapLatest { filter ->
        exerciseRepository.observeExercises(
            query = filter.query,
            muscleFilter = filter.selectedMuscle,
            equipmentFilter = filter.selectedEquipment,
        )
    }

    val uiState: StateFlow<RoutineEditorUiState> = combine(
        formState,
        userPreferencesRepository.preferences,
        pickerFilterState,
        pickerExercisesFlow,
    ) { form, prefs, filter, exercises ->
        RoutineEditorUiState(
            form = form,
            language = prefs.language,
            pickerFilter = filter,
            pickerExercises = exercises,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutineEditorUiState(form = formState.value),
    )

    init {
        viewModelScope.launch {
            exerciseRepository.ensureSeeded()
        }
        if (editingRoutineId != null) {
            viewModelScope.launch {
                val existing = routineRepository.getRoutineById(editingRoutineId)
                if (existing != null) {
                    formState.update {
                        it.copy(
                            routineId = existing.id,
                            isLoading = false,
                            title = existing.title,
                            notes = existing.notes,
                            exercises = existing.exercises.map { item ->
                                RoutineExerciseDraft(
                                    itemId = item.id,
                                    exercise = item.exercise,
                                    targetSets = item.targetSets,
                                    targetRepsMin = item.targetRepsMin,
                                    targetRepsMax = item.targetRepsMax,
                                    targetRestSeconds = item.targetRestSeconds,
                                )
                            },
                        )
                    }
                } else {
                    formState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateTitle(value: String) {
        formState.update { it.copy(title = value, titleError = false) }
    }

    fun updateNotes(value: String) {
        formState.update { it.copy(notes = value) }
    }

    fun setPickerOpen(open: Boolean) {
        formState.update { it.copy(isPickerOpen = open) }
    }

    fun updatePickerQuery(query: String) {
        pickerFilterState.update { it.copy(query = query) }
    }

    fun selectPickerMuscle(muscle: MuscleGroup?) {
        pickerFilterState.update { current ->
            current.copy(selectedMuscle = if (current.selectedMuscle == muscle) null else muscle)
        }
    }

    fun selectPickerEquipment(equipment: EquipmentType?) {
        pickerFilterState.update { current ->
            current.copy(selectedEquipment = if (current.selectedEquipment == equipment) null else equipment)
        }
    }

    fun clearPickerFilters() {
        pickerFilterState.value = RoutinePickerFilterState()
    }

    fun toggleExerciseFromPicker(exercise: Exercise) {
        formState.update { current ->
            val exists = current.exercises.any { it.exercise.id == exercise.id }
            val updated = if (exists) {
                current.exercises.filterNot { it.exercise.id == exercise.id }
            } else {
                current.exercises + RoutineExerciseDraft(
                    itemId = UUID.randomUUID().toString(),
                    exercise = exercise,
                )
            }
            current.copy(exercises = updated)
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        formState.update { current ->
            val list = current.exercises
            if (fromIndex == toIndex ||
                fromIndex !in list.indices ||
                toIndex !in list.indices
            ) {
                return@update current
            }
            val mutable = list.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)
            current.copy(exercises = mutable)
        }
    }

    fun removeExercise(itemId: String) {
        formState.update { current ->
            current.copy(exercises = current.exercises.filterNot { it.itemId == itemId })
        }
    }

    fun adjustSets(itemId: String, delta: Int) {
        updateExerciseItem(itemId) { draft ->
            draft.copy(targetSets = draft.targetSets + delta)
        }
    }

    fun adjustRepsMin(itemId: String, delta: Int) {
        updateExerciseItem(itemId) { draft ->
            val newMin = (draft.targetRepsMin + delta).coerceIn(
                RoutineExercise.MIN_REPS,
                RoutineExercise.MAX_REPS,
            )
            val newMax = maxOf(newMin, draft.targetRepsMax)
            draft.copy(targetRepsMin = newMin, targetRepsMax = newMax)
        }
    }

    fun adjustRepsMax(itemId: String, delta: Int) {
        updateExerciseItem(itemId) { draft ->
            val newMax = (draft.targetRepsMax + delta).coerceIn(
                RoutineExercise.MIN_REPS,
                RoutineExercise.MAX_REPS,
            )
            val newMin = minOf(draft.targetRepsMin, newMax)
            draft.copy(targetRepsMin = newMin, targetRepsMax = newMax)
        }
    }

    fun adjustRestSeconds(itemId: String, deltaSeconds: Int) {
        updateExerciseItem(itemId) { draft ->
            draft.copy(targetRestSeconds = draft.targetRestSeconds + deltaSeconds)
        }
    }

    private fun updateExerciseItem(
        itemId: String,
        transform: (RoutineExerciseDraft) -> RoutineExerciseDraft,
    ) {
        formState.update { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.itemId == itemId) transform(item).normalized() else item
                },
            )
        }
    }

    fun saveRoutine(onSaved: () -> Unit) {
        val current = formState.value
        if (current.isSaving) return

        val trimmedTitle = current.title.trim()
        if (trimmedTitle.isEmpty()) {
            formState.update { it.copy(titleError = true) }
            return
        }

        formState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            routineRepository.saveRoutine(
                existingId = current.routineId,
                title = trimmedTitle,
                notes = current.notes,
                exercises = current.exercises,
            )
            formState.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
