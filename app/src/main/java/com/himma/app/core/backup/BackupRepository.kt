package com.himma.app.core.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.himma.app.BuildConfig
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.database.HimmaDatabase
import com.himma.app.core.database.dao.AnalyticsDao
import com.himma.app.core.database.dao.ExerciseDao
import com.himma.app.core.database.dao.RoutineDao
import com.himma.app.core.database.dao.UserProfileDao
import com.himma.app.core.database.dao.WorkoutDao
import com.himma.app.core.database.entity.BodyWeightEntity
import com.himma.app.core.database.entity.ExerciseEntity
import com.himma.app.core.database.entity.PersonalRecordEntity
import com.himma.app.core.database.entity.RoutineEntity
import com.himma.app.core.database.entity.RoutineExerciseEntity
import com.himma.app.core.database.entity.UserProfileEntity
import com.himma.app.core.database.entity.WorkoutEntity
import com.himma.app.core.database.entity.WorkoutSetEntity
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.BackupBodyWeight
import com.himma.app.domain.model.BackupExercise
import com.himma.app.domain.model.BackupPersonalRecord
import com.himma.app.domain.model.BackupPreferences
import com.himma.app.domain.model.BackupPreview
import com.himma.app.domain.model.BackupProfile
import com.himma.app.domain.model.BackupRoutine
import com.himma.app.domain.model.BackupRoutineExercise
import com.himma.app.domain.model.BackupValidationError
import com.himma.app.domain.model.BackupValidationException
import com.himma.app.domain.model.BackupWorkout
import com.himma.app.domain.model.BackupWorkoutSet
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.HimmaBackupDocument
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.RestoreMode
import com.himma.app.domain.model.RestoreSummary
import com.himma.app.domain.model.SetType
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.text.ArabicTextNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BackupInspection(
    val document: HimmaBackupDocument,
    val preview: BackupPreview,
)

interface BackupRepository {
    suspend fun createBackupJson(): String
    suspend fun createCsvExport(): String
    suspend fun exportBackupToUri(uri: Uri): Result<Unit>
    suspend fun exportCsvToUri(uri: Uri): Result<Unit>
    suspend fun inspectBackupUri(uri: Uri): Result<BackupInspection>
    suspend fun inspectLatestAutoSnapshot(): Result<BackupInspection>
    fun inspectBackupJson(rawJson: String): Result<BackupInspection>
    suspend fun restoreBackup(document: HimmaBackupDocument, mode: RestoreMode): Result<RestoreSummary>
    suspend fun createLocalAutoSnapshot(): Result<Long>
    fun hasLocalAutoSnapshot(): Boolean
}

