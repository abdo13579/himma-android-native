package com.himma.app.features.workout

import com.himma.app.domain.model.Routine
import com.himma.app.features.onboarding.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutLauncherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutRepo: FakeWorkoutRepository
    private lateinit var prefsRepo: FakeUserPreferencesRepository
    private lateinit var timer: FakeRestTimerController

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        workoutRepo = FakeWorkoutRepository()
        prefsRepo = FakeUserPreferencesRepository()
        timer = FakeRestTimerController()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `requestStartFromRoutine surfaces conflict when different workout draft is active`() = runTest {
        val viewModel = WorkoutLauncherViewModel(workoutRepo, prefsRepo, timer)
        val otherRoutine = Routine(
            id = "r_other",
            title = "Pull Day",
            notes = "",
            orderIndex = 0,
            createdAt = 1L,
            updatedAt = 1L,
        )

        var navigatedId: String? = null
        viewModel.requestStartFromRoutine(otherRoutine) { navigatedId = it }
        advanceUntilIdle()

        assertNull(navigatedId)
        val conflict = viewModel.pendingConflict.value
        assertNotNull(conflict)
        assertEquals("w1", conflict?.activeDraft?.id)

        viewModel.resumeConflictingDraft { navigatedId = it }
        assertNull(viewModel.pendingConflict.value)
        assertEquals("w1", navigatedId)
    }

    @Test
    fun `requestStartFromRoutine resumes immediately when same routine is active draft`() = runTest {
        val viewModel = WorkoutLauncherViewModel(workoutRepo, prefsRepo, timer)
        val sameRoutine = Routine(
            id = "r1",
            title = "Push Day",
            notes = "",
            orderIndex = 0,
            createdAt = 1L,
            updatedAt = 1L,
        )

        var navigatedId: String? = null
        viewModel.requestStartFromRoutine(sameRoutine) { navigatedId = it }
        advanceUntilIdle()

        assertNull(viewModel.pendingConflict.value)
        assertEquals("w1", navigatedId)
    }

    @Test
    fun `discardAndStartPending skips timer, clears conflict, and launches new routine workout`() = runTest {
        val viewModel = WorkoutLauncherViewModel(workoutRepo, prefsRepo, timer)
        val otherRoutine = Routine(
            id = "r_new",
            title = "Legs Day",
            notes = "",
            orderIndex = 0,
            createdAt = 1L,
            updatedAt = 1L,
        )

        viewModel.requestStartFreeWorkout("Free") { }
        advanceUntilIdle()
        assertNotNull(viewModel.pendingConflict.value)

        viewModel.dismissConflict()
        assertNull(viewModel.pendingConflict.value)

        viewModel.requestStartFromRoutine(otherRoutine) { }
        advanceUntilIdle()
        assertNotNull(viewModel.pendingConflict.value)

        var startedId: String? = null
        viewModel.discardAndStartPending { startedId = it }
        advanceUntilIdle()

        assertNull(viewModel.pendingConflict.value)
        assertEquals(1, timer.skipCalls)
        assertNotNull(startedId)
    }
}
