package com.himma.app.core.database

import com.himma.app.core.database.dao.WeeklyScheduleDao
import com.himma.app.core.database.entity.WeeklyScheduleEntity
import com.himma.app.domain.model.ScheduleDayStatus
import com.himma.app.domain.model.WeekScheduleHelper
import com.himma.app.domain.model.WeeklyScheduleEntry
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface WeeklyScheduleRepository {
    fun observeWeeklySchedule(): Flow<Map<DayOfWeek, WeeklyScheduleEntry>>

    suspend fun setDayRoutine(dayOfWeek: DayOfWeek, routineId: String)

    suspend fun setDayRest(dayOfWeek: DayOfWeek)

    suspend fun clearDay(dayOfWeek: DayOfWeek)
}

@Singleton
class RoomWeeklyScheduleRepository @Inject constructor(
    private val weeklyScheduleDao: WeeklyScheduleDao,
) : WeeklyScheduleRepository {

    override fun observeWeeklySchedule(): Flow<Map<DayOfWeek, WeeklyScheduleEntry>> {
        return weeklyScheduleDao.observeAll().map { entities ->
            entities.mapNotNull { entity ->
                val day = WeekScheduleHelper.dayFromIsoValueOrNull(entity.dayOfWeek) ?: return@mapNotNull null
                val status = ScheduleDayStatus.fromNameOrNull(entity.status) ?: return@mapNotNull null
                day to WeeklyScheduleEntry(
                    dayOfWeek = day,
                    status = status,
                    routineId = entity.routineId,
                    updatedAt = entity.updatedAt,
                )
            }.toMap()
        }
    }

    override suspend fun setDayRoutine(dayOfWeek: DayOfWeek, routineId: String) = withContext(Dispatchers.IO) {
        weeklyScheduleDao.upsert(
            WeeklyScheduleEntity(
                dayOfWeek = dayOfWeek.value,
                status = ScheduleDayStatus.ROUTINE.name,
                routineId = routineId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun setDayRest(dayOfWeek: DayOfWeek) = withContext(Dispatchers.IO) {
        weeklyScheduleDao.upsert(
            WeeklyScheduleEntity(
                dayOfWeek = dayOfWeek.value,
                status = ScheduleDayStatus.REST.name,
                routineId = null,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun clearDay(dayOfWeek: DayOfWeek) = withContext(Dispatchers.IO) {
        weeklyScheduleDao.deleteByDay(dayOfWeek.value)
    }
}
