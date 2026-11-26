package com.himma.app.core.backup

import android.content.Context
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.database.HimmaDatabase
import com.himma.app.core.database.dao.AnalyticsDao
import com.himma.app.core.database.dao.ExerciseDao
import com.himma.app.core.database.dao.RoutineDao
import com.himma.app.core.database.dao.UserProfileDao
import com.himma.app.core.database.dao.WeeklyScheduleDao
import com.himma.app.core.database.dao.WorkoutDao
import com.himma.app.core.database.entity.ExerciseEntity
import com.himma.app.core.database.entity.PersonalRecordEntity
import com.himma.app.core.database.entity.RoutineEntity
import com.himma.app.core.database.entity.WeeklyScheduleEntity
import com.himma.app.domain.model.BackupExercise
import com.himma.app.domain.model.BackupPersonalRecord
import com.himma.app.domain.model.BackupRoutine
import com.himma.app.domain.model.BackupWeeklyScheduleEntry
import com.himma.app.domain.model.HimmaBackupDocument
import com.himma.app.domain.model.RestoreMode
import com.himma.app.domain.model.ScheduleDayStatus
import com.himma.app.features.onboarding.FakeUserPreferencesRepository
import java.time.DayOfWeek
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class LocalBackupRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: HimmaDatabase
    private lateinit var userProfileDao: UserProfileDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var routineDao: RoutineDao
    private lateinit var weeklyScheduleDao: WeeklyScheduleDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var analyticsDao: AnalyticsDao
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var prefsRepository: FakeUserPreferencesRepository
    private lateinit var repository: LocalBackupRepository

    @Before
    fun setup() {
        context = Mockito.mock(Context::class.java)
        database = Mockito.mock(HimmaDatabase::class.java)
        userProfileDao = Mockito.mock(UserProfileDao::class.java)
        exerciseDao = Mockito.mock(ExerciseDao::class.java)
        routineDao = Mockito.mock(RoutineDao::class.java)
        weeklyScheduleDao = Mockito.mock(WeeklyScheduleDao::class.java)
        workoutDao = Mockito.mock(WorkoutDao::class.java)
        analyticsDao = Mockito.mock(AnalyticsDao::class.java)
        exerciseRepository = Mockito.mock(ExerciseRepository::class.java)
        prefsRepository = FakeUserPreferencesRepository()

        repository = LocalBackupRepository(
            context = context,
            database = database,
            userProfileDao = userProfileDao,
            exerciseDao = exerciseDao,
            routineDao = routineDao,
            weeklyScheduleDao = weeklyScheduleDao,
            workoutDao = workoutDao,
            analyticsDao = analyticsDao,
            exerciseRepository = exerciseRepository,
            preferencesRepository = prefsRepository,
        )
    }

    @Test
    fun `applyReplaceRestore restores weeklySchedule skipping deleted routines and restores PRs and soft-deleted custom exercises`() = runTest {
        Mockito.`when`(exerciseDao.getAllExerciseIdsOnce()).thenReturn(listOf("bench-press", "custom_del"))

        val doc = HimmaBackupDocument(
            customExercises = listOf(
                BackupExercise(
                    id = "custom_del",
                    nameEn = "Legacy Pull",
                    nameAr = "سحب قديم",
                    primaryMuscle = "BACK",
                    equipment = "CABLE",
                    isDeleted = true,
                ),
            ),
            routines = listOf(
                BackupRoutine(id = "r_active", title = "Push", isDeleted = false),
                BackupRoutine(id = "r_deleted", title = "Archived", isDeleted = true),
            ),
            weeklySchedule = listOf(
                BackupWeeklyScheduleEntry(
                    dayOfWeek = DayOfWeek.SATURDAY.value,
                    status = ScheduleDayStatus.ROUTINE.name,
                    routineId = "r_active",
                    updatedAt = 100L,
                ),
                BackupWeeklyScheduleEntry(
                    dayOfWeek = DayOfWeek.SUNDAY.value,
                    status = ScheduleDayStatus.ROUTINE.name,
                    routineId = "r_deleted",
                    updatedAt = 100L,
                ),
                BackupWeeklyScheduleEntry(
                    dayOfWeek = DayOfWeek.MONDAY.value,
                    status = ScheduleDayStatus.REST.name,
                    routineId = null,
                    updatedAt = 200L,
                ),
            ),
            personalRecords = listOf(
                BackupPersonalRecord(
                    id = "pr_1",
                    exerciseId = "bench-press",
                    maxWeightKg = 110.0,
                    maxVolumeSetKg = 550.0,
                    estimated1rmKg = 123.75,
                    achievedAt = 500L,
                ),
            ),
        )

        val summary = repository.applyReplaceRestore(doc)

        assertEquals(RestoreMode.REPLACE, summary.mode)
        assertEquals(1, summary.restoredCustomExercises)
        assertEquals(1, summary.restoredRoutines)
        assertEquals(2, summary.restoredScheduleDays)

        Mockito.verify(weeklyScheduleDao).upsertAll(
            listOf(
                WeeklyScheduleEntity(
                    dayOfWeek = DayOfWeek.SATURDAY.value,
                    status = ScheduleDayStatus.ROUTINE.name,
                    routineId = "r_active",
                    updatedAt = 100L,
                ),
                WeeklyScheduleEntity(
                    dayOfWeek = DayOfWeek.MONDAY.value,
                    status = ScheduleDayStatus.REST.name,
                    routineId = null,
                    updatedAt = 200L,
                ),
            ),
        )
        Mockito.verify(analyticsDao).upsertPersonalRecords(
            listOf(
                PersonalRecordEntity(
                    id = "pr_1",
                    exerciseId = "bench-press",
                    maxWeightKg = 110.0,
                    maxVolumeSetKg = 550.0,
                    estimated1rmKg = 123.75,
                    achievedAt = 500L,
                    workoutId = null,
                ),
            ),
        )
    }

    @Test
    fun `applyMergeRestore resolves weeklySchedule by updatedAt and keeps better PR`() = runTest {
        Mockito.`when`(exerciseDao.getCustomExercisesOnce()).thenReturn(emptyList())
        Mockito.`when`(exerciseDao.getAllExerciseIdsOnce()).thenReturn(listOf("bench-press"))
        Mockito.`when`(routineDao.getAllRoutinesOnce()).thenReturn(
            listOf(
                RoutineEntity(
                    id = "r_1",
                    title = "Upper",
                    notes = "",
                    orderIndex = 0,
                    createdAt = 1L,
                    updatedAt = 1L,
                    isDeleted = false,
                ),
            ),
        )
        Mockito.`when`(weeklyScheduleDao.getAllOnce()).thenReturn(
            listOf(
                WeeklyScheduleEntity(
                    dayOfWeek = DayOfWeek.TUESDAY.value,
                    status = ScheduleDayStatus.REST.name,
                    routineId = null,
                    updatedAt = 1000L,
                ),
                WeeklyScheduleEntity(
                    dayOfWeek = DayOfWeek.WEDNESDAY.value,
                    status = ScheduleDayStatus.REST.name,
                    routineId = null,
                    updatedAt = 50L,
                ),
            ),
        )
        Mockito.`when`(workoutDao.getAllWorkoutsOnce()).thenReturn(emptyList())
        Mockito.`when`(analyticsDao.getAllPersonalRecordsOnce()).thenReturn(
            listOf(
                PersonalRecordEntity(
                    id = "pr_existing",
                    exerciseId = "bench-press",
                    maxWeightKg = 100.0,
                    maxVolumeSetKg = 500.0,
                    estimated1rmKg = 112.5,
                    achievedAt = 100L,
                    workoutId = null,
                ),
            ),
        )
        Mockito.`when`(analyticsDao.getAllBodyWeightsOnce()).thenReturn(emptyList())

        val doc = HimmaBackupDocument(
            weeklySchedule = listOf(
                // Older than existing (1000L) -> must be skipped
                BackupWeeklyScheduleEntry(
                    dayOfWeek = DayOfWeek.TUESDAY.value,
                    status = ScheduleDayStatus.ROUTINE.name,
                    routineId = "r_1",
                    updatedAt = 500L,
                ),
                // Newer than existing (50L) -> must update
                BackupWeeklyScheduleEntry(
                    dayOfWeek = DayOfWeek.WEDNESDAY.value,
                    status = ScheduleDayStatus.ROUTINE.name,
                    routineId = "r_1",
                    updatedAt = 900L,
                ),
            ),
            personalRecords = listOf(
                BackupPersonalRecord(
                    id = "pr_incoming",
                    exerciseId = "bench-press",
                    maxWeightKg = 120.0,
                    maxVolumeSetKg = 480.0,
                    estimated1rmKg = 120.0,
                    achievedAt = 2000L,
                ),
            ),
        )

        val summary = repository.applyMergeRestore(doc)

        assertEquals(RestoreMode.MERGE, summary.mode)
        assertEquals(1, summary.restoredScheduleDays)

        Mockito.verify(weeklyScheduleDao).upsertAll(
            listOf(
                WeeklyScheduleEntity(
                    dayOfWeek = DayOfWeek.WEDNESDAY.value,
                    status = ScheduleDayStatus.ROUTINE.name,
                    routineId = "r_1",
                    updatedAt = 900L,
                ),
            ),
        )
        Mockito.verify(analyticsDao).upsertPersonalRecords(
            listOf(
                PersonalRecordEntity(
                    id = "pr_existing",
                    exerciseId = "bench-press",
                    maxWeightKg = 120.0,
                    maxVolumeSetKg = 500.0,
                    estimated1rmKg = 120.0,
                    achievedAt = 2000L,
                    workoutId = null,
                ),
            ),
        )
    }
}
