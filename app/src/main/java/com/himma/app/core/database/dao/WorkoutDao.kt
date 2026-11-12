package com.himma.app.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.himma.app.core.database.entity.ExerciseEntity
import com.himma.app.core.database.entity.WorkoutEntity
import com.himma.app.core.database.entity.WorkoutSetEntity
import com.himma.app.domain.model.WorkoutExerciseGroup
import com.himma.app.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

data class WorkoutSetWithExercise(
    @Embedded
    val set: WorkoutSetEntity,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id",
    )
    val exercise: ExerciseEntity?,
)

data class WorkoutWithSets(
    @Embedded
    val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutSetEntity::class,
        parentColumn = "id",
        entityColumn = "workout_id",
    )
    val sets: List<WorkoutSetWithExercise>,
) {
    fun toDomain(): WorkoutSession {
        val grouped = sets
            .filter { it.exercise != null }
            .groupBy { it.set.exerciseId to it.set.exerciseOrder }
            .entries
            .sortedBy { it.key.second }
            .map { (_, rows) ->
                val orderedRows = rows.sortedBy { it.set.setIndex }
                val first = orderedRows.first()
                WorkoutExerciseGroup(
                    exercise = first.exercise!!.toDomain(),
                    exerciseOrder = first.set.exerciseOrder,
                    restSeconds = first.set.restSeconds,
                    sets = orderedRows.map { it.set.toDomain() },
                )
            }

        return WorkoutSession(
            id = workout.id,
            routineId = workout.routineId,
            title = workout.title,
            startTime = workout.startTime,
            endTime = workout.endTime,
            durationSeconds = workout.durationSeconds,
            totalVolumeKg = workout.totalVolumeKg,
            notes = workout.notes,
            isCompleted = workout.isCompleted,
            exercises = grouped,
        )
    }
}

@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workouts WHERE is_completed = 0 ORDER BY start_time DESC LIMIT 1")
    fun observeActiveDraftWorkout(): Flow<WorkoutWithSets?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE is_completed = 0 ORDER BY start_time DESC LIMIT 1")
    suspend fun getActiveDraftWorkoutOnce(): WorkoutWithSets?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    fun observeWorkoutById(workoutId: String): Flow<WorkoutWithSets?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkoutByIdOnce(workoutId: String): WorkoutWithSets?

    @Query("SELECT * FROM workout_sets WHERE id = :setId LIMIT 1")
    suspend fun getSetByIdOnce(setId: String): WorkoutSetEntity?

    @Query("SELECT * FROM workouts ORDER BY start_time ASC")
    suspend fun getAllWorkoutsOnce(): List<WorkoutEntity>

    @Query("SELECT * FROM workout_sets ORDER BY workout_id ASC, exercise_order ASC, set_index ASC")
    suspend fun getAllWorkoutSetsOnce(): List<WorkoutSetEntity>

    @Transaction
    @Query("SELECT * FROM workouts WHERE is_completed = 1 ORDER BY start_time DESC")
    suspend fun getCompletedWorkoutsWithSetsOnce(): List<WorkoutWithSets>

    @Upsert
    suspend fun upsertWorkouts(workouts: List<WorkoutEntity>)

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Query("SELECT COALESCE(MAX(exercise_order), -1) FROM workout_sets WHERE workout_id = :workoutId")
    suspend fun getMaxExerciseOrder(workoutId: String): Int

    @Query("SELECT COALESCE(MAX(set_index), -1) FROM workout_sets WHERE workout_id = :workoutId AND exercise_id = :exerciseId")
    suspend fun getMaxSetIndexForExercise(workoutId: String, exerciseId: String): Int

    @Query(
        """
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workouts w ON w.id = ws.workout_id
        WHERE w.is_completed = 1
          AND ws.exercise_id = :exerciseId
          AND ws.set_index = :setIndex
          AND ws.is_completed = 1
        ORDER BY w.start_time DESC, ws.completed_at DESC
        LIMIT 1
        """
    )
    suspend fun getPreviousCompletedSet(
        exerciseId: String,
        setIndex: Int,
    ): WorkoutSetEntity?

    @Query(
        """
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workouts w ON w.id = ws.workout_id
        WHERE w.is_completed = 1
          AND ws.exercise_id = :exerciseId
          AND ws.is_completed = 1
        ORDER BY w.start_time DESC, ws.set_index ASC
        LIMIT 20
        """
    )
    suspend fun getRecentCompletedSetsForExercise(exerciseId: String): List<WorkoutSetEntity>

    @Upsert
    suspend fun upsertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Upsert
    suspend fun upsertSet(set: WorkoutSetEntity)

    @Query("UPDATE workouts SET notes = :notes WHERE id = :workoutId")
    suspend fun updateNotes(workoutId: String, notes: String)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSetById(setId: String)

    @Query("DELETE FROM workout_sets WHERE workout_id = :workoutId AND exercise_id = :exerciseId")
    suspend fun deleteSetsForExercise(workoutId: String, exerciseId: String)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: String)

    @Transaction
    suspend fun createWorkoutWithSets(
        workout: WorkoutEntity,
        sets: List<WorkoutSetEntity>,
    ) {
        upsertWorkout(workout)
        if (sets.isNotEmpty()) {
            insertSets(sets)
        }
    }
}
