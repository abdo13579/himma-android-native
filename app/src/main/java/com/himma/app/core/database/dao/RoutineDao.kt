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
import com.himma.app.core.database.entity.RoutineEntity
import com.himma.app.core.database.entity.RoutineExerciseEntity
import com.himma.app.domain.model.Routine
import com.himma.app.domain.model.RoutineExercise
import kotlinx.coroutines.flow.Flow

data class RoutineExerciseWithExercise(
    @Embedded
    val item: RoutineExerciseEntity,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id",
    )
    val exercise: ExerciseEntity?,
)

data class RoutineWithExercises(
    @Embedded
    val routine: RoutineEntity,
    @Relation(
        entity = RoutineExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "routine_id",
    )
    val exercises: List<RoutineExerciseWithExercise>,
) {
    fun toDomain(): Routine = Routine(
        id = routine.id,
        title = routine.title,
        notes = routine.notes,
        orderIndex = routine.orderIndex,
        createdAt = routine.createdAt,
        updatedAt = routine.updatedAt,
        isDeleted = routine.isDeleted,
        exercises = exercises
            .sortedBy { it.item.orderIndex }
            .mapNotNull { relation ->
                val ex = relation.exercise?.toDomain() ?: return@mapNotNull null
                RoutineExercise(
                    id = relation.item.id,
                    routineId = relation.item.routineId,
                    exercise = ex,
                    orderIndex = relation.item.orderIndex,
                    targetSets = relation.item.targetSets,
                    targetRepsMin = relation.item.targetRepsMin,
                    targetRepsMax = relation.item.targetRepsMax,
                    targetRestSeconds = relation.item.targetRestSeconds,
                )
            },
    )
}

@Dao
interface RoutineDao {

    @Transaction
    @Query("SELECT * FROM routines WHERE is_deleted = 0 ORDER BY order_index ASC, updated_at DESC")
    fun observeActiveRoutinesWithExercises(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")
    fun observeRoutineWithExercises(routineId: String): Flow<RoutineWithExercises?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")
    suspend fun getRoutineWithExercisesOnce(routineId: String): RoutineWithExercises?

    @Query("SELECT COALESCE(MAX(order_index), -1) FROM routines WHERE is_deleted = 0")
    suspend fun getMaxOrderIndex(): Int

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routine_id = :routineId")
    suspend fun deleteExercisesForRoutine(routineId: String)

    @Query("UPDATE routines SET is_deleted = :isDeleted, updated_at = :updatedAt WHERE id = :routineId")
    suspend fun setDeleted(routineId: String, isDeleted: Boolean, updatedAt: Long)

    @Transaction
    suspend fun saveRoutineWithExercises(
        routine: RoutineEntity,
        exercises: List<RoutineExerciseEntity>,
    ) {
        upsertRoutine(routine)
        deleteExercisesForRoutine(routine.id)
        if (exercises.isNotEmpty()) {
            insertRoutineExercises(exercises)
        }
    }
}
