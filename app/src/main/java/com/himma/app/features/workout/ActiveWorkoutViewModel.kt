package com.himma.app.features.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.database.SetCompletionResult
import com.himma.app.core.database.WorkoutRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.FinishedWorkoutSummary
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.PreviousSetHint
import com.himma.app.domain.model.SetType
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WorkoutExerciseGroup
import com.himma.app.domain.model.WorkoutMath
import com.himma.app.domain.model.WorkoutSession
import com.himma.app.domain.model.WorkoutSet
import com.himma.app.features.routines.RoutinePickerFilterState
import com.himma.app.service.RestTimerController
import com.himma.app.service.RestTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSession? = null,
    val preferences: UserPreferences = UserPreferences(),
    val previousHints: Map<Pair<String, Int>, PreviousSetHint> = emptyMap(),
    val restTimer: RestTimerState = RestTimerState(),
    val isPickerOpen: Boolean = false,
    val pickerFilter: RoutinePickerFilterState = RoutinePickerFilterState(),
    val pickerExercises: List<Exercise> = emptyList(),
) {
    val language: AppLanguage get() = preferences.language
    val weightUnit: WeightUnit get() = preferences.weightUnit
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    preferencesRepository: UserPreferencesRepository,
    private val restTimerController: RestTimerController,
) : ViewModel() {

    private val workoutId: String = checkNotNull(savedStateHandle.get<String>("workoutId"))

    private val isPickerOpenFlow = MutableStateFlow(false)
    private val pickerFilterFlow = MutableStateFlow(RoutinePickerFilterState())
    private val previousHintsFlow = MutableStateFlow<Map<Pair<String, Int>, PreviousSetHint>>(emptyMap())

    private val sessionFlow = workoutRepository.observeWorkoutById(workoutId)

    private val pickerExercisesFlow = pickerFilterFlow.flatMapLatest { filter ->
        exerciseRepository.observeExercises(
            query = filter.query,
            muscleFilter = filter.selectedMuscle,
            equipmentFilter = filter.selectedEquipment,
        )
    }

    private val headerAndSessionFlow = combine(
        sessionFlow,
        preferencesRepository.preferences,
        previousHintsFlow,
        restTimerController.state,
    ) { session, prefs, hints, restTimer ->
        ActiveWorkoutUiState(
            isLoading = false,
            session = session,
            preferences = prefs,
            previousHints = hints,
            restTimer = restTimer,
        )
    }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        headerAndSessionFlow,
        isPickerOpenFlow,
        pickerFilterFlow,
        pickerExercisesFlow,
    ) { base, isPickerOpen, pickerFilter, pickerExercises ->
        base.copy(
            isPickerOpen = isPickerOpen,
            pickerFilter = pickerFilter,
            pickerExercises = pickerExercises,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActiveWorkoutUiState(),
    )

    init {
        viewModelScope.launch {
            exerciseRepository.ensureSeeded()
        }
        viewModelScope.launch {
            sessionFlow
                .map { session -> session?.exercises?.map { it.exercise.id }?.distinct().orEmpty() }
                .distinctUntilChanged()
                .collect { exerciseIds ->
                    if (exerciseIds.isNotEmpty()) {
                        previousHintsFlow.value = workoutRepository.getPreviousHintsForExercises(exerciseIds)
                    }
                }
        }
    }

    fun updateWeightFromInput(set: WorkoutSet, rawInput: String) {
        val parsedDisplay = parsePositiveOrZeroDecimal(rawInput)
        val weightKg = parsedDisplay?.let { WeightUnit.toStorageKg(it, uiState.value.weightUnit) }
        viewModelScope.launch {
            workoutRepository.updateSetValues(
                setId = set.id,
                weightKg = weightKg,
                reps = set.reps,
                setType = set.setType,
                rpe = set.rpe,
            )
        }
    }

    fun quickIncrementWeight(set: WorkoutSet, deltaDisplayUnit: Double) {
        val unit = uiState.value.weightUnit
        val currentDisplay = set.weightKg?.let { WeightUnit.toDisplayWeight(it, unit) } ?: 0.0
        val nextDisplay = (currentDisplay + deltaDisplayUnit).coerceAtLeast(0.0)
        val nextKg = WeightUnit.toStorageKg(nextDisplay, unit)
        viewModelScope.launch {
            workoutRepository.updateSetValues(
                setId = set.id,
                weightKg = nextKg,
                reps = set.reps,
                setType = set.setType,
                rpe = set.rpe,
            )
        }
    }

    fun updateRepsFromInput(set: WorkoutSet, rawInput: String) {
        val parsedReps = rawInput.trim().toIntOrNull()?.coerceIn(0, 500)
        viewModelScope.launch {
            workoutRepository.updateSetValues(
                setId = set.id,
                weightKg = set.weightKg,
                reps = parsedReps,
                setType = set.setType,
                rpe = set.rpe,
            )
        }
    }

    fun updateSetType(set: WorkoutSet, setType: SetType) {
        viewModelScope.launch {
            workoutRepository.updateSetValues(
                setId = set.id,
                weightKg = set.weightKg,
                reps = set.reps,
                setType = setType,
                rpe = set.rpe,
            )
        }
    }

    fun toggleSetCompleted(set: WorkoutSet, exerciseDisplayName: String) {
        viewModelScope.launch {
            val outcome: SetCompletionResult = workoutRepository.toggleSetCompleted(set.id)
            if (outcome.isCompleted && outcome.shouldStartTimer) {
                restTimerController.start(
                    seconds = outcome.restSeconds,
                    exerciseName = exerciseDisplayName,
                )
            } else if (!outcome.isCompleted) {
                restTimerController.skip()
            }
        }
    }

    fun addSet(group: WorkoutExerciseGroup) {
        viewModelScope.launch {
            workoutRepository.addSetToExercise(
                workoutId = workoutId,
                exerciseId = group.exercise.id,
                exerciseOrder = group.exerciseOrder,
                restSeconds = group.restSeconds,
            )
        }
    }

    fun removeSet(setId: String) {
        viewModelScope.launch {
            workoutRepository.removeSet(setId)
        }
    }

    fun removeExercise(exerciseId: String) {
        viewModelScope.launch {
            workoutRepository.removeExerciseFromWorkout(workoutId, exerciseId)
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutNotes(workoutId, notes)
        }
    }

    fun setPickerOpen(open: Boolean) {
        isPickerOpenFlow.value = open
    }

    fun updatePickerQuery(query: String) {
        pickerFilterFlow.update { it.copy(query = query) }
    }

    fun selectPickerMuscle(muscle: MuscleGroup?) {
        pickerFilterFlow.update { current ->
            current.copy(selectedMuscle = if (current.selectedMuscle == muscle) null else muscle)
        }
    }

    fun selectPickerEquipment(equipment: EquipmentType?) {
        pickerFilterFlow.update { current ->
            current.copy(selectedEquipment = if (current.selectedEquipment == equipment) null else equipment)
        }
    }

    fun clearPickerFilters() {
        pickerFilterFlow.value = RoutinePickerFilterState()
    }

    fun addExerciseFromPicker(exercise: Exercise) {
        val userDefaultRest = uiState.value.preferences.defaultRestSeconds
        val resolvedRest = WorkoutMath.resolveRestSeconds(
            routineRestSeconds = null,
            exerciseDefaultRestSeconds = exercise.defaultRestSeconds,
            userDefaultRestSeconds = userDefaultRest,
        )
        viewModelScope.launch {
            workoutRepository.addExerciseToWorkout(
                workoutId = workoutId,
                exercise = exercise,
                targetSets = 3,
                restSeconds = resolvedRest,
            )
            isPickerOpenFlow.value = false
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        restTimerController.adjust(deltaSeconds)
    }

    fun skipRestTimer() {
        restTimerController.skip()
    }

    fun finishWorkout(onFinished: (FinishedWorkoutSummary) -> Unit) {
        restTimerController.skip()
        viewModelScope.launch {
            val summary = workoutRepository.finishWorkout(workoutId)
            if (summary != null) {
                onFinished(summary)
            }
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        restTimerController.skip()
        viewModelScope.launch {
            workoutRepository.discardWorkout(workoutId)
            onDiscarded()
        }
    }

    private fun parsePositiveOrZeroDecimal(rawInput: String): Double? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null
        val normalized = trimmed.replace('٫', '.').replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value.isFinite() && value >= 0.0 && value <= 2000.0) value else null
    }
}
