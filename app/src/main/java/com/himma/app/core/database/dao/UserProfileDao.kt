package com.himma.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.himma.app.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: Int = UserProfileEntity.SINGLETON_PROFILE_ID): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileOnce(id: Int = UserProfileEntity.SINGLETON_PROFILE_ID): UserProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)
}
