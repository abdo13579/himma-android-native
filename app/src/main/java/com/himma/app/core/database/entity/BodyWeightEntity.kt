package com.himma.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.himma.app.domain.model.BodyWeightEntry

@Entity(
    tableName = "body_weights",
    indices = [
        Index(value = ["recorded_at"]),
    ],
)
data class BodyWeightEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long,
) {
    fun toDomain(): BodyWeightEntry = BodyWeightEntry(
        id = id,
        weightKg = weightKg,
        recordedAt = recordedAt,
    )
}
