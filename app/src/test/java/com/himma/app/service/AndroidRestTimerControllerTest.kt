package com.himma.app.service

import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidRestTimerControllerTest {

    @Test
    fun `start falls back to in-process countdown when foreground service throws SecurityException`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val stateHolder = RestTimerStateHolder()
        val controller = AndroidRestTimerController(
            stateHolder = stateHolder,
            dispatcher = dispatcher,
            intentFactory = { Mockito.mock(Intent::class.java) },
            startForegroundServiceAction = { throw SecurityException("FGS blocked") },
            startServiceAction = { throw IllegalStateException("Service blocked") },
        )

        controller.start(seconds = 5, exerciseName = "Bench Press")
        runCurrent()

        assertTrue(controller.state.value.isRunning)
        assertEquals(5, controller.state.value.remainingSeconds)
        assertEquals("Bench Press", controller.state.value.exerciseName)

        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(3, controller.state.value.remainingSeconds)

        controller.adjust(deltaSeconds = 10)
        runCurrent()
        assertEquals(13, controller.state.value.remainingSeconds)
        assertEquals(13, controller.state.value.totalSeconds)

        controller.skip()
        runCurrent()
        assertFalse(controller.state.value.isRunning)
        assertEquals(0, controller.state.value.remainingSeconds)
    }

    @Test
    fun `fallback countdown clears state cleanly when timer completes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val stateHolder = RestTimerStateHolder()
        val controller = AndroidRestTimerController(
            stateHolder = stateHolder,
            dispatcher = dispatcher,
            intentFactory = { Mockito.mock(Intent::class.java) },
            startForegroundServiceAction = { throw IllegalStateException("Not allowed") },
            startServiceAction = { },
        )

        controller.start(seconds = 5, exerciseName = "Pull Up")
        runCurrent()
        assertTrue(controller.state.value.isRunning)

        advanceTimeBy(5_000L)
        runCurrent()

        assertFalse(controller.state.value.isRunning)
        assertEquals(0, controller.state.value.remainingSeconds)
    }

    @Test
    fun `foregroundRejected switches to in-process countdown and adjust never starts foreground service`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val stateHolder = RestTimerStateHolder()
        var foregroundCalls = 0
        val controller = AndroidRestTimerController(
            stateHolder = stateHolder,
            dispatcher = dispatcher,
            intentFactory = { Mockito.mock(Intent::class.java) },
            startForegroundServiceAction = { foregroundCalls++ },
            startServiceAction = { throw IllegalStateException("Service background blocked") },
        )

        controller.start(seconds = 30, exerciseName = "Squat")
        runCurrent()
        assertEquals(1, foregroundCalls)

        // Simulate service rejecting FGS start
        stateHolder.update(
            RestTimerState(
                isRunning = true,
                totalSeconds = 30,
                remainingSeconds = 30,
                exerciseName = "Squat",
            ),
        )
        stateHolder.notifyForegroundRejected()
        runCurrent()

        assertTrue(controller.state.value.isRunning)
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(28, controller.state.value.remainingSeconds)

        // Adjusting must never call startForegroundServiceAction again
        controller.adjust(deltaSeconds = 15)
        runCurrent()
        assertEquals(1, foregroundCalls)
        assertEquals(43, controller.state.value.remainingSeconds)

        controller.skip()
        runCurrent()
        assertFalse(controller.state.value.isRunning)
    }
}
