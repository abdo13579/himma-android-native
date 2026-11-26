package com.himma.app.domain.model

import java.time.DayOfWeek

enum class ScheduleDayStatus {
    ROUTINE,
    REST,
    UNSET,
    ;

    companion object {
        fun fromNameOrNull(raw: String?): ScheduleDayStatus? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        }
    }
}

data class WeeklyScheduleEntry(
    val dayOfWeek: DayOfWeek,
    val status: ScheduleDayStatus,
    val routineId: String? = null,
    val updatedAt: Long = 0L,
)

object WeekScheduleHelper {
    val DEFAULT_WEEK_START: DayOfWeek = DayOfWeek.SATURDAY

    /**
     * Returns the 7 days of the week in display order starting at [startDay].
     */
    fun orderedDaysStartingFrom(startDay: DayOfWeek): List<DayOfWeek> =
        (0 until 7).map { offset ->
            DayOfWeek.of(((startDay.value - 1 + offset) % 7) + 1)
        }

    fun dayFromIsoValueOrNull(value: Int): DayOfWeek? =
        if (value in 1..7) DayOfWeek.of(value) else null

    fun dayFromNameOrDefault(
        name: String?,
        default: DayOfWeek = DEFAULT_WEEK_START,
    ): DayOfWeek {
        if (name.isNullOrBlank()) return default
        return DayOfWeek.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            ?: default
    }
}
