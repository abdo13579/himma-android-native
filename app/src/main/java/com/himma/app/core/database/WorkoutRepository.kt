package com.himma.app.core.database

import com.himma.app.core.database.dao.AnalyticsDao
import com.himma.app.core.database.dao.RoutineDao
import com.himma.app.core.database.dao.WorkoutDao
import com.himma.app.core.database.entity.WorkoutEntity
import com.himma.app.core.database.entity.WorkoutSetEntity
import com.himma.app.domain.model.BrzyckiMath
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.FinishedWorkoutSummary
import com.himma.app.domain.model.PersonalRecord
import com.himma.app.domain.model.PreviousSetHint
import com.himma.app.domain.model.SetType
import com.himma.app.domain.model.WorkoutMath
import com.himma.app.domain.model.WorkoutSession
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class SetCompletionResult(
    val isCompleted: Boolean,
    val shouldStartTimer: Boolean,
    val restSeconds: Int,
    val isPr: Boolean = false,
)

interface WorkoutRepository {
    fun observeActiveDraft(): Flow<WorkoutSession?>

    suspend fun getActiveDraftOnce(): WorkoutSession?

    fun observeWorkoutById(workoutId: String): Flow<WorkoutSession?>

    suspend fun startWorkoutFromRoutine(
        routineId: String,
        userDefaultRestSeconds: Int,
    ): String

    suspend fun startFreeWorkout(
        title: String,
    ): String

    suspend fun updateWorkoutNotes(workoutId: String, notes: String)

    suspend fun addExerciseToWorkout(
        workoutId: String,
        exercise: Exercise,
        targetSets: Int,
        restSeconds: Int,
    )

    suspend fun addSetToExercise(
        workoutId: String,
        exerciseId: String,
        exerciseOrder: Int,
        restSeconds: Int,
    )

    suspend fun removeSet(setId: String)

    suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String)

    suspend fun updateSetValues(
        setId: String,
        weightKg: Double?,
        reps: Int?,
        setType: SetType,
        rpe: Double? = null,
    )

    suspend fun toggleSetCompleted(setId: String): SetCompletionResult

    suspend fun getPreviousHintsForExercises(exerciseIds: List<String>): Map<Pair<String, Int>, PreviousSetHint>

    suspend fun finishWorkout(workoutId: String): FinishedWorkoutSummary?

    suspend fun discardWorkout(workoutId: String)
}

