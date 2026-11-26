package com.himma.app.features.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.BuildConfig
import com.himma.app.core.database.AnalyticsRepository
import com.himma.app.core.database.RoutineRepository
import com.himma.app.core.database.UserProfileRepository
import com.himma.app.core.database.WeeklyScheduleRepository
import com.himma.app.core.database.WorkoutRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.core.updater.AppUpdateInfo
import com.himma.app.core.updater.AppUpdateRepository
import com.himma.app.domain.model.Routine
import com.himma.app.domain.model.UserProfile
import com.himma.app.domain.model.WeeklyScheduleEntry
import com.himma.app.domain.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val preferences: UserPreferences = UserPreferences(),
    val profile: UserProfile? = null,
    val activeDraft: WorkoutSession? = null,
    val routines: List<Routine> = emptyList(),
    val weeklySchedule: Map<DayOfWeek, WeeklyScheduleEntry> = emptyMap(),
    val todayDay: DayOfWeek = LocalDate.now().dayOfWeek,
    val selectedDay: DayOfWeek = LocalDate.now().dayOfWeek,
    val completedTodayRoutineIds: Set<String> = emptySet(),
    val availableUpdate: AppUpdateInfo? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
    profileRepository: UserProfileRepository,
    workoutRepository: WorkoutRepository,
    routineRepository: RoutineRepository,
    private val weeklyScheduleRepository: WeeklyScheduleRepository,
    analyticsRepository: AnalyticsRepository,
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {

    private val availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    private val currentDate = MutableStateFlow(LocalDate.now())
    private val selectedDay = MutableStateFlow(currentDate.value.dayOfWeek)
    private var hasCheckedUpdates = false

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            if (BuildConfig.SUPPORTS_IN_APP_UPDATES && prefs.autoCheckUpdates && !hasCheckedUpdates) {
                hasCheckedUpdates = true
                appUpdateRepository.checkForUpdate().onSuccess { info ->
                    if (info.isUpdateAvailable) {
                        availableUpdate.value = info
                    }
                }
            }
        }
    }

    private val baseData = combine(
        preferencesRepository.preferences,
        profileRepository.profile,
        workoutRepository.observeActiveDraft(),
        routineRepository.observeActiveRoutines(),
        availableUpdate,
    ) { prefs, profile, activeDraft, routines, update ->
        BaseTodayData(
            preferences = prefs,
            profile = profile,
            activeDraft = activeDraft,
            routines = routines,
            availableUpdate = update,
        )
    }

    private val scheduleData = combine(
        weeklyScheduleRepository.observeWeeklySchedule(),
        analyticsRepository.observeWorkoutHistory(),
        selectedDay,
        currentDate,
    ) { schedule, history, selected, today ->
        val zoneId = ZoneId.systemDefault()
        val completedToday = history
            .filter { summary ->
                summary.routineId != null &&
                    Instant.ofEpochMilli(summary.startTime).atZone(zoneId).toLocalDate() == today
            }
            .mapNotNull { it.routineId }
            .toSet()
        ScheduleTodayData(
            weeklySchedule = schedule,
            todayDay = today.dayOfWeek,
            selectedDay = selected,
            completedTodayRoutineIds = completedToday,
        )
    }

    val uiState: StateFlow<TodayUiState> = combine(baseData, scheduleData) { base, sched ->
        TodayUiState(
            preferences = base.preferences,
            profile = base.profile,
            activeDraft = base.activeDraft,
            routines = base.routines,
            weeklySchedule = sched.weeklySchedule,
            todayDay = sched.todayDay,
            selectedDay = sched.selectedDay,
            completedTodayRoutineIds = sched.completedTodayRoutineIds,
            availableUpdate = base.availableUpdate,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    fun refreshDateIfNeeded() {
        val now = LocalDate.now()
        val previous = currentDate.value
        if (now != previous) {
            currentDate.value = now
            if (selectedDay.value == previous.dayOfWeek) {
                selectedDay.value = now.dayOfWeek
            }
        }
    }

    fun selectDay(dayOfWeek: DayOfWeek) {
        selectedDay.value = dayOfWeek
    }

    fun assignRoutineToDay(dayOfWeek: DayOfWeek, routineId: String) {
        viewModelScope.launch {
            weeklyScheduleRepository.setDayRoutine(dayOfWeek, routineId)
        }
    }

    fun setRestDay(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            weeklyScheduleRepository.setDayRest(dayOfWeek)
        }
    }

    fun clearDaySchedule(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            weeklyScheduleRepository.clearDay(dayOfWeek)
        }
    }

    fun dismissUpdateBanner() {
        availableUpdate.value = null
    }

    private data class BaseTodayData(
        val preferences: UserPreferences,
        val profile: UserProfile?,
        val activeDraft: WorkoutSession?,
        val routines: List<Routine>,
        val availableUpdate: AppUpdateInfo?,
    )

    private data class ScheduleTodayData(
        val weeklySchedule: Map<DayOfWeek, WeeklyScheduleEntry>,
        val todayDay: DayOfWeek,
        val selectedDay: DayOfWeek,
        val completedTodayRoutineIds: Set<String>,
    )
}
