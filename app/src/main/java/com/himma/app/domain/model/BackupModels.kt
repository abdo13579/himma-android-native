package com.himma.app.domain.model

import kotlinx.serialization.Serializable

enum class RestoreMode {
    REPLACE,
    MERGE,
}

enum class BackupValidationError {
    EMPTY_FILE,
    INVALID_JSON,
    UNSUPPORTED_FORMAT,
    UNSUPPORTED_VERSION,
    CORRUPT_STRUCTURE,
}

class BackupValidationException(
    val error: BackupValidationError,
    message: String = error.name,
) : IllegalArgumentException(message)

@Serializable
data class HimmaBackupDocument(
    val format: String = FORMAT_ID,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long = 0L,
    val appVersion: String = "0.1.1-beta",
    val profile: BackupProfile? = null,
    val preferences: BackupPreferences? = null,
    val customExercises: List<BackupExercise> = emptyList(),
    val routines: List<BackupRoutine> = emptyList(),
    val routineExercises: List<BackupRoutineExercise> = emptyList(),
    val workouts: List<BackupWorkout> = emptyList(),
    val workoutSets: List<BackupWorkoutSet> = emptyList(),
    val personalRecords: List<BackupPersonalRecord> = emptyList(),
    val bodyWeights: List<BackupBodyWeight> = emptyList(),
) {
    companion object {
        const val FORMAT_ID = "himma-backup"
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupProfile(
    val displayName: String? = null,
    val bodyWeightKg: Double? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class BackupPreferences(
    val languageTag: String = AppLanguage.ARABIC.tag,
    val weightUnit: String = WeightUnit.KG.code,
    val defaultRestSeconds: Int = 90,
    val themeMode: String = ThemeMode.AMOLED_DARK.key,
    val useDynamicColor: Boolean = false,
)

@Serializable
data class BackupExercise(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String,
    val instructionsEn: String = "",
    val instructionsAr: String = "",
    val localMediaKey: String? = null,
    val defaultRestSeconds: Int = 90,
    val isCustom: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class BackupRoutine(
    val id: String,
    val title: String,
    val notes: String = "",
    val orderIndex: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

@Serializable
data class BackupRoutineExercise(
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val orderIndex: Int = 0,
    val targetSets: Int = 3,
    val targetRepsMin: Int = 8,
    val targetRepsMax: Int = 12,
    val targetRestSeconds: Int = 90,
)

@Serializable
data class BackupWorkout(
    val id: String,
    val routineId: String? = null,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val durationSeconds: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val notes: String = "",
    val isCompleted: Boolean = true,
)

@Serializable
data class BackupWorkoutSet(
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseOrder: Int = 0,
    val setIndex: Int = 0,
    val restSeconds: Int = 90,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rpe: Double? = null,
    val setType: String = SetType.NORMAL.name,
    val isCompleted: Boolean = false,
    val isPr: Boolean = false,
    val completedAt: Long? = null,
)

@Serializable
data class BackupPersonalRecord(
    val id: String,
    val exerciseId: String,
    val maxWeightKg: Double? = null,
    val maxVolumeSetKg: Double? = null,
    val estimated1rmKg: Double? = null,
    val achievedAt: Long = 0L,
    val workoutId: String? = null,
)

@Serializable
data class BackupBodyWeight(
    val id: String,
    val weightKg: Double,
    val recordedAt: Long,
)

data class BackupPreview(
    val exportedAt: Long,
    val appVersion: String,
    val schemaVersion: Int,
    val hasProfile: Boolean,
    val customExerciseCount: Int,
    val routineCount: Int,
    val workoutCount: Int,
    val setCount: Int,
)

data class RestoreSummary(
    val mode: RestoreMode,
    val restoredCustomExercises: Int,
    val restoredRoutines: Int,
    val restoredWorkouts: Int,
    val restoredSets: Int,
    val skippedMissingExercises: Int = 0,
)