@Singleton
class RoomWorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val routineDao: RoutineDao,
    private val analyticsDao: AnalyticsDao,
    private val analyticsRepository: AnalyticsRepository,
) : WorkoutRepository {

    override fun observeActiveDraft(): Flow<WorkoutSession?> {
        return workoutDao.observeActiveDraftWorkout().map { it?.toDomain() }
    }

    override suspend fun getActiveDraftOnce(): WorkoutSession? = withContext(Dispatchers.IO) {
        workoutDao.getActiveDraftWorkoutOnce()?.toDomain()
    }

    override fun observeWorkoutById(workoutId: String): Flow<WorkoutSession?> {
        return workoutDao.observeWorkoutById(workoutId).map { it?.toDomain() }
    }

    override suspend fun startWorkoutFromRoutine(
        routineId: String,
        userDefaultRestSeconds: Int,
    ): String = withContext(Dispatchers.IO) {
        val routineRelation = routineDao.getRoutineWithExercisesOnce(routineId)
            ?: return@withContext startFreeWorkout("Workout")
        val routine = routineRelation.toDomain()
        val now = System.currentTimeMillis()
        val workoutId = UUID.randomUUID().toString()

        val workoutEntity = WorkoutEntity(
            id = workoutId,
            routineId = routine.id,
            title = routine.title,
            startTime = now,
            endTime = null,
            durationSeconds = 0,
            totalVolumeKg = 0.0,
            notes = routine.notes,
            isCompleted = false,
        )

        val setEntities = buildList {
            routine.exercises.forEachIndexed { exIndex, routineEx ->
                val resolvedRest = WorkoutMath.resolveRestSeconds(
                    routineRestSeconds = routineEx.targetRestSeconds,
                    exerciseDefaultRestSeconds = routineEx.exercise.defaultRestSeconds,
                    userDefaultRestSeconds = userDefaultRestSeconds,
                )
                val setsCount = routineEx.targetSets.coerceIn(1, 20)
                repeat(setsCount) { setIdx ->
                    add(
                        WorkoutSetEntity(
                            id = UUID.randomUUID().toString(),
                            workoutId = workoutId,
                            exerciseId = routineEx.exercise.id,
                            exerciseOrder = exIndex,
                            setIndex = setIdx,
                            restSeconds = resolvedRest,
                            weightKg = null,
                            reps = null,
                            rpe = null,
                            setType = SetType.NORMAL.name,
                            isCompleted = false,
                            isPr = false,
                            completedAt = null,
                        ),
                    )
                }
            }
        }

        workoutDao.createWorkoutWithSets(workoutEntity, setEntities)
        workoutId
    }

    override suspend fun startFreeWorkout(title: String): String = withContext(Dispatchers.IO) {
        val workoutId = UUID.randomUUID().toString()
        workoutDao.createWorkoutWithSets(
            workout = WorkoutEntity(
                id = workoutId,
                routineId = null,
                title = title.trim(),
                startTime = System.currentTimeMillis(),
                endTime = null,
                durationSeconds = 0,
                totalVolumeKg = 0.0,
                notes = "",
                isCompleted = false,
            ),
            sets = emptyList(),
        )
        workoutId
    }

    override suspend fun updateWorkoutNotes(workoutId: String, notes: String) {
        withContext(Dispatchers.IO) {
            workoutDao.updateNotes(workoutId, notes)
        }
    }

    override suspend fun addExerciseToWorkout(
        workoutId: String,
        exercise: Exercise,
        targetSets: Int,
        restSeconds: Int,
    ) {
        withContext(Dispatchers.IO) {
            val nextOrder = workoutDao.getMaxExerciseOrder(workoutId) + 1
            val sets = (0 until targetSets.coerceIn(1, 10)).map { setIdx ->
                WorkoutSetEntity(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    exerciseId = exercise.id,
                    exerciseOrder = nextOrder,
                    setIndex = setIdx,
                    restSeconds = restSeconds.coerceIn(15, 600),
                    weightKg = null,
                    reps = null,
                    rpe = null,
                    setType = SetType.NORMAL.name,
                    isCompleted = false,
                    isPr = false,
                    completedAt = null,
                )
            }
            workoutDao.insertSets(sets)
        }
    }

    override suspend fun addSetToExercise(
        workoutId: String,
        exerciseId: String,
        exerciseOrder: Int,
        restSeconds: Int,
    ) {
        withContext(Dispatchers.IO) {
            val nextSetIdx = workoutDao.getMaxSetIndexForExercise(workoutId, exerciseId) + 1
            workoutDao.upsertSet(
                WorkoutSetEntity(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    exerciseOrder = exerciseOrder,
                    setIndex = nextSetIdx,
                    restSeconds = restSeconds.coerceIn(15, 600),
                    weightKg = null,
                    reps = null,
                    rpe = null,
                    setType = SetType.NORMAL.name,
                    isCompleted = false,
                    isPr = false,
                    completedAt = null,
                ),
            )
        }
    }

    override suspend fun removeSet(setId: String) {
        withContext(Dispatchers.IO) {
            workoutDao.deleteSetById(setId)
        }
    }

    override suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        withContext(Dispatchers.IO) {
            workoutDao.deleteSetsForExercise(workoutId, exerciseId)
        }
    }

    override suspend fun updateSetValues(
        setId: String,
        weightKg: Double?,
        reps: Int?,
        setType: SetType,
        rpe: Double?,
    ) {
        withContext(Dispatchers.IO) {
            val existing = workoutDao.getSetByIdOnce(setId) ?: return@withContext
            workoutDao.upsertSet(
                existing.copy(
                    weightKg = weightKg,
                    reps = reps,
                    setType = setType.name,
                    rpe = rpe,
                ),
            )
        }
    }

    override suspend fun toggleSetCompleted(setId: String): SetCompletionResult =
        withContext(Dispatchers.IO) {
            val existing = workoutDao.getSetByIdOnce(setId)
                ?: return@withContext SetCompletionResult(false, false, 0)

            val nextCompleted = !existing.isCompleted
            var updatedWeight = existing.weightKg
            var updatedReps = existing.reps

            // Auto-fill from previous session's completed set at the same setIndex if blank.
            if (nextCompleted && (updatedWeight == null || updatedReps == null)) {
                val previous = workoutDao.getPreviousCompletedSet(
                    exerciseId = existing.exerciseId,
                    setIndex = existing.setIndex,
                )
                if (previous != null) {
                    if (updatedWeight == null) updatedWeight = previous.weightKg
                    if (updatedReps == null) updatedReps = previous.reps
                }
            }

            val isLivePr = if (nextCompleted) {
                val existingPr = analyticsDao.getPersonalRecordForExercise(existing.exerciseId)?.let {
                    PersonalRecord(
                        id = it.id,
                        exerciseId = it.exerciseId,
                        maxWeightKg = it.maxWeightKg,
                        maxVolumeSetKg = it.maxVolumeSetKg,
                        estimated1rmKg = it.estimated1rmKg,
                        achievedAt = it.achievedAt,
                        workoutId = it.workoutId,
                    )
                }
                BrzyckiMath.isSetPersonalRecord(
                    weightKg = updatedWeight,
                    reps = updatedReps,
                    existingPr = existingPr,
                )
            } else {
                false
            }

            workoutDao.upsertSet(
                existing.copy(
                    weightKg = updatedWeight,
                    reps = updatedReps,
                    isCompleted = nextCompleted,
                    isPr = isLivePr,
                    completedAt = if (nextCompleted) System.currentTimeMillis() else null,
                ),
            )

            SetCompletionResult(
                isCompleted = nextCompleted,
                shouldStartTimer = nextCompleted,
                restSeconds = existing.restSeconds,
                isPr = isLivePr,
            )
        }

    override suspend fun getPreviousHintsForExercises(
        exerciseIds: List<String>,
    ): Map<Pair<String, Int>, PreviousSetHint> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Pair<String, Int>, PreviousSetHint>()
        exerciseIds.distinct().forEach { exerciseId ->
            val recent = workoutDao.getRecentCompletedSetsForExercise(exerciseId)
            // Keep first occurrence (most recent completed workout) per setIndex
            recent.forEach { row ->
                val key = exerciseId to row.setIndex
                if (!result.containsKey(key)) {
                    result[key] = PreviousSetHint(
                        exerciseId = exerciseId,
                        setIndex = row.setIndex,
                        weightKg = row.weightKg,
                        reps = row.reps,
                    )
                }
            }
        }
        result
    }

    override suspend fun finishWorkout(workoutId: String): FinishedWorkoutSummary? =
        withContext(Dispatchers.IO) {
            val relation = workoutDao.getWorkoutByIdOnce(workoutId) ?: return@withContext null
            val domain = relation.toDomain()
            val allSets = domain.exercises.flatMap { it.sets }
            val endTime = System.currentTimeMillis()
            val durationSeconds = WorkoutMath.calculateDurationSeconds(domain.startTime, endTime)
            val totalVolumeKg = WorkoutMath.calculateTotalVolumeKg(allSets)

            workoutDao.upsertWorkout(
                relation.workout.copy(
                    endTime = endTime,
                    durationSeconds = durationSeconds,
                    totalVolumeKg = totalVolumeKg,
                    isCompleted = true,
                ),
            )

            val newPrCount = analyticsRepository.evaluateAndPersistWorkoutPrs(workoutId)

            FinishedWorkoutSummary(
                workoutId = domain.id,
                title = domain.title,
                durationSeconds = durationSeconds,
                totalVolumeKg = totalVolumeKg,
                completedSets = domain.completedSetsCount,
                newPrCount = newPrCount,
            )
        }

    override suspend fun discardWorkout(workoutId: String) {
        withContext(Dispatchers.IO) {
            workoutDao.deleteWorkoutById(workoutId)
        }
    }
}
