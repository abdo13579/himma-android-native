package com.himma.app.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.himma.app.R
import java.time.DayOfWeek

@Composable
fun dayOfWeekFullName(day: DayOfWeek): String = stringResource(
    id = when (day) {
        DayOfWeek.SATURDAY -> R.string.day_full_sat
        DayOfWeek.SUNDAY -> R.string.day_full_sun
        DayOfWeek.MONDAY -> R.string.day_full_mon
        DayOfWeek.TUESDAY -> R.string.day_full_tue
        DayOfWeek.WEDNESDAY -> R.string.day_full_wed
        DayOfWeek.THURSDAY -> R.string.day_full_thu
        DayOfWeek.FRIDAY -> R.string.day_full_fri
    },
)

@Composable
fun dayOfWeekShortName(day: DayOfWeek): String = stringResource(
    id = when (day) {
        DayOfWeek.SATURDAY -> R.string.day_short_sat
        DayOfWeek.SUNDAY -> R.string.day_short_sun
        DayOfWeek.MONDAY -> R.string.day_short_mon
        DayOfWeek.TUESDAY -> R.string.day_short_tue
        DayOfWeek.WEDNESDAY -> R.string.day_short_wed
        DayOfWeek.THURSDAY -> R.string.day_short_thu
        DayOfWeek.FRIDAY -> R.string.day_short_fri
    },
)
