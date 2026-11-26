package com.himma.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.himma.app.core.database.entity.WeeklyScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyScheduleDao {

    @Query("SELECT * FROM weekly_schedule ORDER BY day_of_week ASC")
    fun observeAll(): Flow<List<WeeklyScheduleEntity>>

    @Query("SELECT * FROM weekly_schedule ORDER BY day_of_week ASC")
    suspend fun getAllOnce(): List<WeeklyScheduleEntity>

    @Upsert
    suspend fun upsert(entry: WeeklyScheduleEntity)

    @Upsert
    suspend fun upsertAll(entries: List<WeeklyScheduleEntity>)

    @Query("DELETE FROM weekly_schedule WHERE day_of_week = :dayOfWeek")
    suspend fun deleteByDay(dayOfWeek: Int)

    @Query("DELETE FROM weekly_schedule")
    suspend fun deleteAll()
}
