package com.himma.app.features.analytics

import com.himma.app.core.database.AnalyticsRepository
import com.himma.app.domain.model.BodyWeightEntry
import com.himma.app.domain.model.InsightType
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.MuscleVolumeDistribution
import com.himma.app.domain.model.PersonalRecord
import com.himma.app.domain.model.ProgressionPoint
import com.himma.app.domain.model.ProgressiveInsight
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WorkoutHistorySummary
import com.himma.app.domain.model.WorkoutSession
import com.himma.app.features.exercises.FakeExerciseRepository
import com.himma.app.features.onboarding.FakeUserPreferencesRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAnalyticsRepo: FakeAnalyticsRepository
    private lateinit var fakeExerciseRepo: FakeExerciseRepository
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAnalyticsRepo = FakeAnalyticsRepository()
        fakeExerciseRepo = FakeExerciseRepository()
        fakePrefsRepo = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `analytics switches tabs, toggles progression curves, and logs or deletes body weight`() = runTest {
        val viewModel = AnalyticsViewModel(
            analyticsRepository = fakeAnalyticsRepo,
            exerciseRepository = fakeExerciseRepo,
            preferencesRepository = fakePrefsRepo,
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(AnalyticsTab.HISTORY, viewModel.uiState.value.selectedTab)
        assertEquals(1, viewModel.uiState.value.workoutHistory.size)

        // Open and close workout detail
        viewModel.openWorkoutDetail("w1")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.selectedWorkoutDetail)
        viewModel.closeWorkoutDetail()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedWorkoutDetail)

        // Select PR -> jumps directly to PROGRESSION tab
        viewModel.selectExerciseForProgression("Barbell_Bench_Press")
        advanceUntilIdle()
        assertEquals(AnalyticsTab.PROGRESSION, viewModel.uiState.value.selectedTab)
        assertEquals(112.5, viewModel.uiState.value.progressionPoints.first().value, 0.1)

        // Switch progression mode to MAX_WEIGHT
        viewModel.setProgressionMode(ProgressionChartMode.MAX_WEIGHT)
        advanceUntilIdle()
        assertEquals(100.0, viewModel.uiState.value.progressionPoints.first().value, 0.1)

        // Body weight tab: invalid input sets error, valid input in LBS converts to KG
        viewModel.selectTab(AnalyticsTab.BODY_WEIGHT)
        viewModel.updateBodyWeightInput("-5")
        viewModel.logBodyWeight()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.bodyWeightError)

        fakePrefsRepo.setWeightUnit(WeightUnit.LBS)
        advanceUntilIdle()
        viewModel.updateBodyWeightInput("220.5")
        viewModel.logBodyWeight()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.bodyWeightError)
        assertEquals("", viewModel.uiState.value.bodyWeightInput)
        assertEquals(1, viewModel.uiState.value.bodyWeightEntries.size)
        assertEquals(100.0, viewModel.uiState.value.bodyWeightEntries.first().weightKg, 0.1)

        val entryId = viewModel.uiState.value.bodyWeightEntries.first().id
        viewModel.deleteBodyWeight(entryId)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.bodyWeightEntries.isEmpty())

        collector.cancel()
    }
}

private class FakeAnalyticsRepository : AnalyticsRepository {
    private val historyFlow = MutableStateFlow(
        listOf(
            WorkoutHistorySummary(
                id = "w1",
                routineId = null,
                title = "Push Day",
                startTime = 1_700_000_000_000L,
                endTime = 1_700_003_600_000L,
                durationSeconds = 3600,
                totalVolumeKg = 2400.0,
                completedSetsCount = 12,
                prCount = 2,
            ),
        ),
    )

    private val prsFlow = MutableStateFlow(
        listOf(
            PersonalRecord(
                id = "pr_bench",
                exerciseId = "Barbell_Bench_Press",
                exerciseNameAr = "بنش بار مستوي",
                exerciseNameEn = "Barbell Bench Press",
                maxWeightKg = 100.0,
                maxVolumeSetKg = 500.0,
                estimated1rmKg = 112.5,
                achievedAt = 1_700_000_000_000L,
                workoutId = "w1",
            ),
        ),
    )

    private val distFlow = MutableStateFlow(
        listOf(
            MuscleVolumeDistribution(MuscleGroup.CHEST, 12, 2400.0, 100.0),
        ),
    )

    private val insightsFlow = MutableStateFlow(
        listOf(
            ProgressiveInsight(
                type = InsightType.MILESTONE,
                titleAr = "إنجاز جديد",
                titleEn = "New PR",
                descriptionAr = "عاش",
                descriptionEn = "Nice",
            ),
        ),
    )

    private val bodyWeightFlow = MutableStateFlow<List<BodyWeightEntry>>(emptyList())

    override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = historyFlow

    override fun observeWorkoutDetail(workoutId: String): Flow<WorkoutSession?> =
        MutableStateFlow(
            WorkoutSession(
                id = workoutId,
                routineId = null,
                title = "Push Day",
                startTime = 1_700_000_000_000L,
                endTime = 1_700_003_600_000L,
                durationSeconds = 3600,
                totalVolumeKg = 2400.0,
                notes = "",
                isCompleted = true,
                exercises = emptyList(),
            ),
        )

    override fun observeAllPersonalRecords(): Flow<List<PersonalRecord>> = prsFlow

    override fun observeMuscleVolumeDistribution(days: Int): Flow<List<MuscleVolumeDistribution>> = distFlow

    override fun observeExercise1RmProgression(exerciseId: String): Flow<List<ProgressionPoint>> =
        MutableStateFlow(listOf(ProgressionPoint(1_700_000_000_000L, 112.5, 5, "Push Day")))

    override fun observeExerciseWeightProgression(exerciseId: String): Flow<List<ProgressionPoint>> =
        MutableStateFlow(listOf(ProgressionPoint(1_700_000_000_000L, 100.0, 5, "Push Day")))

    override fun observeBodyWeightTrend(days: Int): Flow<List<BodyWeightEntry>> = bodyWeightFlow

    override fun observeInsights(): Flow<List<ProgressiveInsight>> = insightsFlow

    override suspend fun logBodyWeight(weightKg: Double, recordedAt: Long) {
        bodyWeightFlow.value = bodyWeightFlow.value + BodyWeightEntry(
            id = "bw_${bodyWeightFlow.value.size + 1}",
            weightKg = weightKg,
            recordedAt = recordedAt,
        )
    }

    override suspend fun deleteBodyWeight(id: String) {
        bodyWeightFlow.value = bodyWeightFlow.value.filterNot { it.id == id }
    }

    override suspend fun evaluateAndPersistWorkoutPrs(workoutId: String): Int = 0
}
