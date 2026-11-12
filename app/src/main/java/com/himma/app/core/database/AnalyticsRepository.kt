package com.himma.app.core.database

import com.himma.app.core.database.dao.AnalyticsDao
import com.himma.app.core.database.dao.ExerciseDao
import com.himma.app.core.database.dao.WorkoutDao
import com.himma.app.core.database.entity.BodyWeightEntity
import com.himma.app.core.database.entity.PersonalRecordEntity
import com.himma.app.domain.model.BodyWeightEntry
import com.himma.app.domain.model.BrzyckiMath
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.MuscleVolumeDistribution
import com.himma.app.domain.model.PersonalRecord
import com.himma.app.domain.model.ProgressionPoint
import com.himma.app.domain.model.ProgressiveInsight
import com.himma.app.domain.model.WorkoutHistorySummary
import com.himma.app.domain.model.WorkoutSession
import com.himma.app.domain.model.WorkoutSet
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface AnalyticsRepository {
    fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>>

    fun observeWorkoutDetail(workoutId: String): Flow<WorkoutSession?>

    fun observeAllPersonalRecords(): Flow<List<PersonalRecord>>

    fun observeMuscleVolumeDistribution(days: Int): Flow<List<MuscleVolumeDistribution>>

    fun observeExercise1RmProgression(exerciseId: String): Flow<List<ProgressionPoint>>

    fun observeExerciseWeightProgression(exerciseId: String): Flow<List<ProgressionPoint>>

    fun observeBodyWeightTrend(days: Int): Flow<List<BodyWeightEntry>>

    fun observeInsights(): Flow<List<ProgressiveInsight>>

    suspend fun logBodyWeight(weightKg: Double, recordedAt: Long = System.currentTimeMillis())

    suspend fun deleteBodyWeight(id: String)

    suspend fun evaluateAndPersistWorkoutPrs(workoutId: String): Int
}

