package com.himma.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private val _foregroundRejected = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val foregroundRejected: SharedFlow<Unit> = _foregroundRejected.asSharedFlow()

    fun update(state: RestTimerState) {
        _state.value = state
    }

    fun clear() {
        _state.value = RestTimerState()
    }

    fun notifyForegroundRejected() {
        _foregroundRejected.tryEmit(Unit)
    }
}

@Singleton
class AndroidRestTimerController internal constructor(
    private val stateHolder: RestTimerStateHolder,
    dispatcher: CoroutineDispatcher,
    private val intentFactory: (String) -> Intent,
    private val startForegroundServiceAction: (Intent) -> Unit,
    private val startServiceAction: (Intent) -> Unit,
) : RestTimerController {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        stateHolder: RestTimerStateHolder,
    ) : this(
        stateHolder = stateHolder,
        dispatcher = Dispatchers.Main.immediate,
        intentFactory = { actionName ->
            Intent(context, RestTimerForegroundService::class.java).apply {
                action = actionName
            }
        },
        startForegroundServiceAction = { intent -> ContextCompat.startForegroundService(context, intent) },
        startServiceAction = { intent -> context.startService(intent) },
    )

    private val controllerScope = CoroutineScope(SupervisorJob() + dispatcher)
    private var fallbackJob: Job? = null
    private var fallbackActive: Boolean = false

    init {
        controllerScope.launch {
            stateHolder.foregroundRejected.collect {
                val current = stateHolder.state.value
                if (current.isRunning && !fallbackActive) {
                    startInProcessFallback(
                        seconds = current.remainingSeconds.coerceAtLeast(1),
                        exerciseName = current.exerciseName,
                        totalSecondsOverride = current.totalSeconds.coerceAtLeast(1),
                    )
                }
            }
        }
    }

    override val state: StateFlow<RestTimerState> = stateHolder.state

    override fun start(seconds: Int, exerciseName: String) {
        val clamped = seconds.coerceIn(5, 600)
        cancelFallback()
        stateHolder.update(
            RestTimerState(
                isRunning = true,
                exerciseName = exerciseName,
                totalSeconds = clamped,
                remainingSeconds = clamped,
            ),
        )
        val intent = intentFactory(RestTimerForegroundService.ACTION_START).apply {
            putExtra(RestTimerForegroundService.EXTRA_SECONDS, clamped)
            putExtra(RestTimerForegroundService.EXTRA_EXERCISE_NAME, exerciseName)
        }
        val startedService = runCatching { startForegroundServiceAction(intent) }.isSuccess
        if (!startedService) {
            startInProcessFallback(clamped, exerciseName)
        }
    }

    override fun adjust(deltaSeconds: Int) {
        val current = stateHolder.state.value
        if (!current.isRunning) return
        if (fallbackActive) {
            applyInProcessAdjustment(current, deltaSeconds)
            return
        }
        val intent = intentFactory(RestTimerForegroundService.ACTION_ADJUST).apply {
            putExtra(RestTimerForegroundService.EXTRA_DELTA_SECONDS, deltaSeconds)
        }
        // Never fall back to startForegroundService for ACTION_ADJUST:
        // only ACTION_START guarantees startForeground() promotion.
        val adjusted = runCatching { startServiceAction(intent) }.isSuccess
        if (!adjusted) {
            applyInProcessAdjustment(current, deltaSeconds)
        }
    }

    override fun skip() {
        val current = stateHolder.state.value
        val wasFallback = fallbackActive
        cancelFallback()
        stateHolder.clear()
        if (!current.isRunning || wasFallback) return
        val intent = intentFactory(RestTimerForegroundService.ACTION_SKIP)
        runCatching { startServiceAction(intent) }
    }

    private fun applyInProcessAdjustment(current: RestTimerState, deltaSeconds: Int) {
        val nextRemaining = (current.remainingSeconds + deltaSeconds).coerceAtMost(600)
        if (nextRemaining <= 0) {
            skip()
        } else {
            startInProcessFallback(
                seconds = nextRemaining,
                exerciseName = current.exerciseName,
                totalSecondsOverride = maxOf(current.totalSeconds, nextRemaining),
            )
        }
    }

    private fun startInProcessFallback(
        seconds: Int,
        exerciseName: String,
        totalSecondsOverride: Int = seconds,
    ) {
        fallbackJob?.cancel()
        fallbackActive = true
        stateHolder.update(
            RestTimerState(
                isRunning = true,
                exerciseName = exerciseName,
                totalSeconds = totalSecondsOverride,
                remainingSeconds = seconds,
            ),
        )
        fallbackJob = controllerScope.launch {
            var remaining = seconds
            while (isActive && remaining > 0) {
                delay(1_000L)
                remaining -= 1
                if (remaining <= 0) {
                    fallbackActive = false
                    stateHolder.clear()
                    break
                }
                stateHolder.update(
                    RestTimerState(
                        isRunning = true,
                        exerciseName = exerciseName,
                        totalSeconds = totalSecondsOverride,
                        remainingSeconds = remaining,
                    ),
                )
            }
        }
    }

    private fun cancelFallback() {
        fallbackJob?.cancel()
        fallbackJob = null
        fallbackActive = false
    }
}
