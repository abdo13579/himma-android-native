package com.himma.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routines",
    indices = [
        Index(value = ["is_deleted", "order_index"]),
    ],
)
data class RoutineEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val notes: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
)
