package com.himma.app.features.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.himma.app.R

enum class HimmaDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    TODAY(
        route = "today",
        labelRes = R.string.nav_today,
        titleRes = R.string.app_name,
        icon = Icons.Rounded.Today,
    ),
    ROUTINES(
        route = "routines",
        labelRes = R.string.nav_routines,
        titleRes = R.string.routines_title,
        icon = Icons.Rounded.FitnessCenter,
    ),
    EXERCISES(
        route = "exercises",
        labelRes = R.string.nav_exercises,
        titleRes = R.string.exercises_title,
        icon = Icons.AutoMirrored.Rounded.ListAlt,
    ),
    ANALYTICS(
        route = "analytics",
        labelRes = R.string.nav_analytics,
        titleRes = R.string.analytics_title,
        icon = Icons.Rounded.QueryStats,
    ),
    SETTINGS(
        route = "settings",
        labelRes = R.string.nav_settings,
        titleRes = R.string.settings_title,
        icon = Icons.Rounded.Settings,
    ),
}
