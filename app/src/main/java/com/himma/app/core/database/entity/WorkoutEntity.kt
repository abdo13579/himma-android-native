package com.himma.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["routine_id"]),
        Index(value = ["is_completed", "start_time"]),
    ],
)
data class WorkoutEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "routine_id")
    val routineId: String?,
    val title: String,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long?,
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,
    @ColumnInfo(name = "total_volume_kg")
    val totalVolumeKg: Double,
    val notes: String,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
)
