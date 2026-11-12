package com.himma.app.features.workout

import androidx.lifecycle.SavedStateHandle
import com.himma.app.core.database.SetCompletionResult
import com.himma.app.core.database.WorkoutRepository
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
import com.himma.app.features.exercises.FakeExerciseRepository
import com.himma.app.features.onboarding.FakeUserPreferencesRepository
import com.himma.app.service.RestTimerController
import com.himma.app.service.RestTimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeWorkoutRepo: FakeWorkoutRepository
    private lateinit var fakeExerciseRepo: FakeExerciseRepository
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var fakeTimer: FakeRestTimerController

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeWorkoutRepo = FakeWorkoutRepository()
        fakeExerciseRepo = FakeExerciseRepository()
        fakePrefsRepo = FakeUserPreferencesRepository()
        fakeTimer = FakeRestTimerController()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completing empty set auto-fills previous weight and reps, starts rest timer, and computes finished volume`() = runTest {
        val viewModel = ActiveWorkoutViewModel(
            savedStateHandle = SavedStateHandle(mapOf("workoutId" to "w1")),
            workoutRepository = fakeWorkoutRepo,
            exerciseRepository = fakeExerciseRepo,
            preferencesRepository = fakePrefsRepo,
            restTimerController = fakeTimer,
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initialSet = viewModel.uiState.value.session!!.exercises.first().sets.first()
        assertFalse(initialSet.isCompleted)

        // Toggling complete on empty set auto-fills previous (80 kg x 8) and starts 120s timer
        viewModel.toggleSetCompleted(initialSet, "بنش برس بالبار")
        advanceUntilIdle()

        val completedSet = viewModel.uiState.value.session!!.exercises.first().sets.first()
        assertTrue(completedSet.isCompleted)
        assertEquals(80.0, completedSet.weightKg!!, 0.01)
        assertEquals(8, completedSet.reps)
        assertTrue(viewModel.uiState.value.restTimer.isRunning)
        assertEquals(120, viewModel.uiState.value.restTimer.remainingSeconds)

        // Quick increment +2.5 kg moves 80.0 -> 82.5
        viewModel.quickIncrementWeight(completedSet, 2.5)
        advanceUntilIdle()
        val bumpedSet = viewModel.uiState.value.session!!.exercises.first().sets.first()
        assertEquals(82.5, bumpedSet.weightKg!!, 0.01)

        // Unchecking stops rest timer
        viewModel.toggleSetCompleted(bumpedSet, "بنش برس بالبار")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.restTimer.isRunning)

        // Re-check and finish workout
        val uncheckedSet = viewModel.uiState.value.session!!.exercises.first().sets.first()
        viewModel.toggleSetCompleted(uncheckedSet, "بنش برس بالبار")
        advanceUntilIdle()

        var summary: FinishedWorkoutSummary? = null
        viewModel.finishWorkout { summary = it }
        advanceUntilIdle()

        assertNotNull(summary)
        assertEquals(1, summary!!.completedSets)
        assertEquals(82.5 * 8, summary!!.totalVolumeKg, 0.1)
        assertFalse(viewModel.uiState.value.restTimer.isRunning)

        collector.cancel()
    }
}

class FakeRestTimerController : RestTimerController {
    private val _state = MutableStateFlow(RestTimerState())
    override val state: StateFlow<RestTimerState> = _state

    override fun start(seconds: Int, exerciseName: String) {
        _state.value = RestTimerState(
            isRunning = true,
            exerciseName = exerciseName,
            totalSeconds = seconds,
            remainingSeconds = seconds,
        )
    }

    override fun adjust(deltaSeconds: Int) {
        val current = _state.value
        if (!current.isRunning) return
        val next = (current.remainingSeconds + deltaSeconds).coerceAtLeast(0)
        if (next == 0) {
            skip()
        } else {
            _state.value = current.copy(remainingSeconds = next, totalSeconds = maxOf(current.totalSeconds, next))
        }
    }

    override fun skip() {
        _state.value = RestTimerState()
    }
}

class FakeWorkoutRepository : WorkoutRepository {
    private val bench = Exercise(
        id = "bench",
        nameEn = "Barbell Bench Press",
        nameAr = "بنش برس بالبار",
        primaryMuscle = MuscleGroup.CHEST,
        secondaryMuscles = listOf(MuscleGroup.ARMS),
        equipment = EquipmentType.BARBELL,
        instructionsEn = "",
        instructionsAr = "",
        localMediaKey = "bench",
        defaultRestSeconds = 120,
        isCustom = false,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private val sessionFlow = MutableStateFlow<WorkoutSession?>(
        WorkoutSession(
            id = "w1",
            routineId = "r1",
            title = "Push Day",
            startTime = System.currentTimeMillis() - 60_000L,
            endTime = null,
            durationSeconds = 0,
            totalVolumeKg = 0.0,
            notes = "",
            isCompleted = false,
            exercises = listOf(
                WorkoutExerciseGroup(
                    exercise = bench,
                    exerciseOrder = 0,
                    restSeconds = 120,
                    sets = listOf(
                        WorkoutSet(
                            id = "s1",
                            workoutId = "w1",
                            exerciseId = "bench",
                            exerciseOrder = 0,
                            setIndex = 0,
                            restSeconds = 120,
                            weightKg = null,
                            reps = null,
                        ),
                    ),
                ),
            ),
        ),
    )

    override fun observeActiveDraft(): Flow<WorkoutSession?> = sessionFlow.map {
        if (it?.isCompleted == false) it else null
    }

    override suspend fun getActiveDraftOnce(): WorkoutSession? =
        sessionFlow.value?.takeIf { !it.isCompleted }

    override fun observeWorkoutById(workoutId: String): Flow<WorkoutSession?> = sessionFlow.map {
        if (it?.id == workoutId) it else null
    }

    override suspend fun startWorkoutFromRoutine(routineId: String, userDefaultRestSeconds: Int): String = "w1"

    override suspend fun startFreeWorkout(title: String): String = "w1"

    override suspend fun updateWorkoutNotes(workoutId: String, notes: String) {
        sessionFlow.value = sessionFlow.value?.copy(notes = notes)
    }

    override suspend fun addExerciseToWorkout(workoutId: String, exercise: Exercise, targetSets: Int, restSeconds: Int) {}

    override suspend fun addSetToExercise(workoutId: String, exerciseId: String, exerciseOrder: Int, restSeconds: Int) {}

    override suspend fun removeSet(setId: String) {}

    override suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {}

    override suspend fun updateSetValues(setId: String, weightKg: Double?, reps: Int?, setType: SetType, rpe: Double?) {
        sessionFlow.value = sessionFlow.value?.let { current ->
            current.copy(
                exercises = current.exercises.map { group ->
                    group.copy(
                        sets = group.sets.map { s ->
                            if (s.id == setId) s.copy(weightKg = weightKg, reps = reps, setType = setType, rpe = rpe) else s
                        },
                    )
                },
            )
        }
    }

    override suspend fun toggleSetCompleted(setId: String): SetCompletionResult {
        var result = SetCompletionResult(false, false, 0)
        sessionFlow.value = sessionFlow.value?.let { current ->
            current.copy(
                exercises = current.exercises.map { group ->
                    group.copy(
                        sets = group.sets.map { s ->
                            if (s.id == setId) {
                                val nextDone = !s.isCompleted
                                result = SetCompletionResult(nextDone, nextDone, s.restSeconds)
                                s.copy(
                                    isCompleted = nextDone,
                                    weightKg = if (nextDone && s.weightKg == null) 80.0 else s.weightKg,
                                    reps = if (nextDone && s.reps == null) 8 else s.reps,
                                )
                            } else {
                                s
                            }
                        },
                    )
                },
            )
        }
        return result
    }

    override suspend fun getPreviousHintsForExercises(exerciseIds: List<String>): Map<Pair<String, Int>, PreviousSetHint> {
        return mapOf(("bench" to 0) to PreviousSetHint("bench", 0, 80.0, 8))
    }

    override suspend fun finishWorkout(workoutId: String): FinishedWorkoutSummary? {
        val current = sessionFlow.value ?: return null
        val allSets = current.exercises.flatMap { it.sets }
        val volume = WorkoutMath.calculateTotalVolumeKg(allSets)
        sessionFlow.value = current.copy(isCompleted = true, totalVolumeKg = volume)
        return FinishedWorkoutSummary(
            workoutId = current.id,
            title = current.title,
            durationSeconds = 60,
            totalVolumeKg = volume,
            completedSets = current.completedSetsCount,
        )
    }

    override suspend fun discardWorkout(workoutId: String) {
        sessionFlow.value = null
    }
}
