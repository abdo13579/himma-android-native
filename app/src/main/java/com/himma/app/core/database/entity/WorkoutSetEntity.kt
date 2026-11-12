package com.himma.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.himma.app.domain.model.SetType
import com.himma.app.domain.model.WorkoutSet

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["workout_id", "exercise_order", "set_index"]),
        Index(value = ["exercise_id", "set_index", "is_completed"]),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "workout_id")
    val workoutId: String,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,
    @ColumnInfo(name = "exercise_order")
    val exerciseOrder: Int,
    @ColumnInfo(name = "set_index")
    val setIndex: Int,
    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    @ColumnInfo(name = "set_type")
    val setType: String,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    @ColumnInfo(name = "is_pr")
    val isPr: Boolean,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
) {
    fun toDomain(): WorkoutSet = WorkoutSet(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        exerciseOrder = exerciseOrder,
        setIndex = setIndex,
        restSeconds = restSeconds,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        setType = SetType.fromNameOrDefault(setType),
        isCompleted = isCompleted,
        isPr = isPr,
        completedAt = completedAt,
    )
}
