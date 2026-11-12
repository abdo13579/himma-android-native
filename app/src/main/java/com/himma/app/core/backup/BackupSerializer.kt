package com.himma.app.core.backup

import com.himma.app.domain.model.BackupPreview
import com.himma.app.domain.model.BackupValidationError
import com.himma.app.domain.model.BackupValidationException
import com.himma.app.domain.model.HimmaBackupDocument
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupSerializer {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(document: HimmaBackupDocument): String = json.encodeToString(document)

    fun decodeAndValidate(rawJson: String): Result<HimmaBackupDocument> {
        val trimmed = rawJson.trimStart('\uFEFF').trim()
        if (trimmed.isEmpty()) {
            return Result.failure(BackupValidationException(BackupValidationError.EMPTY_FILE))
        }

        val parsed = runCatching {
            json.decodeFromString<HimmaBackupDocument>(trimmed)
        }.getOrElse {
            return Result.failure(
                BackupValidationException(
                    error = BackupValidationError.INVALID_JSON,
                    message = it.message ?: "Invalid backup JSON",
                ),
            )
        }

        if (parsed.format != HimmaBackupDocument.FORMAT_ID) {
            return Result.failure(BackupValidationException(BackupValidationError.UNSUPPORTED_FORMAT))
        }

        if (parsed.schemaVersion !in 1..HimmaBackupDocument.CURRENT_SCHEMA_VERSION) {
            return Result.failure(BackupValidationException(BackupValidationError.UNSUPPORTED_VERSION))
        }

        if (!isStructurallyValid(parsed)) {
            return Result.failure(BackupValidationException(BackupValidationError.CORRUPT_STRUCTURE))
        }

        return Result.success(parsed)
    }

    fun preview(document: HimmaBackupDocument): BackupPreview = BackupPreview(
        exportedAt = document.exportedAt,
        appVersion = document.appVersion,
        schemaVersion = document.schemaVersion,
        hasProfile = document.profile != null,
        customExerciseCount = document.customExercises.size,
        routineCount = document.routines.count { !it.isDeleted },
        workoutCount = document.workouts.size,
        setCount = document.workoutSets.size,
    )

    private fun isStructurallyValid(document: HimmaBackupDocument): Boolean {
        val customIds = document.customExercises.map { it.id }
        if (customIds.any { it.isBlank() } || customIds.distinct().size != customIds.size) {
            return false
        }

        val routineIds = document.routines.map { it.id }
        if (routineIds.any { it.isBlank() } || routineIds.distinct().size != routineIds.size) {
            return false
        }
        val routineIdSet = routineIds.toSet()

        if (document.routineExercises.any { item ->
                item.id.isBlank() ||
                    item.routineId.isBlank() ||
                    item.exerciseId.isBlank() ||
                    item.routineId !in routineIdSet ||
                    item.targetSets <= 0 ||
                    item.targetRepsMin <= 0 ||
                    item.targetRepsMax < item.targetRepsMin
            }
        ) {
            return false
        }

        val workoutIds = document.workouts.map { it.id }
        if (workoutIds.any { it.isBlank() } || workoutIds.distinct().size != workoutIds.size) {
            return false
        }
        val workoutIdSet = workoutIds.toSet()

        if (document.workouts.any { it.startTime < 0L || it.durationSeconds < 0 }) {
            return false
        }

        if (document.workoutSets.any { set ->
                set.id.isBlank() ||
                    set.workoutId.isBlank() ||
                    set.exerciseId.isBlank() ||
                    set.workoutId !in workoutIdSet ||
                    set.exerciseOrder < 0 ||
                    set.setIndex < 0
            }
        ) {
            return false
        }

        return true
    }
}
