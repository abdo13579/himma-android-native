package com.himma.app.features.shell

import com.himma.app.core.database.AnalyticsRepository
import com.himma.app.core.database.WeeklyScheduleRepository
import com.himma.app.core.updater.AppUpdateInfo
import com.himma.app.core.updater.AppUpdateRepository
import com.himma.app.domain.model.BodyWeightEntry
import com.himma.app.domain.model.MuscleVolumeDistribution
import com.himma.app.domain.model.PersonalRecord
import com.himma.app.domain.model.ProgressionPoint
import com.himma.app.domain.model.ProgressiveInsight
import com.himma.app.domain.model.ScheduleDayStatus
import com.himma.app.domain.model.WeekScheduleHelper
import com.himma.app.domain.model.WeeklyScheduleEntry
import com.himma.app.domain.model.WorkoutHistorySummary
import com.himma.app.domain.model.WorkoutSession
import com.himma.app.features.onboarding.FakeUserPreferencesRepository
import com.himma.app.features.onboarding.FakeUserProfileRepository
import com.himma.app.features.routines.FakeRoutineRepository
import com.himma.app.features.workout.FakeWorkoutRepository
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var fakeProfileRepo: FakeUserProfileRepository
    private lateinit var fakeWorkoutRepo: FakeWorkoutRepository
    private lateinit var fakeRoutineRepo: FakeRoutineRepository
    private lateinit var fakeScheduleRepo: FakeWeeklyScheduleRepository
    private lateinit var fakeAnalyticsRepo: FakeTodayAnalyticsRepository
    private lateinit var fakeUpdateRepo: FakeTodayUpdateRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakePrefsRepo = FakeUserPreferencesRepository()
        fakeProfileRepo = FakeUserProfileRepository()
        fakeWorkoutRepo = FakeWorkoutRepository()
        fakeRoutineRepo = FakeRoutineRepository()
        fakeScheduleRepo = FakeWeeklyScheduleRepository()
        fakeAnalyticsRepo = FakeTodayAnalyticsRepository()
        fakeUpdateRepo = FakeTodayUpdateRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `assigning routine, rest day, clearing day, and selecting day update uiState`() = runTest {
        val pullId = fakeRoutineRepo.saveRoutine(
            existingId = null,
            title = "تمرينة سحب",
            notes = "",
            exercises = emptyList(),
        )
        val viewModel = TodayViewModel(
            preferencesRepository = fakePrefsRepo,
            profileRepository = fakeProfileRepo,
            workoutRepository = fakeWorkoutRepo,
            routineRepository = fakeRoutineRepo,
            weeklyScheduleRepository = fakeScheduleRepo,
            analyticsRepository = fakeAnalyticsRepo,
            appUpdateRepository = fakeUpdateRepo,
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(LocalDate.now().dayOfWeek, viewModel.uiState.value.selectedDay)
        assertEquals(DayOfWeek.SATURDAY, viewModel.uiState.value.preferences.weekStartDay)

        viewModel.selectDay(DayOfWeek.TUESDAY)
        viewModel.assignRoutineToDay(DayOfWeek.TUESDAY, pullId)
        viewModel.setRestDay(DayOfWeek.WEDNESDAY)
        advanceUntilIdle()

        assertEquals(DayOfWeek.TUESDAY, viewModel.uiState.value.selectedDay)
        val tuesdayEntry = viewModel.uiState.value.weeklySchedule[DayOfWeek.TUESDAY]
        assertEquals(ScheduleDayStatus.ROUTINE, tuesdayEntry?.status)
        assertEquals(pullId, tuesdayEntry?.routineId)

        val wednesdayEntry = viewModel.uiState.value.weeklySchedule[DayOfWeek.WEDNESDAY]
        assertEquals(ScheduleDayStatus.REST, wednesdayEntry?.status)
        assertNull(wednesdayEntry?.routineId)

        viewModel.clearDaySchedule(DayOfWeek.WEDNESDAY)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.weeklySchedule[DayOfWeek.WEDNESDAY])

        collector.cancel()
    }

    @Test
    fun `completed workout today marks routine as completed today and week ordering respects start day`() = runTest {
        val pushId = fakeRoutineRepo.saveRoutine(
            existingId = null,
            title = "Push",
            notes = "",
            exercises = emptyList(),
        )
        fakeAnalyticsRepo.emitHistory(
            listOf(
                WorkoutHistorySummary(
                    id = "w_today",
                    routineId = pushId,
                    title = "Push",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    durationSeconds = 1800,
                    totalVolumeKg = 1000.0,
                    completedSetsCount = 6,
                    prCount = 0,
                ),
            ),
        )
        val viewModel = TodayViewModel(
            preferencesRepository = fakePrefsRepo,
            profileRepository = fakeProfileRepo,
            workoutRepository = fakeWorkoutRepo,
            routineRepository = fakeRoutineRepo,
            weeklyScheduleRepository = fakeScheduleRepo,
            analyticsRepository = fakeAnalyticsRepo,
            appUpdateRepository = fakeUpdateRepo,
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertTrue(pushId in viewModel.uiState.value.completedTodayRoutineIds)

        val orderedFromSat = WeekScheduleHelper.orderedDaysStartingFrom(DayOfWeek.SATURDAY)
        assertEquals(
            listOf(
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            orderedFromSat,
        )

        val orderedFromWed = WeekScheduleHelper.orderedDaysStartingFrom(DayOfWeek.WEDNESDAY)
        assertEquals(DayOfWeek.WEDNESDAY, orderedFromWed.first())
        assertEquals(DayOfWeek.TUESDAY, orderedFromWed.last())

        collector.cancel()
    }
}

private class FakeWeeklyScheduleRepository : WeeklyScheduleRepository {
    private val scheduleFlow = MutableStateFlow<Map<DayOfWeek, WeeklyScheduleEntry>>(emptyMap())

    override fun observeWeeklySchedule(): Flow<Map<DayOfWeek, WeeklyScheduleEntry>> = scheduleFlow

    override suspend fun setDayRoutine(dayOfWeek: DayOfWeek, routineId: String) {
        scheduleFlow.value = scheduleFlow.value + (
            dayOfWeek to WeeklyScheduleEntry(
                dayOfWeek = dayOfWeek,
                status = ScheduleDayStatus.ROUTINE,
                routineId = routineId,
                updatedAt = 1_000L,
            )
        )
    }

    override suspend fun setDayRest(dayOfWeek: DayOfWeek) {
        scheduleFlow.value = scheduleFlow.value + (
            dayOfWeek to WeeklyScheduleEntry(
                dayOfWeek = dayOfWeek,
                status = ScheduleDayStatus.REST,
                routineId = null,
                updatedAt = 1_000L,
            )
        )
    }

    override suspend fun clearDay(dayOfWeek: DayOfWeek) {
        scheduleFlow.value = scheduleFlow.value - dayOfWeek
    }
}

private class FakeTodayAnalyticsRepository : AnalyticsRepository {
    private val historyFlow = MutableStateFlow<List<WorkoutHistorySummary>>(emptyList())

    fun emitHistory(list: List<WorkoutHistorySummary>) {
        historyFlow.value = list
    }

    override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = historyFlow
    override fun observeWorkoutDetail(workoutId: String): Flow<WorkoutSession?> = MutableStateFlow(null)
    override fun observeAllPersonalRecords(): Flow<List<PersonalRecord>> = MutableStateFlow(emptyList())
    override fun observeMuscleVolumeDistribution(days: Int): Flow<List<MuscleVolumeDistribution>> = MutableStateFlow(emptyList())
    override fun observeExercise1RmProgression(exerciseId: String): Flow<List<ProgressionPoint>> = MutableStateFlow(emptyList())
    override fun observeExerciseWeightProgression(exerciseId: String): Flow<List<ProgressionPoint>> = MutableStateFlow(emptyList())
    override fun observeBodyWeightTrend(days: Int): Flow<List<BodyWeightEntry>> = MutableStateFlow(emptyList())
    override fun observeInsights(): Flow<List<ProgressiveInsight>> = MutableStateFlow(emptyList())
    override suspend fun logBodyWeight(weightKg: Double, recordedAt: Long) = Unit
    override suspend fun deleteBodyWeight(id: String) = Unit
    override suspend fun deleteWorkout(workoutId: String) = Unit
    override suspend fun evaluateAndPersistWorkoutPrs(workoutId: String): Int = 0
}

private class FakeTodayUpdateRepository : AppUpdateRepository {
    override suspend fun checkForUpdate(): Result<AppUpdateInfo> = Result.failure(UnsupportedOperationException())
    override suspend fun downloadUpdate(
        downloadUrl: String,
        fileName: String,
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<File> = Result.failure(UnsupportedOperationException())
}
