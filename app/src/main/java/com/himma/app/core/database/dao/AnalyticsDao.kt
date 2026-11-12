package com.himma.app.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.himma.app.core.database.entity.BodyWeightEntity
import com.himma.app.core.database.entity.ExerciseEntity
import com.himma.app.core.database.entity.PersonalRecordEntity
import com.himma.app.domain.model.BodyWeightEntry
import com.himma.app.domain.model.PersonalRecord
import com.himma.app.domain.model.WorkoutHistorySummary
import kotlinx.coroutines.flow.Flow

data class PersonalRecordWithExercise(
    @Embedded
    val pr: PersonalRecordEntity,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id",
    )
    val exercise: ExerciseEntity?,
) {
    fun toDomain(): PersonalRecord = PersonalRecord(
        id = pr.id,
        exerciseId = pr.exerciseId,
        exerciseNameAr = exercise?.nameAr.orEmpty(),
        exerciseNameEn = exercise?.nameEn.orEmpty(),
        maxWeightKg = pr.maxWeightKg,
        maxVolumeSetKg = pr.maxVolumeSetKg,
        estimated1rmKg = pr.estimated1rmKg,
        achievedAt = pr.achievedAt,
        workoutId = pr.workoutId,
    )
}

data class MuscleGroupStatRow(
    val primaryMuscle: String,
    val completedSetsCount: Int,
    val totalVolumeKg: Double,
)

data class ProgressionSetRow(
    val startTime: Long,
    val workoutTitle: String,
    val weightKg: Double?,
    val reps: Int?,
    val isPr: Boolean,
)

@Dao
interface AnalyticsDao {

    @Transaction
    @Query(
        """
        SELECT pr.* FROM personal_records pr
        INNER JOIN exercises e ON e.id = pr.exercise_id
        ORDER BY pr.achieved_at DESC
        """
    )
    fun observeAllPersonalRecordsWithExercise(): Flow<List<PersonalRecordWithExercise>>

    @Query("SELECT * FROM personal_records WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun getPersonalRecordForExercise(exerciseId: String): PersonalRecordEntity?

    @Query("SELECT * FROM personal_records")
    suspend fun getAllPersonalRecordsOnce(): List<PersonalRecordEntity>

    @Upsert
    suspend fun upsertPersonalRecord(pr: PersonalRecordEntity)

    @Upsert
    suspend fun upsertPersonalRecords(prs: List<PersonalRecordEntity>)

    @Query("DELETE FROM personal_records")
    suspend fun deleteAllPersonalRecords()

    @Query(
        """
        SELECT 
            w.id AS id,
            w.routine_id AS routineId,
            w.title AS title,
            w.start_time AS startTime,
            w.end_time AS endTime,
            w.duration_seconds AS durationSeconds,
            w.total_volume_kg AS totalVolumeKg,
            (SELECT COUNT(*) FROM workout_sets ws WHERE ws.workout_id = w.id AND ws.is_completed = 1) AS completedSetsCount,
            (SELECT COUNT(*) FROM workout_sets ws WHERE ws.workout_id = w.id AND ws.is_pr = 1) AS prCount
        FROM workouts w
        WHERE w.is_completed = 1
        ORDER BY w.start_time DESC
        """
    )
    fun observeWorkoutHistorySummaries(): Flow<List<WorkoutHistorySummary>>

    @Query(
        """
        SELECT 
            e.primary_muscle AS primaryMuscle,
            COUNT(ws.id) AS completedSetsCount,
            COALESCE(SUM(ws.weight_kg * ws.reps), 0.0) AS totalVolumeKg
        FROM workout_sets ws
        INNER JOIN workouts w ON w.id = ws.workout_id
        INNER JOIN exercises e ON e.id = ws.exercise_id
        WHERE w.is_completed = 1
          AND ws.is_completed = 1
          AND w.start_time >= :sinceTimestamp
        GROUP BY e.primary_muscle
        """
    )
    fun observeMuscleVolumeDistribution(sinceTimestamp: Long): Flow<List<MuscleGroupStatRow>>

    @Query(
        """
        SELECT 
            w.start_time AS startTime,
            w.title AS workoutTitle,
            ws.weight_kg AS weightKg,
            ws.reps AS reps,
            ws.is_pr AS isPr
        FROM workout_sets ws
        INNER JOIN workouts w ON w.id = ws.workout_id
        WHERE w.is_completed = 1
          AND ws.is_completed = 1
          AND ws.exercise_id = :exerciseId
          AND ws.weight_kg IS NOT NULL
          AND ws.reps IS NOT NULL
        ORDER BY w.start_time ASC, ws.exercise_order ASC, ws.set_index ASC
        """
    )
    fun observeExerciseProgressionSets(exerciseId: String): Flow<List<ProgressionSetRow>>

    // Body Weight Queries
    @Query("SELECT * FROM body_weights WHERE recorded_at >= :sinceTimestamp ORDER BY recorded_at ASC")
    fun observeBodyWeights(sinceTimestamp: Long): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weights ORDER BY recorded_at DESC LIMIT 1")
    suspend fun getLatestBodyWeight(): BodyWeightEntity?

    @Query("SELECT * FROM body_weights ORDER BY recorded_at ASC")
    suspend fun getAllBodyWeightsOnce(): List<BodyWeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeight(entry: BodyWeightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBodyWeights(entries: List<BodyWeightEntity>)

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun deleteBodyWeightById(id: String)

    @Query("DELETE FROM body_weights")
    suspend fun deleteAllBodyWeights()
}
