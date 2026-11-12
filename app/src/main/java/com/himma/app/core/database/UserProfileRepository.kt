package com.himma.app.core.database

import com.himma.app.core.database.dao.UserProfileDao
import com.himma.app.core.database.entity.UserProfileEntity
import com.himma.app.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface UserProfileRepository {
    val profile: Flow<UserProfile?>
    suspend fun saveProfile(displayName: String?, bodyWeightKg: Double?)
}

@Singleton
class RoomUserProfileRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
) : UserProfileRepository {

    override val profile: Flow<UserProfile?> = userProfileDao.observeProfile()
        .map { it?.toDomain() }

    override suspend fun saveProfile(displayName: String?, bodyWeightKg: Double?) {
        val now = System.currentTimeMillis()
        val existing = userProfileDao.getProfileOnce()
        val normalizedName = displayName?.trim()?.takeIf { it.isNotEmpty() }
        userProfileDao.upsertProfile(
            UserProfileEntity(
                id = UserProfileEntity.SINGLETON_PROFILE_ID,
                displayName = normalizedName,
                bodyWeightKg = bodyWeightKg,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }
}
