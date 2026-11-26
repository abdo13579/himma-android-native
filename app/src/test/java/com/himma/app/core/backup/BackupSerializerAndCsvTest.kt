package com.himma.app.core.backup

import com.himma.app.core.database.dao.WorkoutSetWithExercise
import com.himma.app.core.database.dao.WorkoutWithSets
import com.himma.app.core.database.entity.ExerciseEntity
import com.himma.app.core.database.entity.WorkoutEntity
import com.himma.app.core.database.entity.WorkoutSetEntity
import com.himma.app.domain.model.BackupExercise
import com.himma.app.domain.model.BackupPreferences
import com.himma.app.domain.model.BackupProfile
import com.himma.app.domain.model.BackupRoutine
import com.himma.app.domain.model.BackupRoutineExercise
import com.himma.app.domain.model.BackupValidationError
import com.himma.app.domain.model.BackupValidationException
import com.himma.app.domain.model.BackupWeeklyScheduleEntry
import com.himma.app.domain.model.BackupWorkout
import com.himma.app.domain.model.BackupWorkoutSet
import com.himma.app.domain.model.HimmaBackupDocument
import com.himma.app.domain.model.ScheduleDayStatus
import com.himma.app.domain.model.WeightUnit
import java.time.DayOfWeek
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerAndCsvTest {

    @Test
    fun `backup encode and decode round-trips complete document with preview counts`() {
        val doc = HimmaBackupDocument(
            exportedAt = 1_731_000_000_000L,
            appVersion = "0.1.1-beta",
            profile = BackupProfile(
                displayName = "عبده",
                bodyWeightKg = 80.5,
                createdAt = 100L,
                updatedAt = 200L,
            ),
            preferences = BackupPreferences(
                languageTag = "ar",
                weightUnit = "kg",
                defaultRestSeconds = 90,
                themeMode = "AMOLED_DARK",
                useDynamicColor = false,
            ),
            customExercises = listOf(
                BackupExercise(
                    id = "custom_1",
                    nameEn = "Cable Lateral Raise",
                    nameAr = "رفرفة جانبي كيبل",
                    primaryMuscle = "SHOULDERS",
                    secondaryMuscles = listOf("CORE"),
                    equipment = "CABLE",
                    isCustom = true,
                ),
            ),
            routines = listOf(
                BackupRoutine(id = "r1", title = "Push Day", notes = "Heavy", orderIndex = 0),
                BackupRoutine(id = "r_deleted", title = "Old", isDeleted = true),
            ),
            routineExercises = listOf(
                BackupRoutineExercise(
                    id = "re1",
                    routineId = "r1",
                    exerciseId = "custom_1",
                    targetSets = 4,
                    targetRepsMin = 10,
                    targetRepsMax = 15,
                ),
            ),
            workouts = listOf(
                BackupWorkout(
                    id = "w1",
                    routineId = "r1",
                    title = "Push Day",
                    startTime = 1_731_000_000_000L,
                    endTime = 1_731_003_600_000L,
                    durationSeconds = 3600,
                    totalVolumeKg = 1200.0,
                    isCompleted = true,
                ),
            ),
            workoutSets = listOf(
                BackupWorkoutSet(
                    id = "ws1",
                    workoutId = "w1",
                    exerciseId = "custom_1",
                    exerciseOrder = 0,
                    setIndex = 0,
                    weightKg = 12.5,
                    reps = 12,
                    isCompleted = true,
                ),
            ),
        )

        val encoded = BackupSerializer.encode(doc)
        val decoded = BackupSerializer.decodeAndValidate(encoded).getOrThrow()
        val preview = BackupSerializer.preview(decoded)

        assertEquals(doc, decoded)
        assertTrue(preview.hasProfile)
        assertEquals(1, preview.customExerciseCount)
        assertEquals(1, preview.routineCount) // ignores soft-deleted routines
        assertEquals(1, preview.workoutCount)
        assertEquals(1, preview.setCount)
    }

    @Test
    fun `decode rejects invalid json, wrong format, newer schema version, and orphan references`() {
        val emptyResult = BackupSerializer.decodeAndValidate("   ")
        assertEquals(
            BackupValidationError.EMPTY_FILE,
            (emptyResult.exceptionOrNull() as BackupValidationException).error,
        )

        val badJsonResult = BackupSerializer.decodeAndValidate("{not valid json")
        assertEquals(
            BackupValidationError.INVALID_JSON,
            (badJsonResult.exceptionOrNull() as BackupValidationException).error,
        )

        val wrongFormat = BackupSerializer.encode(
            HimmaBackupDocument(format = "other-app", schemaVersion = 1),
        )
        assertEquals(
            BackupValidationError.UNSUPPORTED_FORMAT,
            (BackupSerializer.decodeAndValidate(wrongFormat).exceptionOrNull() as BackupValidationException).error,
        )

        val futureVersion = BackupSerializer.encode(
            HimmaBackupDocument(format = HimmaBackupDocument.FORMAT_ID, schemaVersion = 99),
        )
        assertEquals(
            BackupValidationError.UNSUPPORTED_VERSION,
            (BackupSerializer.decodeAndValidate(futureVersion).exceptionOrNull() as BackupValidationException).error,
        )

        val corruptOrphanSet = BackupSerializer.encode(
            HimmaBackupDocument(
                workoutSets = listOf(
                    BackupWorkoutSet(
                        id = "ws_orphan",
                        workoutId = "missing_workout",
                        exerciseId = "Barbell_Bench_Press",
                    ),
                ),
            ),
        )
        assertEquals(
            BackupValidationError.CORRUPT_STRUCTURE,
            (BackupSerializer.decodeAndValidate(corruptOrphanSet).exceptionOrNull() as BackupValidationException).error,
        )
    }

    @Test
    fun `csv exporter includes utf8 bom, converts weight unit, and escapes quotes and commas`() {
        val exercise = ExerciseEntity(
            id = "Barbell_Bench_Press",
            nameEn = "Barbell Bench Press",
            nameAr = "بنش بار مستوي",
            nameArNormalized = "بنش بار مستوي",
            primaryMuscle = "CHEST",
            secondaryMuscles = "ARMS,SHOULDERS",
            equipment = "BARBELL",
            instructionsEn = "",
            instructionsAr = "",
            localMediaKey = null,
            defaultRestSeconds = 90,
            isCustom = false,
            createdAt = 1L,
            updatedAt = 1L,
        )
        val workout = WorkoutWithSets(
            workout = WorkoutEntity(
                id = "w1",
                routineId = null,
                title = "Chest, \"Heavy\" Day",
                startTime = 1_700_000_000_000L,
                endTime = 1_700_003_600_000L,
                durationSeconds = 3600,
                totalVolumeKg = 1000.0,
                notes = "Felt strong,\nPR next week",
                isCompleted = true,
            ),
            sets = listOf(
                WorkoutSetWithExercise(
                    set = WorkoutSetEntity(
                        id = "s1",
                        workoutId = "w1",
                        exerciseId = "Barbell_Bench_Press",
                        exerciseOrder = 0,
                        setIndex = 0,
                        restSeconds = 90,
                        weightKg = 100.0,
                        reps = 5,
                        rpe = 8.5,
                        setType = "NORMAL",
                        isCompleted = true,
                        isPr = true,
                        completedAt = 1_700_001_000_000L,
                    ),
                    exercise = exercise,
                ),
            ),
        )

        val csv = CsvWorkoutExporter.exportWorkouts(
            workouts = listOf(workout),
            weightUnit = WeightUnit.LBS,
            zoneId = ZoneOffset.UTC,
        )

        assertTrue(csv.startsWith("\uFEFFworkout_id,date_iso,workout_title"))
        assertTrue(csv.contains("\"Chest, \"\"Heavy\"\" Day\""))
        assertTrue(csv.contains("\"Felt strong,\nPR next week\""))
        assertTrue(csv.contains("بنش بار مستوي"))
        assertTrue(csv.contains(",220.5,lbs,5,8.5,true,true,"))
    }

    @Test
    fun `backup encodes and validates partial custom document like routines only or theme only`() {
        val routinesOnlyDoc = HimmaBackupDocument(
            exportedAt = 1_731_000_000_000L,
            appVersion = "0.1.1-beta",
            routines = listOf(
                BackupRoutine(id = "r1", title = "Upper Body", notes = "Chest & Back", orderIndex = 0),
            ),
        )
        val encodedRoutines = BackupSerializer.encode(routinesOnlyDoc)
        val decodedRoutines = BackupSerializer.decodeAndValidate(encodedRoutines).getOrThrow()
        assertEquals(1, decodedRoutines.routines.size)
        assertTrue(decodedRoutines.customExercises.isEmpty())
        assertTrue(decodedRoutines.workouts.isEmpty())

        val themeOnlyDoc = HimmaBackupDocument(
            exportedAt = 1_731_000_000_000L,
            appVersion = "0.1.1-beta",
            preferences = BackupPreferences(
                languageTag = "ar",
                weightUnit = "kg",
                themeMode = "FROST",
            ),
        )
        val encodedTheme = BackupSerializer.encode(themeOnlyDoc)
        val decodedTheme = BackupSerializer.decodeAndValidate(encodedTheme).getOrThrow()
        assertEquals("FROST", decodedTheme.preferences?.themeMode)
        assertTrue(decodedTheme.routines.isEmpty())
    }

    @Test
    fun `backup encodes and validates weekly schedule and week start day`() {
        val doc = HimmaBackupDocument(
            exportedAt = 1_731_000_000_000L,
            appVersion = "1.3.0",
            preferences = BackupPreferences(
                weekStartDay = DayOfWeek.SUNDAY.name,
            ),
            routines = listOf(
                BackupRoutine(id = "pull_1", title = "Pull Day"),
            ),
            weeklySchedule = listOf(
                BackupWeeklyScheduleEntry(
                    dayOfWeek = DayOfWeek.TUESDAY.value,
                    status = ScheduleDayStatus.ROUTINE.name,
                    routineId = "pull_1",
                    updatedAt = 1000L,
                ),
                BackupWeeklyScheduleEntry(
                    dayOfWeek = DayOfWeek.WEDNESDAY.value,
                    status = ScheduleDayStatus.REST.name,
                    routineId = null,
                    updatedAt = 2000L,
                ),
            ),
        )

        val encoded = BackupSerializer.encode(doc)
        val decoded = BackupSerializer.decodeAndValidate(encoded).getOrThrow()
        val preview = BackupSerializer.preview(decoded)

        assertEquals(DayOfWeek.SUNDAY.name, decoded.preferences?.weekStartDay)
        assertEquals(2, decoded.weeklySchedule.size)
        assertEquals(2, preview.weeklyScheduleCount)

        val invalidDoc = doc.copy(
            weeklySchedule = listOf(
                BackupWeeklyScheduleEntry(
                    dayOfWeek = DayOfWeek.MONDAY.value,
                    status = ScheduleDayStatus.ROUTINE.name,
                    routineId = null,
                ),
            ),
        )
        val invalidResult = BackupSerializer.decodeAndValidate(BackupSerializer.encode(invalidDoc))
        assertTrue(invalidResult.isFailure)
        val ex = invalidResult.exceptionOrNull() as BackupValidationException
        assertEquals(BackupValidationError.CORRUPT_STRUCTURE, ex.error)
    }

    @Test
    fun `decode rejects duplicate ids and out-of-range numeric values`() {
        val duplicateSetDoc = HimmaBackupDocument(
            workouts = listOf(BackupWorkout(id = "w1", title = "W1", startTime = 100L)),
            workoutSets = listOf(
                BackupWorkoutSet(id = "dup", workoutId = "w1", exerciseId = "ex1"),
                BackupWorkoutSet(id = "dup", workoutId = "w1", exerciseId = "ex1", setIndex = 1),
            ),
        )
        val dupResult = BackupSerializer.decodeAndValidate(BackupSerializer.encode(duplicateSetDoc))
        assertEquals(
            BackupValidationError.CORRUPT_STRUCTURE,
            (dupResult.exceptionOrNull() as BackupValidationException).error,
        )

        val negativeRepsDoc = HimmaBackupDocument(
            workouts = listOf(BackupWorkout(id = "w1", title = "W1", startTime = 100L)),
            workoutSets = listOf(
                BackupWorkoutSet(id = "s1", workoutId = "w1", exerciseId = "ex1", reps = -3),
            ),
        )
        val rangeResult = BackupSerializer.decodeAndValidate(BackupSerializer.encode(negativeRepsDoc))
        assertEquals(
            BackupValidationError.CORRUPT_STRUCTURE,
            (rangeResult.exceptionOrNull() as BackupValidationException).error,
        )
    }

    @Test
    fun `csv exporter neutralizes formula injection and exports stack plates and set duration`() {
        val exercise = ExerciseEntity(
            id = "Cable_Row",
            nameEn = "Cable Row",
            nameAr = "=SUM(A1:A2)",
            nameArNormalized = "sum",
            primaryMuscle = "BACK",
            secondaryMuscles = "",
            equipment = "CABLE",
            instructionsEn = "",
            instructionsAr = "",
            localMediaKey = null,
            defaultRestSeconds = 90,
            isCustom = false,
            createdAt = 1L,
            updatedAt = 1L,
        )
        val workout = WorkoutWithSets(
            workout = WorkoutEntity(
                id = "w1",
                routineId = null,
                title = "+InjectedTitle",
                startTime = 1_700_000_000_000L,
                endTime = 1_700_001_000_000L,
                durationSeconds = 1000,
                totalVolumeKg = 0.0,
                notes = "@cmd",
                isCompleted = true,
            ),
            sets = listOf(
                WorkoutSetWithExercise(
                    set = WorkoutSetEntity(
                        id = "s1",
                        workoutId = "w1",
                        exerciseId = "Cable_Row",
                        exerciseOrder = 0,
                        setIndex = 0,
                        restSeconds = 90,
                        weightKg = 6.0,
                        reps = 12,
                        durationSeconds = 45,
                        rpe = null,
                        setType = "NORMAL",
                        isCompleted = true,
                        isPr = false,
                        completedAt = null,
                        weightUnitMode = "STACK_PLATES",
                    ),
                    exercise = exercise,
                ),
            ),
        )

        val csv = CsvWorkoutExporter.exportWorkouts(
            workouts = listOf(workout),
            weightUnit = WeightUnit.LBS,
            zoneId = ZoneOffset.UTC,
        )

        assertTrue(csv.contains("'+InjectedTitle"))
        assertTrue(csv.contains("'=SUM(A1:A2)"))
        assertTrue(csv.contains("'@cmd"))
        assertTrue(csv.contains(",6,plates,12,"))
        assertTrue(csv.contains(",45,STACK_PLATES"))
    }
}
