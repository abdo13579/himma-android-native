package com.himma.app.features.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.WorkoutRepository
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.Routine
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WorkoutSession
import com.himma.app.service.RestTimerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PendingWorkoutStart {
    data class FromRoutine(val routine: Routine) : PendingWorkoutStart
    data class Free(val defaultTitle: String) : PendingWorkoutStart
}

data class ActiveDraftConflict(
    val activeDraft: WorkoutSession,
    val pendingStart: PendingWorkoutStart,
)

@HiltViewModel
class WorkoutLauncherViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val restTimerController: RestTimerController,
) : ViewModel() {

    val activeDraft: StateFlow<WorkoutSession?> = workoutRepository.observeActiveDraft()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val weightUnit: StateFlow<WeightUnit> = preferencesRepository.preferences
        .map { it.weightUnit }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeightUnit.KG,
        )

    private val _pendingConflict = MutableStateFlow<ActiveDraftConflict?>(null)
    val pendingConflict: StateFlow<ActiveDraftConflict?> = _pendingConflict.asStateFlow()

    fun requestStartFromRoutine(
        routine: Routine,
        onNavigateToWorkout: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val currentDraft = workoutRepository.getActiveDraftOnce()
            if (currentDraft != null) {
                if (currentDraft.routineId == routine.id) {
                    onNavigateToWorkout(currentDraft.id)
                } else {
                    _pendingConflict.value = ActiveDraftConflict(
                        activeDraft = currentDraft,
                        pendingStart = PendingWorkoutStart.FromRoutine(routine),
                    )
                }
            } else {
                val defaultRest = preferencesRepository.preferences.first().defaultRestSeconds
                val newId = workoutRepository.startWorkoutFromRoutine(
                    routineId = routine.id,
                    userDefaultRestSeconds = defaultRest,
                )
                onNavigateToWorkout(newId)
            }
        }
    }

    fun requestStartFreeWorkout(
        defaultTitle: String,
        onNavigateToWorkout: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val currentDraft = workoutRepository.getActiveDraftOnce()
            if (currentDraft != null) {
                _pendingConflict.value = ActiveDraftConflict(
                    activeDraft = currentDraft,
                    pendingStart = PendingWorkoutStart.Free(defaultTitle),
                )
            } else {
                val newId = workoutRepository.startFreeWorkout(defaultTitle)
                onNavigateToWorkout(newId)
            }
        }
    }

    fun dismissConflict() {
        _pendingConflict.value = null
    }

    fun resumeConflictingDraft(onNavigateToWorkout: (String) -> Unit) {
        val conflict = _pendingConflict.value ?: return
        _pendingConflict.value = null
        onNavigateToWorkout(conflict.activeDraft.id)
    }

    fun discardAndStartPending(onNavigateToWorkout: (String) -> Unit) {
        val conflict = _pendingConflict.value ?: return
        _pendingConflict.value = null
        restTimerController.skip()
        viewModelScope.launch {
            workoutRepository.discardWorkout(conflict.activeDraft.id)
            val newId = when (val pending = conflict.pendingStart) {
                is PendingWorkoutStart.FromRoutine -> {
                    val defaultRest = preferencesRepository.preferences.first().defaultRestSeconds
                    workoutRepository.startWorkoutFromRoutine(
                        routineId = pending.routine.id,
                        userDefaultRestSeconds = defaultRest,
                    )
                }
                is PendingWorkoutStart.Free -> {
                    workoutRepository.startFreeWorkout(pending.defaultTitle)
                }
            }
            onNavigateToWorkout(newId)
        }
    }
}
