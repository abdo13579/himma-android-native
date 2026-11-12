package com.himma.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RestTimerState(
    val isRunning: Boolean = false,
    val exerciseName: String = "",
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

interface RestTimerController {
    val state: StateFlow<RestTimerState>

    fun start(seconds: Int, exerciseName: String)

    fun adjust(deltaSeconds: Int)

    fun skip()
}

@Singleton
class RestTimerStateHolder @Inject constructor() {
    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    fun update(state: RestTimerState) {
        _state.value = state
    }

    fun clear() {
        _state.value = RestTimerState()
    }
}

@Singleton
class AndroidRestTimerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateHolder: RestTimerStateHolder,
) : RestTimerController {

    override val state: StateFlow<RestTimerState> = stateHolder.state

    override fun start(seconds: Int, exerciseName: String) {
        val clamped = seconds.coerceIn(5, 600)
        stateHolder.update(
            RestTimerState(
                isRunning = true,
                exerciseName = exerciseName,
                totalSeconds = clamped,
                remainingSeconds = clamped,
            ),
        )
        val intent = Intent(context, RestTimerForegroundService::class.java).apply {
            action = RestTimerForegroundService.ACTION_START
            putExtra(RestTimerForegroundService.EXTRA_SECONDS, clamped)
            putExtra(RestTimerForegroundService.EXTRA_EXERCISE_NAME, exerciseName)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun adjust(deltaSeconds: Int) {
        if (!stateHolder.state.value.isRunning) return
        val intent = Intent(context, RestTimerForegroundService::class.java).apply {
            action = RestTimerForegroundService.ACTION_ADJUST
            putExtra(RestTimerForegroundService.EXTRA_DELTA_SECONDS, deltaSeconds)
        }
        try {
            context.startService(intent)
        } catch (_: IllegalStateException) {
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun skip() {
        val current = stateHolder.state.value
        stateHolder.clear()
        if (!current.isRunning) return
        val intent = Intent(context, RestTimerForegroundService::class.java).apply {
            action = RestTimerForegroundService.ACTION_SKIP
        }
        try {
            context.startService(intent)
        } catch (_: IllegalStateException) {
            // Safe fallback if service already stopped.
        }
    }
}