@Singleton
class RoomAnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
) : AnalyticsRepository {

    override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> {
        return analyticsDao.observeWorkoutHistorySummaries()
    }

    override fun observeWorkoutDetail(workoutId: String): Flow<WorkoutSession?> {
        return workoutDao.observeWorkoutById(workoutId).map { it?.toDomain() }
    }

    override fun observeAllPersonalRecords(): Flow<List<PersonalRecord>> {
        return analyticsDao.observeAllPersonalRecordsWithExercise().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeMuscleVolumeDistribution(days: Int): Flow<List<MuscleVolumeDistribution>> {
        val since = if (days <= 0) 0L else System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return analyticsDao.observeMuscleVolumeDistribution(since).map { rows ->
            val totalSets = rows.sumOf { it.completedSetsCount }.coerceAtLeast(1)
            val byGroup = rows.mapNotNull { row ->
                val group = MuscleGroup.fromNameOrNull(row.primaryMuscle) ?: return@mapNotNull null
                MuscleVolumeDistribution(
                    muscleGroup = group,
                    completedSets = row.completedSetsCount,
                    totalVolumeKg = row.totalVolumeKg,
                    percentageOfSets = (row.completedSetsCount.toDouble() / totalSets) * 100.0,
                )
            }
            byGroup.sortedByDescending { it.completedSets }
        }
    }

    override fun observeExercise1RmProgression(exerciseId: String): Flow<List<ProgressionPoint>> {
        return analyticsDao.observeExerciseProgressionSets(exerciseId).map { rows ->
            // Group by workout start time and pick max estimated 1RM per workout session
            rows.groupBy { it.startTime }
                .entries
                .sortedBy { it.key }
                .mapNotNull { (startTime, sessionSets) ->
                    val bestSet = sessionSets.maxByOrNull {
                        BrzyckiMath.calculateEstimated1Rm(it.weightKg, it.reps)
                    } ?: return@mapNotNull null
                    val value = BrzyckiMath.calculateEstimated1Rm(bestSet.weightKg, bestSet.reps)
                    if (value <= 0.0) return@mapNotNull null
                    ProgressionPoint(
                        timestamp = startTime,
                        value = value,
                        reps = bestSet.reps,
                        workoutTitle = bestSet.workoutTitle,
                    )
                }
        }
    }

    override fun observeExerciseWeightProgression(exerciseId: String): Flow<List<ProgressionPoint>> {
        return analyticsDao.observeExerciseProgressionSets(exerciseId).map { rows ->
            rows.groupBy { it.startTime }
                .entries
                .sortedBy { it.key }
                .mapNotNull { (startTime, sessionSets) ->
                    val bestSet = sessionSets.maxByOrNull { it.weightKg ?: 0.0 } ?: return@mapNotNull null
                    val weight = bestSet.weightKg ?: return@mapNotNull null
                    if (weight <= 0.0) return@mapNotNull null
                    ProgressionPoint(
                        timestamp = startTime,
                        value = weight,
                        reps = bestSet.reps,
                        workoutTitle = bestSet.workoutTitle,
                    )
                }
        }
    }

    override fun observeBodyWeightTrend(days: Int): Flow<List<BodyWeightEntry>> {
        val since = if (days <= 0) 0L else System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return analyticsDao.observeBodyWeights(since).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeInsights(): Flow<List<ProgressiveInsight>> {
        val weeklyDistributionFlow = observeMuscleVolumeDistribution(days = 7)
        val prsFlow = observeAllPersonalRecords()
        val historyFlow = observeWorkoutHistory()

        return combine(weeklyDistributionFlow, prsFlow, historyFlow) { dists, prs, history ->
            val topExercisesMap = mutableMapOf<Exercise, List<WorkoutSet>>()
            // Sample top 2 recent PR exercises for progressive overload suggestions
            prs.take(2).forEach { pr ->
                val exEntity = exerciseDao.getByIdOnce(pr.exerciseId) ?: return@forEach
                val recentSets = workoutDao.getRecentCompletedSetsForExercise(pr.exerciseId)
                    .map { it.toDomain() }
                topExercisesMap[exEntity.toDomain()] = recentSets
            }
            if (history.isEmpty() && prs.isEmpty()) {
                emptyList()
            } else {
                BrzyckiMath.generateInsights(
                    distributions = dists,
                    recentPrs = prs,
                    topExercisesWithSets = topExercisesMap,
                )
            }
        }
    }

    override suspend fun logBodyWeight(weightKg: Double, recordedAt: Long) {
        withContext(Dispatchers.IO) {
            if (weightKg <= 0.0) return@withContext
            analyticsDao.insertBodyWeight(
                BodyWeightEntity(
                    id = UUID.randomUUID().toString(),
                    weightKg = weightKg,
                    recordedAt = recordedAt,
                ),
            )
        }
    }

    override suspend fun deleteBodyWeight(id: String) {
        withContext(Dispatchers.IO) {
            analyticsDao.deleteBodyWeightById(id)
        }
    }

    override suspend fun evaluateAndPersistWorkoutPrs(workoutId: String): Int =
        withContext(Dispatchers.IO) {
            val relation = workoutDao.getWorkoutByIdOnce(workoutId) ?: return@withContext 0
            val completedSets = relation.sets
                .map { it.set }
                .filter { it.isCompleted && (it.weightKg ?: 0.0) > 0.0 && (it.reps ?: 0) > 0 }
                .sortedWith(compareBy({ it.exerciseOrder }, { it.setIndex }))

            if (completedSets.isEmpty()) return@withContext 0

            var totalNewPrSets = 0
            val setsByExercise = completedSets.groupBy { it.exerciseId }

            setsByExercise.forEach { (exerciseId, exerciseSets) ->
                val existingEntity = analyticsDao.getPersonalRecordForExercise(exerciseId)
                var currentRecord = existingEntity?.let {
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

                var updatedInWorkout = false
                exerciseSets.forEach { setEntity ->
                    val w = setEntity.weightKg ?: return@forEach
                    val r = setEntity.reps ?: return@forEach
                    val brokePr = BrzyckiMath.isSetPersonalRecord(
                        weightKg = w,
                        reps = r,
                        existingPr = currentRecord,
                    )
                    if (brokePr) {
                        totalNewPrSets++
                        updatedInWorkout = true
                        if (!setEntity.isPr) {
                            workoutDao.upsertSet(setEntity.copy(isPr = true))
                        }
                        currentRecord = BrzyckiMath.updateRecord(
                            existing = currentRecord,
                            exerciseId = exerciseId,
                            weightKg = w,
                            reps = r,
                            achievedAt = setEntity.completedAt ?: System.currentTimeMillis(),
                            workoutId = workoutId,
                        )
                    }
                }

                if (updatedInWorkout && currentRecord != null) {
                    val rec = currentRecord!!
                    analyticsDao.upsertPersonalRecord(
                        PersonalRecordEntity(
                            id = rec.id,
                            exerciseId = rec.exerciseId,
                            maxWeightKg = rec.maxWeightKg,
                            maxVolumeSetKg = rec.maxVolumeSetKg,
                            estimated1rmKg = rec.estimated1rmKg,
                            achievedAt = rec.achievedAt,
                            workoutId = rec.workoutId,
                        ),
                    )
                }
            }

            totalNewPrSets
        }
}
