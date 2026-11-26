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
import com.himma.app.domain.model.WeightUnitMode
import com.himma.app.domain.model.WorkoutExerciseGroup
import com.himma.app.domain.model.WorkoutMath
import com.himma.app.domain.model.WorkoutSession
import com.himma.app.domain.model.WorkoutSet
import com.himma.app.features.routines.RoutinePickerFilterState
import com.himma.app.service.RestTimerController
import com.himma.app.service.RestTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ConcurrentHashMap
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    private val workoutId: String = savedStateHandle.get<String>("workoutId").orEmpty()
    private val setMutexes = ConcurrentHashMap<String, Mutex>()
    private val latestSetEdits = ConcurrentHashMap<String, WorkoutSet>()
    private val finishOrDiscardMutex = Mutex()

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

    private fun mutexForSet(setId: String): Mutex = setMutexes.getOrPut(setId) { Mutex() }

    private fun currentSetSnapshot(fallback: WorkoutSet): WorkoutSet {
        val fromSession = uiState.value.session
            ?.exercises
            ?.asSequence()
            ?.flatMap { it.sets.asSequence() }
            ?.firstOrNull { it.id == fallback.id }
        return latestSetEdits[fallback.id] ?: fromSession ?: fallback
    }

    fun updateWeightFromInput(set: WorkoutSet, rawInput: String) {
        val parsedDisplay = parsePositiveOrZeroDecimal(rawInput)
        val unit = uiState.value.weightUnit
        viewModelScope.launch {
            mutexForSet(set.id).withLock {
                val base = currentSetSnapshot(set)
                val weightKg = if (base.weightUnitMode == WeightUnitMode.STACK_PLATES) {
                    parsedDisplay
                } else {
                    parsedDisplay?.let { WeightUnit.toStorageKg(it, unit) }
                }
                val updated = base.copy(weightKg = weightKg)
                latestSetEdits[set.id] = updated
                workoutRepository.updateSetValues(
                    setId = base.id,
                    weightKg = updated.weightKg,
                    reps = updated.reps,
                    durationSeconds = updated.durationSeconds,
                    setType = updated.setType,
                    rpe = updated.rpe,
                )
            }
        }
    }

    fun quickIncrementWeight(set: WorkoutSet, deltaDisplayUnit: Double) {
        val unit = uiState.value.weightUnit
        viewModelScope.launch {
            mutexForSet(set.id).withLock {
                val base = currentSetSnapshot(set)
                val nextKg = if (base.weightUnitMode == WeightUnitMode.STACK_PLATES) {
                    val currentPlates = base.weightKg ?: 0.0
                    (currentPlates + deltaDisplayUnit).coerceAtLeast(0.0)
                } else {
                    val currentDisplay = base.weightKg?.let { WeightUnit.toDisplayWeight(it, unit) } ?: 0.0
                    val nextDisplay = (currentDisplay + deltaDisplayUnit).coerceAtLeast(0.0)
                    WeightUnit.toStorageKg(nextDisplay, unit)
                }
                val updated = base.copy(weightKg = nextKg)
                latestSetEdits[set.id] = updated
                workoutRepository.updateSetValues(
                    setId = base.id,
                    weightKg = updated.weightKg,
                    reps = updated.reps,
                    durationSeconds = updated.durationSeconds,
                    setType = updated.setType,
                    rpe = updated.rpe,
                )
            }
        }
    }

    fun updateExerciseWeightUnitMode(group: WorkoutExerciseGroup, mode: WeightUnitMode) {
        group.sets.forEach { set -> latestSetEdits.remove(set.id) }
        viewModelScope.launch {
            workoutRepository.updateExerciseWeightUnitMode(
                workoutId = workoutId,
                exerciseId = group.exercise.id,
                exerciseOrder = group.exerciseOrder,
                mode = mode,
            )
        }
    }

    fun updateRepsFromInput(set: WorkoutSet, rawInput: String) {
        val parsedReps = rawInput.trim().toIntOrNull()?.coerceIn(0, 500)
        viewModelScope.launch {
            mutexForSet(set.id).withLock {
                val base = currentSetSnapshot(set)
                val updated = base.copy(reps = parsedReps)
                latestSetEdits[set.id] = updated
                workoutRepository.updateSetValues(
                    setId = base.id,
                    weightKg = updated.weightKg,
                    reps = updated.reps,
                    durationSeconds = updated.durationSeconds,
                    setType = updated.setType,
                    rpe = updated.rpe,
                )
            }
        }
    }

    fun updateDurationSeconds(set: WorkoutSet, seconds: Int?) {
        val clamped = seconds?.coerceIn(0, 86400)
        viewModelScope.launch {
            mutexForSet(set.id).withLock {
                val base = currentSetSnapshot(set)
                val updated = base.copy(durationSeconds = clamped)
                latestSetEdits[set.id] = updated
                workoutRepository.updateSetValues(
                    setId = base.id,
                    weightKg = updated.weightKg,
                    reps = updated.reps,
                    durationSeconds = updated.durationSeconds,
                    setType = updated.setType,
                    rpe = updated.rpe,
                )
            }
        }
    }

    fun quickIncrementDuration(set: WorkoutSet, deltaSeconds: Int) {
        viewModelScope.launch {
            mutexForSet(set.id).withLock {
                val base = currentSetSnapshot(set)
                val current = base.durationSeconds ?: 0
                val next = (current + deltaSeconds).coerceIn(0, 86400)
                val updated = base.copy(durationSeconds = next)
                latestSetEdits[set.id] = updated
                workoutRepository.updateSetValues(
                    setId = base.id,
                    weightKg = updated.weightKg,
                    reps = updated.reps,
                    durationSeconds = updated.durationSeconds,
                    setType = updated.setType,
                    rpe = updated.rpe,
                )
            }
        }
    }

    fun updateSetType(set: WorkoutSet, setType: SetType) {
        viewModelScope.launch {
            mutexForSet(set.id).withLock {
                val base = currentSetSnapshot(set)
                val updated = base.copy(setType = setType)
                latestSetEdits[set.id] = updated
                workoutRepository.updateSetValues(
                    setId = base.id,
                    weightKg = updated.weightKg,
                    reps = updated.reps,
                    durationSeconds = updated.durationSeconds,
                    setType = updated.setType,
                    rpe = updated.rpe,
                )
            }
        }
    }

    fun toggleSetCompleted(set: WorkoutSet, exerciseDisplayName: String) {
        viewModelScope.launch {
            mutexForSet(set.id).withLock {
                latestSetEdits.remove(set.id)
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
            mutexForSet(setId).withLock {
                latestSetEdits.remove(setId)
                workoutRepository.removeSet(setId)
            }
            setMutexes.remove(setId)
        }
    }

    fun removeExercise(exerciseId: String, exerciseOrder: Int) {
        uiState.value.session
            ?.exercises
            ?.filter { it.exercise.id == exerciseId && it.exerciseOrder == exerciseOrder }
            ?.flatMap { it.sets }
            ?.forEach { latestSetEdits.remove(it.id) }
        viewModelScope.launch {
            workoutRepository.removeExerciseFromWorkout(workoutId, exerciseId, exerciseOrder)
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
        if (!finishOrDiscardMutex.tryLock()) return
        restTimerController.skip()
        viewModelScope.launch {
            try {
                val summary = workoutRepository.finishWorkout(workoutId)
                if (summary != null) {
                    onFinished(summary)
                }
            } finally {
                finishOrDiscardMutex.unlock()
            }
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        if (!finishOrDiscardMutex.tryLock()) return
        restTimerController.skip()
        viewModelScope.launch {
            try {
                workoutRepository.discardWorkout(workoutId)
                onDiscarded()
            } finally {
                finishOrDiscardMutex.unlock()
            }
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
