package com.himma.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.himma.app.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = SINGLETON_PROFILE_ID,
    @ColumnInfo(name = "display_name")
    val displayName: String? = null,
    @ColumnInfo(name = "body_weight_kg")
    val bodyWeightKg: Double? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        displayName = displayName,
        bodyWeightKg = bodyWeightKg,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        const val SINGLETON_PROFILE_ID = 1
    }
}
