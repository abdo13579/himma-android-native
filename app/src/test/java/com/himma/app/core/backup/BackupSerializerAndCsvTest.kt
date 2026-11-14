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
import com.himma.app.domain.model.BackupWorkout
import com.himma.app.domain.model.BackupWorkoutSet
import com.himma.app.domain.model.HimmaBackupDocument
import com.himma.app.domain.model.WeightUnit
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
}