@Singleton
class LocalBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: HimmaDatabase,
    private val userProfileDao: UserProfileDao,
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val workoutDao: WorkoutDao,
    private val analyticsDao: AnalyticsDao,
    private val exerciseRepository: ExerciseRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : BackupRepository {

    private val autoBackupDir: File
        get() = File(context.filesDir, AUTO_BACKUP_DIR_NAME)

    private val latestAutoSnapshotFile: File
        get() = File(autoBackupDir, LATEST_SNAPSHOT_FILE_NAME)

    override suspend fun createBackupJson(): String {
        val prefs = preferencesRepository.preferences.first()
        val profile = userProfileDao.getProfileOnce()
        val customExercises = exerciseDao.getCustomExercisesOnce()
        val routines = routineDao.getAllRoutinesOnce()
        val routineExercises = routineDao.getAllRoutineExercisesOnce()
        val workouts = workoutDao.getAllWorkoutsOnce()
        val workoutSets = workoutDao.getAllWorkoutSetsOnce()
        val personalRecords = analyticsDao.getAllPersonalRecordsOnce()
        val bodyWeights = analyticsDao.getAllBodyWeightsOnce()

        val document = HimmaBackupDocument(
            format = HimmaBackupDocument.FORMAT_ID,
            schemaVersion = HimmaBackupDocument.CURRENT_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            profile = profile?.let {
                BackupProfile(
                    displayName = it.displayName,
                    bodyWeightKg = it.bodyWeightKg,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            },
            preferences = BackupPreferences(
                languageTag = prefs.language.tag,
                weightUnit = prefs.weightUnit.code,
                defaultRestSeconds = prefs.defaultRestSeconds,
                themeMode = prefs.themeMode.key,
                useDynamicColor = prefs.useDynamicColor,
            ),
            customExercises = customExercises.map { it.toBackupDto() },
            routines = routines.map {
                BackupRoutine(
                    id = it.id,
                    title = it.title,
                    notes = it.notes,
                    orderIndex = it.orderIndex,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted,
                )
            },
            routineExercises = routineExercises.map {
                BackupRoutineExercise(
                    id = it.id,
                    routineId = it.routineId,
                    exerciseId = it.exerciseId,
                    orderIndex = it.orderIndex,
                    targetSets = it.targetSets,
                    targetRepsMin = it.targetRepsMin,
                    targetRepsMax = it.targetRepsMax,
                    targetRestSeconds = it.targetRestSeconds,
                )
            },
            workouts = workouts.map {
                BackupWorkout(
                    id = it.id,
                    routineId = it.routineId,
                    title = it.title,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    durationSeconds = it.durationSeconds,
                    totalVolumeKg = it.totalVolumeKg,
                    notes = it.notes,
                    isCompleted = it.isCompleted,
                )
            },
            workoutSets = workoutSets.map {
                BackupWorkoutSet(
                    id = it.id,
                    workoutId = it.workoutId,
                    exerciseId = it.exerciseId,
                    exerciseOrder = it.exerciseOrder,
                    setIndex = it.setIndex,
                    restSeconds = it.restSeconds,
                    weightKg = it.weightKg,
                    reps = it.reps,
                    rpe = it.rpe,
                    setType = it.setType,
                    isCompleted = it.isCompleted,
                    isPr = it.isPr,
                    completedAt = it.completedAt,
                )
            },
            personalRecords = personalRecords.map {
                BackupPersonalRecord(
                    id = it.id,
                    exerciseId = it.exerciseId,
                    maxWeightKg = it.maxWeightKg,
                    maxVolumeSetKg = it.maxVolumeSetKg,
                    estimated1rmKg = it.estimated1rmKg,
                    achievedAt = it.achievedAt,
                    workoutId = it.workoutId,
                )
            },
            bodyWeights = bodyWeights.map {
                BackupBodyWeight(
                    id = it.id,
                    weightKg = it.weightKg,
                    recordedAt = it.recordedAt,
                )
            },
        )
        return BackupSerializer.encode(document)
    }

    override suspend fun createCsvExport(): String {
        val weightUnit = preferencesRepository.preferences.first().weightUnit
        val workouts = workoutDao.getCompletedWorkoutsWithSetsOnce()
        return CsvWorkoutExporter.exportWorkouts(
            workouts = workouts,
            weightUnit = weightUnit,
        )
    }

    override suspend fun exportBackupToUri(uri: Uri): Result<Unit> = runCatching {
        val payload = createBackupJson()
        writeTextToUri(uri, payload)
    }

    override suspend fun exportCsvToUri(uri: Uri): Result<Unit> = runCatching {
        val csv = createCsvExport()
        writeTextToUri(uri, csv)
    }

    override suspend fun inspectBackupUri(uri: Uri): Result<BackupInspection> {
        val rawResult = runCatching { readTextFromUri(uri) }
        val raw = rawResult.getOrElse { return Result.failure(it) }
        return inspectBackupJson(raw)
    }

    override suspend fun inspectLatestAutoSnapshot(): Result<BackupInspection> = withContext(Dispatchers.IO) {
        val file = latestAutoSnapshotFile
        if (!file.exists()) {
            return@withContext Result.failure(
                BackupValidationException(BackupValidationError.EMPTY_FILE),
            )
        }
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrElse {
            return@withContext Result.failure(it)
        }
        inspectBackupJson(raw)
    }

    override fun inspectBackupJson(rawJson: String): Result<BackupInspection> {
        return BackupSerializer.decodeAndValidate(rawJson).map { doc ->
            BackupInspection(
                document = doc,
                preview = BackupSerializer.preview(doc),
            )
        }
    }

    override suspend fun restoreBackup(
        document: HimmaBackupDocument,
        mode: RestoreMode,
    ): Result<RestoreSummary> = runCatching {
        exerciseRepository.ensureSeeded()

        val summary = database.withTransaction {
            when (mode) {
                RestoreMode.REPLACE -> applyReplaceRestore(document)
                RestoreMode.MERGE -> applyMergeRestore(document)
            }
        }

        if (mode == RestoreMode.REPLACE) {
            document.preferences?.let { prefs ->
                preferencesRepository.restorePreferences(
                    language = AppLanguage.fromTag(prefs.languageTag),
                    weightUnit = WeightUnit.fromCode(prefs.weightUnit),
                    defaultRestSeconds = prefs.defaultRestSeconds,
                    themeMode = ThemeMode.fromKey(prefs.themeMode),
                    useDynamicColor = prefs.useDynamicColor,
                )
            }
        }

        summary
    }

    override suspend fun createLocalAutoSnapshot(): Result<Long> = runCatching {
        val payload = createBackupJson()
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            if (!autoBackupDir.exists()) {
                autoBackupDir.mkdirs()
            }
            val tempFile = File(autoBackupDir, "$LATEST_SNAPSHOT_FILE_NAME.tmp")
            tempFile.writeText(payload, Charsets.UTF_8)
            if (!tempFile.renameTo(latestAutoSnapshotFile)) {
                latestAutoSnapshotFile.writeText(payload, Charsets.UTF_8)
                tempFile.delete()
            }
        }
        preferencesRepository.setLastAutoBackupAt(now)
        now
    }

    override fun hasLocalAutoSnapshot(): Boolean = latestAutoSnapshotFile.exists()

    private suspend fun applyReplaceRestore(document: HimmaBackupDocument): RestoreSummary {
        analyticsDao.deleteAllPersonalRecords()
        analyticsDao.deleteAllBodyWeights()
        workoutDao.deleteAllWorkouts()
        routineDao.deleteAllRoutines()
        exerciseDao.deleteAllCustomExercises()
        userProfileDao.deleteProfile()

        val customEntities = document.customExercises.map { it.toEntity() }
        if (customEntities.isNotEmpty()) {
            exerciseDao.upsertAll(customEntities)
        }

        val validExerciseIds = exerciseDao.getAllExerciseIdsOnce().toSet()

        val routineEntities = document.routines.map { it.toEntity() }
        if (routineEntities.isNotEmpty()) {
            routineDao.upsertRoutines(routineEntities)
        }

        val (validRoutineExercises, skippedRoutineEx) = document.routineExercises
            .partition { it.exerciseId in validExerciseIds }
        if (validRoutineExercises.isNotEmpty()) {
            routineDao.insertRoutineExercises(validRoutineExercises.map { it.toEntity() })
        }

        val restoredRoutineIds = routineEntities.map { it.id }.toSet()
        var draftSeen = false
        val workoutEntities = document.workouts.mapNotNull { workout ->
            if (!workout.isCompleted) {
                if (draftSeen) return@mapNotNull null
                draftSeen = true
            }
            workout.toEntity(restoredRoutineIds)
        }
        val acceptedWorkoutIds = workoutEntities.map { it.id }.toSet()
        if (workoutEntities.isNotEmpty()) {
            workoutDao.upsertWorkouts(workoutEntities)
        }

        val candidateSets = document.workoutSets.filter { it.workoutId in acceptedWorkoutIds }
        val (validSets, skippedWorkoutSets) = candidateSets
            .partition { it.exerciseId in validExerciseIds }
        if (validSets.isNotEmpty()) {
            workoutDao.insertSets(validSets.map { it.toEntity() })
        }

        document.profile?.let { profile ->
            userProfileDao.upsertProfile(
                UserProfileEntity(
                    id = UserProfileEntity.SINGLETON_PROFILE_ID,
                    displayName = profile.displayName,
                    bodyWeightKg = profile.bodyWeightKg,
                    createdAt = profile.createdAt,
                    updatedAt = profile.updatedAt,
                ),
            )
        }

        val validPrs = document.personalRecords
            .filter { it.exerciseId in validExerciseIds }
            .map { pr ->
                PersonalRecordEntity(
                    id = pr.id,
                    exerciseId = pr.exerciseId,
                    maxWeightKg = pr.maxWeightKg,
                    maxVolumeSetKg = pr.maxVolumeSetKg,
                    estimated1rmKg = pr.estimated1rmKg,
                    achievedAt = pr.achievedAt,
                    workoutId = pr.workoutId?.takeIf { it in acceptedWorkoutIds },
                )
            }
        if (validPrs.isNotEmpty()) {
            analyticsDao.upsertPersonalRecords(validPrs)
        }

        val bodyWeights = document.bodyWeights.map { bw ->
            BodyWeightEntity(
                id = bw.id,
                weightKg = bw.weightKg,
                recordedAt = bw.recordedAt,
            )
        }
        if (bodyWeights.isNotEmpty()) {
            analyticsDao.insertAllBodyWeights(bodyWeights)
        }

        return RestoreSummary(
            mode = RestoreMode.REPLACE,
            restoredCustomExercises = customEntities.size,
            restoredRoutines = routineEntities.count { !it.isDeleted },
            restoredWorkouts = workoutEntities.size,
            restoredSets = validSets.size,
            skippedMissingExercises = skippedRoutineEx.size + skippedWorkoutSets.size,
        )
    }

    private suspend fun applyMergeRestore(document: HimmaBackupDocument): RestoreSummary {
        document.profile?.let { incoming ->
            val current = userProfileDao.getProfileOnce()
            if (current == null || incoming.updatedAt > current.updatedAt) {
                userProfileDao.upsertProfile(
                    UserProfileEntity(
                        id = UserProfileEntity.SINGLETON_PROFILE_ID,
                        displayName = incoming.displayName,
                        bodyWeightKg = incoming.bodyWeightKg,
                        createdAt = current?.createdAt ?: incoming.createdAt,
                        updatedAt = incoming.updatedAt,
                    ),
                )
            }
        }

        val existingCustomById = exerciseDao.getCustomExercisesOnce().associateBy { it.id }
        val customToUpsert = document.customExercises
            .filter { incoming ->
                val current = existingCustomById[incoming.id]
                current == null || incoming.updatedAt > current.updatedAt
            }
            .map { it.toEntity() }
        if (customToUpsert.isNotEmpty()) {
            exerciseDao.upsertAll(customToUpsert)
        }

        val validExerciseIds = exerciseDao.getAllExerciseIdsOnce().toSet()

        val existingRoutinesById = routineDao.getAllRoutinesOnce().associateBy { it.id }
        val routinesToMerge = document.routines.filter { incoming ->
            val current = existingRoutinesById[incoming.id]
            current == null || incoming.updatedAt > current.updatedAt
        }
        val itemsByRoutine = document.routineExercises.groupBy { it.routineId }
        var skippedCount = 0

        routinesToMerge.forEach { routine ->
            val allItems = itemsByRoutine[routine.id].orEmpty()
            val (validItems, invalidItems) = allItems.partition { it.exerciseId in validExerciseIds }
            skippedCount += invalidItems.size
            routineDao.saveRoutineWithExercises(
                routine = routine.toEntity(),
                exercises = validItems.map { it.toEntity() },
            )
        }

        val allRoutineIds = routineDao.getAllRoutinesOnce().map { it.id }.toSet()
        val existingWorkouts = workoutDao.getAllWorkoutsOnce()
        val existingWorkoutIds = existingWorkouts.map { it.id }.toSet()
        var hasActiveDraft = existingWorkouts.any { !it.isCompleted }

        val workoutsToInsert = document.workouts.mapNotNull { incoming ->
            if (incoming.id in existingWorkoutIds) return@mapNotNull null
            if (!incoming.isCompleted) {
                if (hasActiveDraft) return@mapNotNull null
                hasActiveDraft = true
            }
            incoming.toEntity(allRoutineIds)
        }
        val acceptedWorkoutIds = workoutsToInsert.map { it.id }.toSet()
        if (workoutsToInsert.isNotEmpty()) {
            workoutDao.upsertWorkouts(workoutsToInsert)
        }

        val setsToInsert = document.workoutSets.filter { it.workoutId in acceptedWorkoutIds }
        val (validSets, invalidSets) = setsToInsert.partition { it.exerciseId in validExerciseIds }
        skippedCount += invalidSets.size
        if (validSets.isNotEmpty()) {
            workoutDao.insertSets(validSets.map { it.toEntity() })
        }

        val allWorkoutIds = (existingWorkoutIds + acceptedWorkoutIds)
        val existingPrByExercise = analyticsDao.getAllPersonalRecordsOnce().associateBy { it.exerciseId }
        val prsToUpsert = document.personalRecords
            .filter { it.exerciseId in validExerciseIds }
            .mapNotNull { incoming ->
                val current = existingPrByExercise[incoming.exerciseId]
                val incomingBetter = current == null ||
                    (incoming.maxWeightKg ?: 0.0) > (current.maxWeightKg ?: 0.0) ||
                    (incoming.estimated1rmKg ?: 0.0) > (current.estimated1rmKg ?: 0.0)
                if (!incomingBetter) return@mapNotNull null
                PersonalRecordEntity(
                    id = current?.id ?: incoming.id,
                    exerciseId = incoming.exerciseId,
                    maxWeightKg = maxOf(incoming.maxWeightKg ?: 0.0, current?.maxWeightKg ?: 0.0),
                    maxVolumeSetKg = maxOf(incoming.maxVolumeSetKg ?: 0.0, current?.maxVolumeSetKg ?: 0.0),
                    estimated1rmKg = maxOf(incoming.estimated1rmKg ?: 0.0, current?.estimated1rmKg ?: 0.0),
                    achievedAt = incoming.achievedAt,
                    workoutId = incoming.workoutId?.takeIf { it in allWorkoutIds } ?: current?.workoutId,
                )
            }
        if (prsToUpsert.isNotEmpty()) {
            analyticsDao.upsertPersonalRecords(prsToUpsert)
        }

        val existingBwIds = analyticsDao.getAllBodyWeightsOnce().map { it.id }.toSet()
        val bwToInsert = document.bodyWeights
            .filter { it.id !in existingBwIds }
            .map { BodyWeightEntity(id = it.id, weightKg = it.weightKg, recordedAt = it.recordedAt) }
        if (bwToInsert.isNotEmpty()) {
            analyticsDao.insertAllBodyWeights(bwToInsert)
        }

        return RestoreSummary(
            mode = RestoreMode.MERGE,
            restoredCustomExercises = customToUpsert.size,
            restoredRoutines = routinesToMerge.count { !it.isDeleted },
            restoredWorkouts = workoutsToInsert.size,
            restoredSets = validSets.size,
            skippedMissingExercises = skippedCount,
        )
    }

    private suspend fun writeTextToUri(uri: Uri, content: String) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Unable to open output stream for $uri")
        output.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    private suspend fun readTextFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input stream for $uri")
        input.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }

    private fun ExerciseEntity.toBackupDto(): BackupExercise = BackupExercise(
        id = id,
        nameEn = nameEn,
        nameAr = nameAr,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles
            .split(",")
            .mapNotNull { MuscleGroup.fromNameOrNull(it)?.name },
        equipment = equipment,
        instructionsEn = instructionsEn,
        instructionsAr = instructionsAr,
        localMediaKey = localMediaKey,
        defaultRestSeconds = defaultRestSeconds,
        isCustom = isCustom,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun BackupExercise.toEntity(): ExerciseEntity {
        val normalizedPrimary = MuscleGroup.fromNameOrNull(primaryMuscle)?.name ?: MuscleGroup.CHEST.name
        val normalizedSecondary = secondaryMuscles
            .mapNotNull { MuscleGroup.fromNameOrNull(it)?.name }
            .distinct()
            .joinToString(",")
        val normalizedEquipment = EquipmentType.fromNameOrNull(equipment)?.name ?: EquipmentType.BODYWEIGHT.name
        return ExerciseEntity(
            id = id,
            nameEn = nameEn,
            nameAr = nameAr,
            nameArNormalized = ArabicTextNormalizer.normalize(nameAr),
            primaryMuscle = normalizedPrimary,
            secondaryMuscles = normalizedSecondary,
            equipment = normalizedEquipment,
            instructionsEn = instructionsEn,
            instructionsAr = instructionsAr,
            localMediaKey = localMediaKey,
            defaultRestSeconds = defaultRestSeconds.coerceIn(15, 600),
            isCustom = true,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun BackupRoutine.toEntity(): RoutineEntity = RoutineEntity(
        id = id,
        title = title,
        notes = notes,
        orderIndex = orderIndex,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    private fun BackupRoutineExercise.toEntity(): RoutineExerciseEntity = RoutineExerciseEntity(
        id = id,
        routineId = routineId,
        exerciseId = exerciseId,
        orderIndex = orderIndex,
        targetSets = targetSets.coerceAtLeast(1),
        targetRepsMin = targetRepsMin.coerceAtLeast(1),
        targetRepsMax = targetRepsMax.coerceAtLeast(targetRepsMin.coerceAtLeast(1)),
        targetRestSeconds = targetRestSeconds.coerceIn(15, 600),
    )

    private fun BackupWorkout.toEntity(validRoutineIds: Set<String>): WorkoutEntity = WorkoutEntity(
        id = id,
        routineId = routineId?.takeIf { it in validRoutineIds },
        title = title,
        startTime = startTime,
        endTime = endTime,
        durationSeconds = durationSeconds.coerceAtLeast(0),
        totalVolumeKg = totalVolumeKg.coerceAtLeast(0.0),
        notes = notes,
        isCompleted = isCompleted,
    )

    private fun BackupWorkoutSet.toEntity(): WorkoutSetEntity = WorkoutSetEntity(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        exerciseOrder = exerciseOrder.coerceAtLeast(0),
        setIndex = setIndex.coerceAtLeast(0),
        restSeconds = restSeconds.coerceIn(15, 600),
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        setType = SetType.fromNameOrDefault(setType).name,
        isCompleted = isCompleted,
        isPr = isPr,
        completedAt = completedAt,
    )

    companion object {
        private const val AUTO_BACKUP_DIR_NAME = "auto_backups"
        private const val LATEST_SNAPSHOT_FILE_NAME = "himma-auto-latest.himma.json"
    }
}
