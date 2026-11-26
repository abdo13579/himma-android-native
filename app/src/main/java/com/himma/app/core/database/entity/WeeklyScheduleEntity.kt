package com.himma.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_schedule",
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
    ],
)
data class WeeklyScheduleEntity(
    @PrimaryKey
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,
    val status: String,
    @ColumnInfo(name = "routine_id")
    val routineId: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
