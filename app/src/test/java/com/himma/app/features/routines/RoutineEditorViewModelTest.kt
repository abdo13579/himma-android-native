package com.himma.app.features.routines

import androidx.lifecycle.SavedStateHandle
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.features.exercises.FakeExerciseRepository
import com.himma.app.features.onboarding.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRoutineRepo: FakeRoutineRepository
    private lateinit var fakeExerciseRepo: FakeExerciseRepository
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRoutineRepo = FakeRoutineRepository()
        fakeExerciseRepo = FakeExerciseRepository()
        fakePrefsRepo = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `editor validates title, toggles exercises from picker, reorders, adjusts targets, and saves`() = runTest {
        val viewModel = RoutineEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            routineRepository = fakeRoutineRepo,
            exerciseRepository = fakeExerciseRepo,
            userPreferencesRepository = fakePrefsRepo,
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertTrue(fakeExerciseRepo.seededCalled)
        assertEquals(3, viewModel.uiState.value.pickerExercises.size)

        // Saving blank title sets validation error
        var saved = false
        viewModel.saveRoutine { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        assertTrue(viewModel.uiState.value.form.titleError)

        // Filter picker & add 2 exercises
        viewModel.selectPickerMuscle(MuscleGroup.CHEST)
        advanceUntilIdle()
        val bench = viewModel.uiState.value.pickerExercises.single()
        viewModel.toggleExerciseFromPicker(bench)

        viewModel.clearPickerFilters()
        viewModel.updatePickerQuery("سكوات")
        advanceUntilIdle()
        val squat = viewModel.uiState.value.pickerExercises.single()
        viewModel.toggleExerciseFromPicker(squat)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.form.exercises.size)

        // Reorder bench and squat
        viewModel.moveExercise(0, 1)
        advanceUntilIdle()
        assertEquals("squat", viewModel.uiState.value.form.exercises[0].exercise.id)
        assertEquals("bench", viewModel.uiState.value.form.exercises[1].exercise.id)

        // Adjust sets, reps range, and rest on first item
        val firstItemId = viewModel.uiState.value.form.exercises[0].itemId
        viewModel.adjustSets(firstItemId, 1) // 3 -> 4
        viewModel.adjustRepsMin(firstItemId, -2) // 8 -> 6
        viewModel.adjustRepsMax(firstItemId, -4) // 12 -> 8
        viewModel.adjustRestSeconds(firstItemId, 30) // 150 -> 180

        viewModel.updateTitle("Push & Legs")
        viewModel.updateNotes("Heavy day")
        viewModel.saveRoutine { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        val persisted = fakeRoutineRepo.getRoutineById("routine_1")!!
        assertEquals("Push & Legs", persisted.title)
        assertEquals("Heavy day", persisted.notes)
        assertEquals(2, persisted.exercises.size)
        assertEquals("squat", persisted.exercises[0].exercise.id)
        assertEquals(4, persisted.exercises[0].targetSets)
        assertEquals(6, persisted.exercises[0].targetRepsMin)
        assertEquals(8, persisted.exercises[0].targetRepsMax)
        assertEquals(180, persisted.exercises[0].targetRestSeconds)

        collector.cancel()
    }
}
