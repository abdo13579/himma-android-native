package com.himma.app.core.backup

import com.himma.app.domain.model.BackupPreview
import com.himma.app.domain.model.BackupValidationError
import com.himma.app.domain.model.BackupValidationException
import com.himma.app.domain.model.HimmaBackupDocument
import com.himma.app.domain.model.ScheduleDayStatus
import com.himma.app.domain.model.WeekScheduleHelper
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
        customExerciseCount = document.customExercises.count { !it.isDeleted },
        routineCount = document.routines.count { !it.isDeleted },
        workoutCount = document.workouts.size,
        setCount = document.workoutSets.size,
        weeklyScheduleCount = document.weeklySchedule.size,
    )

    private fun isStructurallyValid(document: HimmaBackupDocument): Boolean {
        document.profile?.let { profile ->
            val bw = profile.bodyWeightKg
            if (bw != null && (!bw.isFinite() || bw <= 0.0 || bw > 1000.0)) {
                return false
            }
        }

        val customIds = document.customExercises.map { it.id }
        if (customIds.any { it.isBlank() } || customIds.distinct().size != customIds.size) {
            return false
        }
        if (document.customExercises.any { it.defaultRestSeconds < 0 }) {
            return false
        }

        val routineIds = document.routines.map { it.id }
        if (routineIds.any { it.isBlank() } || routineIds.distinct().size != routineIds.size) {
            return false
        }
        val routineIdSet = routineIds.toSet()

        val routineExIds = document.routineExercises.map { it.id }
        if (routineExIds.any { it.isBlank() } || routineExIds.distinct().size != routineExIds.size) {
            return false
        }
        if (document.routineExercises.any { item ->
                item.routineId.isBlank() ||
                    item.exerciseId.isBlank() ||
                    item.routineId !in routineIdSet ||
                    item.orderIndex < 0 ||
                    item.targetSets <= 0 ||
                    item.targetRepsMin <= 0 ||
                    item.targetRepsMax < item.targetRepsMin ||
                    (item.targetDurationSeconds != null && item.targetDurationSeconds < 0)
            }
        ) {
            return false
        }

        val workoutIds = document.workouts.map { it.id }
        if (workoutIds.any { it.isBlank() } || workoutIds.distinct().size != workoutIds.size) {
            return false
        }
        val workoutIdSet = workoutIds.toSet()

        if (document.workouts.any {
                it.startTime < 0L ||
                    it.durationSeconds < 0 ||
                    !it.totalVolumeKg.isFinite() ||
                    it.totalVolumeKg < 0.0
            }
        ) {
            return false
        }

        val setIds = document.workoutSets.map { it.id }
        if (setIds.any { it.isBlank() } || setIds.distinct().size != setIds.size) {
            return false
        }
        if (document.workoutSets.any { set ->
                set.workoutId.isBlank() ||
                    set.exerciseId.isBlank() ||
                    set.workoutId !in workoutIdSet ||
                    set.exerciseOrder < 0 ||
                    set.setIndex < 0 ||
                    set.restSeconds < 0 ||
                    (set.weightKg != null && (!set.weightKg.isFinite() || set.weightKg < 0.0 || set.weightKg > 5000.0)) ||
                    (set.reps != null && (set.reps < 0 || set.reps > 5000)) ||
                    (set.durationSeconds != null && (set.durationSeconds < 0 || set.durationSeconds > 86_400)) ||
                    (set.rpe != null && (!set.rpe.isFinite() || set.rpe < 0.0 || set.rpe > 10.0))
            }
        ) {
            return false
        }

        val prIds = document.personalRecords.map { it.id }
        val prExercises = document.personalRecords.map { it.exerciseId }
        if (prIds.any { it.isBlank() } || prIds.distinct().size != prIds.size) {
            return false
        }
        if (prExercises.any { it.isBlank() } || prExercises.distinct().size != prExercises.size) {
            return false
        }
        if (document.personalRecords.any { pr ->
                (pr.maxWeightKg != null && (!pr.maxWeightKg.isFinite() || pr.maxWeightKg < 0.0)) ||
                    (pr.maxVolumeSetKg != null && (!pr.maxVolumeSetKg.isFinite() || pr.maxVolumeSetKg < 0.0)) ||
                    (pr.estimated1rmKg != null && (!pr.estimated1rmKg.isFinite() || pr.estimated1rmKg < 0.0))
            }
        ) {
            return false
        }

        val bwIds = document.bodyWeights.map { it.id }
        if (bwIds.any { it.isBlank() } || bwIds.distinct().size != bwIds.size) {
            return false
        }
        if (document.bodyWeights.any { bw ->
                !bw.weightKg.isFinite() || bw.weightKg <= 0.0 || bw.weightKg > 1000.0 || bw.recordedAt < 0L
            }
        ) {
            return false
        }

        val scheduleDays = document.weeklySchedule.map { it.dayOfWeek }
        if (scheduleDays.distinct().size != scheduleDays.size) {
            return false
        }
        if (document.weeklySchedule.any { entry ->
                if (WeekScheduleHelper.dayFromIsoValueOrNull(entry.dayOfWeek) == null) {
                    return@any true
                }
                when (ScheduleDayStatus.fromNameOrNull(entry.status)) {
                    ScheduleDayStatus.ROUTINE -> entry.routineId.isNullOrBlank()
                    ScheduleDayStatus.REST -> false
                    ScheduleDayStatus.UNSET, null -> true
                }
            }
        ) {
            return false
        }

        return true
    }
}
