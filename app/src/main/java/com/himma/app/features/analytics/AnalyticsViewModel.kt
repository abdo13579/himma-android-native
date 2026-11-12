package com.himma.app.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.AnalyticsRepository
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.BodyWeightEntry
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleVolumeDistribution
import com.himma.app.domain.model.PersonalRecord
import com.himma.app.domain.model.ProgressionPoint
import com.himma.app.domain.model.ProgressiveInsight
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WorkoutHistorySummary
import com.himma.app.domain.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnalyticsTab {
    HISTORY,
    PR_LEDGER,
    PROGRESSION,
    BODY_WEIGHT,
}

enum class ProgressionChartMode {
    ESTIMATED_1RM,
    MAX_WEIGHT,
}

data class AnalyticsUiState(
    val selectedTab: AnalyticsTab = AnalyticsTab.HISTORY,
    val preferences: UserPreferences = UserPreferences(),
    val workoutHistory: List<WorkoutHistorySummary> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val muscleDistributions: List<MuscleVolumeDistribution> = emptyList(),
    val insights: List<ProgressiveInsight> = emptyList(),
    val allExercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val progressionMode: ProgressionChartMode = ProgressionChartMode.ESTIMATED_1RM,
    val progressionPoints: List<ProgressionPoint> = emptyList(),
    val bodyWeightDays: Int = 30,
    val bodyWeightEntries: List<BodyWeightEntry> = emptyList(),
    val bodyWeightInput: String = "",
    val bodyWeightError: Boolean = false,
    val selectedWorkoutDetail: WorkoutSession? = null,
)

sealed interface AnalyticsEvent {
    data object BodyWeightLogged : AnalyticsEvent
    data object BodyWeightDeleted : AnalyticsEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    exerciseRepository: ExerciseRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private data class SelectionState(
        val tab: AnalyticsTab = AnalyticsTab.HISTORY,
        val selectedExerciseId: String? = null,
        val progressionMode: ProgressionChartMode = ProgressionChartMode.ESTIMATED_1RM,
        val bodyWeightDays: Int = 30,
        val selectedWorkoutId: String? = null,
        val bodyWeightInput: String = "",
        val bodyWeightError: Boolean = false,
    )

    private val selection = MutableStateFlow(SelectionState())
    private val _events = MutableSharedFlow<AnalyticsEvent>()
    val events: SharedFlow<AnalyticsEvent> = _events.asSharedFlow()

    private val allExercisesFlow = exerciseRepository.observeExercises(
        query = "",
        muscleFilter = null,
        equipmentFilter = null,
    )
    private val personalRecordsFlow = analyticsRepository.observeAllPersonalRecords()
    private val historyFlow = analyticsRepository.observeWorkoutHistory()
    private val muscleDistFlow = analyticsRepository.observeMuscleVolumeDistribution(days = 7)
    private val insightsFlow = analyticsRepository.observeInsights()

    private val activeExerciseIdFlow: Flow<String?> = combine(
        selection,
        personalRecordsFlow,
        allExercisesFlow,
    ) { sel, prs, exercises ->
        sel.selectedExerciseId
            ?: prs.firstOrNull()?.exerciseId
            ?: exercises.firstOrNull()?.id
    }

    private val progressionPointsFlow: Flow<List<ProgressionPoint>> = combine(
        activeExerciseIdFlow,
        selection,
    ) { exId, sel ->
        exId to sel.progressionMode
    }.flatMapLatest { (exId, mode) ->
        if (exId == null) {
            flowOf(emptyList())
        } else {
            when (mode) {
                ProgressionChartMode.ESTIMATED_1RM -> analyticsRepository.observeExercise1RmProgression(exId)
                ProgressionChartMode.MAX_WEIGHT -> analyticsRepository.observeExerciseWeightProgression(exId)
            }
        }
    }

    private val bodyWeightTrendFlow: Flow<List<BodyWeightEntry>> = selection
        .flatMapLatest { sel ->
            analyticsRepository.observeBodyWeightTrend(sel.bodyWeightDays)
        }

