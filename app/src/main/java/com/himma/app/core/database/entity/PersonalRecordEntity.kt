package com.himma.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["exercise_id"], unique = true),
        Index(value = ["achieved_at"]),
        Index(value = ["workout_id"]),
    ],
)
data class PersonalRecordEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,
    @ColumnInfo(name = "max_weight_kg")
    val maxWeightKg: Double?,
    @ColumnInfo(name = "max_volume_set_kg")
    val maxVolumeSetKg: Double?,
    @ColumnInfo(name = "estimated_1rm_kg")
    val estimated1rmKg: Double?,
    @ColumnInfo(name = "achieved_at")
    val achievedAt: Long,
    @ColumnInfo(name = "workout_id")
    val workoutId: String?,
)