    private val selectedWorkoutDetailFlow: Flow<WorkoutSession?> = selection
        .flatMapLatest { sel ->
            val wId = sel.selectedWorkoutId
            if (wId == null) flowOf(null) else analyticsRepository.observeWorkoutDetail(wId)
        }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        selection,
        preferencesRepository.preferences,
        historyFlow,
        personalRecordsFlow,
        muscleDistFlow,
        insightsFlow,
        allExercisesFlow,
        progressionPointsFlow,
        bodyWeightTrendFlow,
        selectedWorkoutDetailFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val sel = values[0] as SelectionState
        val prefs = values[1] as UserPreferences
        val history = values[2] as List<WorkoutHistorySummary>
        val prs = values[3] as List<PersonalRecord>
        val dist = values[4] as List<MuscleVolumeDistribution>
        val insights = values[5] as List<ProgressiveInsight>
        val exercises = values[6] as List<Exercise>
        val progression = values[7] as List<ProgressionPoint>
        val bodyWeights = values[8] as List<BodyWeightEntry>
        val detail = values[9] as WorkoutSession?

        val resolvedExId = sel.selectedExerciseId
            ?: prs.firstOrNull()?.exerciseId
            ?: exercises.firstOrNull()?.id
        val selectedEx = exercises.firstOrNull { it.id == resolvedExId }

        AnalyticsUiState(
            selectedTab = sel.tab,
            preferences = prefs,
            workoutHistory = history,
            personalRecords = prs,
            muscleDistributions = dist,
            insights = insights,
            allExercises = exercises,
            selectedExercise = selectedEx,
            progressionMode = sel.progressionMode,
            progressionPoints = progression,
            bodyWeightDays = sel.bodyWeightDays,
            bodyWeightEntries = bodyWeights,
            bodyWeightInput = sel.bodyWeightInput,
            bodyWeightError = sel.bodyWeightError,
            selectedWorkoutDetail = detail,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState(),
    )

    fun selectTab(tab: AnalyticsTab) {
        selection.update { it.copy(tab = tab) }
    }

    fun selectExerciseForProgression(exerciseId: String) {
        selection.update {
            it.copy(
                selectedExerciseId = exerciseId,
                tab = AnalyticsTab.PROGRESSION,
            )
        }
    }

    fun setProgressionMode(mode: ProgressionChartMode) {
        selection.update { it.copy(progressionMode = mode) }
    }

    fun setBodyWeightDays(days: Int) {
        selection.update { it.copy(bodyWeightDays = days) }
    }

    fun updateBodyWeightInput(input: String) {
        selection.update { it.copy(bodyWeightInput = input, bodyWeightError = false) }
    }

    fun logBodyWeight() {
        val input = selection.value.bodyWeightInput
        val parsed = WeightUnit.parseOptionalPositiveWeight(input)
        val value = parsed.getOrNull()
        if (parsed.isFailure || value == null || value <= 0.0) {
            selection.update { it.copy(bodyWeightError = true) }
            return
        }

        viewModelScope.launch {
            val unit = preferencesRepository.preferences.first().weightUnit
            val weightKg = WeightUnit.toStorageKg(value, unit)
            analyticsRepository.logBodyWeight(weightKg)
            selection.update { it.copy(bodyWeightInput = "", bodyWeightError = false) }
            _events.emit(AnalyticsEvent.BodyWeightLogged)
        }
    }

    fun deleteBodyWeight(id: String) {
        viewModelScope.launch {
            analyticsRepository.deleteBodyWeight(id)
            _events.emit(AnalyticsEvent.BodyWeightDeleted)
        }
    }

    fun openWorkoutDetail(workoutId: String) {
        selection.update { it.copy(selectedWorkoutId = workoutId) }
    }

    fun closeWorkoutDetail() {
        selection.update { it.copy(selectedWorkoutId = null) }
    }
}
