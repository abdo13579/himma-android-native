package com.himma.app.features.shell

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.himma.app.R
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WorkoutMath
import com.himma.app.features.analytics.AnalyticsScreen
import com.himma.app.features.exercises.CustomExerciseScreen
import com.himma.app.features.exercises.ExerciseDetailScreen
import com.himma.app.features.exercises.ExerciseLibraryScreen
import com.himma.app.features.onboarding.OnboardingScreen
import com.himma.app.features.routines.RoutineEditorScreen
import com.himma.app.features.routines.RoutinesScreen
import com.himma.app.features.settings.SettingsScreen
import com.himma.app.features.workout.ActiveWorkoutScreen
import com.himma.app.features.workout.WorkoutLauncherViewModel
import kotlinx.coroutines.launch

private object ExerciseRoutes {
    const val DETAIL = "exercise_detail/{exerciseId}"
    const val CUSTOM_FORM = "custom_exercise?exerciseId={exerciseId}"

    fun detail(exerciseId: String): String = "exercise_detail/${Uri.encode(exerciseId)}"
    fun createCustom(): String = "custom_exercise"
    fun editCustom(exerciseId: String): String = "custom_exercise?exerciseId=${Uri.encode(exerciseId)}"
}

private object RoutineRoutes {
    const val EDITOR = "routine_editor?routineId={routineId}"

    fun create(): String = "routine_editor"
    fun edit(routineId: String): String = "routine_editor?routineId=${Uri.encode(routineId)}"
}

private object WorkoutRoutes {
    const val ACTIVE = "active_workout/{workoutId}"

    fun active(workoutId: String): String = "active_workout/${Uri.encode(workoutId)}"
}

@Composable
fun HimmaRootNavHost(
    onboardingCompleted: Boolean,
) {
    Crossfade(
        targetState = onboardingCompleted,
        label = "RootOnboardingTransition",
    ) { isCompleted ->
        if (!isCompleted) {
            OnboardingScreen()
        } else {
            HimmaAppShell()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HimmaAppShell(
    workoutLauncherViewModel: WorkoutLauncherViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val pendingConflict by workoutLauncherViewModel.pendingConflict.collectAsStateWithLifecycle()
    val preferredUnit by workoutLauncherViewModel.weightUnit.collectAsStateWithLifecycle()

    val savedExerciseMsg = stringResource(id = R.string.custom_exercise_saved_msg)
    val deletedExerciseMsg = stringResource(id = R.string.custom_exercise_deleted_msg)
    val savedRoutineMsg = stringResource(id = R.string.routine_editor_saved_msg)
    val duplicatedRoutineMsg = stringResource(id = R.string.routines_duplicated_msg)
    val undoLabel = stringResource(id = R.string.routines_undo)
    val freeWorkoutDefaultTitle = stringResource(id = R.string.workout_free_title)

    val activeTab = HimmaDestination.entries.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }
    val showShellChrome = activeTab != null || currentDestination == null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (showShellChrome) {
                val topTab = activeTab ?: HimmaDestination.TODAY
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = topTab.titleRes),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        },
        bottomBar = {
            if (showShellChrome) {
                HimmaFloatingNavigationBar(
                    destinations = HimmaDestination.entries,
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        if (currentDestination?.hierarchy?.any { it.route == destination.route } == true) {
                            return@HimmaFloatingNavigationBar
                        }
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HimmaDestination.TODAY.route,
            modifier = if (showShellChrome) Modifier.padding(innerPadding) else Modifier,
        ) {
            composable(HimmaDestination.TODAY.route) {
                TodayFoundationScreen(
                    onResumeActiveWorkout = { workoutId ->
                        navController.navigate(WorkoutRoutes.active(workoutId))
                    },
                    onStartFreeWorkout = {
                        workoutLauncherViewModel.requestStartFreeWorkout(freeWorkoutDefaultTitle) { id ->
                            navController.navigate(WorkoutRoutes.active(id))
                        }
                    },
                    onStartRoutine = { routine ->
                        workoutLauncherViewModel.requestStartFromRoutine(routine) { id ->
                            navController.navigate(WorkoutRoutes.active(id))
                        }
                    },
                )
            }
            composable(HimmaDestination.ROUTINES.route) {
                RoutinesScreen(
                    onCreateRoutineClick = {
                        navController.navigate(RoutineRoutes.create())
                    },
                    onEditRoutineClick = { routineId ->
                        navController.navigate(RoutineRoutes.edit(routineId))
                    },
                    onStartRoutineClick = { routine ->
                        workoutLauncherViewModel.requestStartFromRoutine(routine) { id ->
                            navController.navigate(WorkoutRoutes.active(id))
                        }
                    },
                    onShowDuplicatedMessage = {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(duplicatedRoutineMsg)
                        }
                    },
                    onShowDeletedUndo = { routine, onUndo ->
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(
                                    R.string.routines_deleted_msg,
                                    routine.title,
                                ),
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                onUndo()
                            }
                        }
                    },
                )
            }
            composable(HimmaDestination.EXERCISES.route) {
                ExerciseLibraryScreen(
                    onExerciseClick = { exerciseId ->
                        navController.navigate(ExerciseRoutes.detail(exerciseId))
                    },
                    onCreateCustomClick = {
                        navController.navigate(ExerciseRoutes.createCustom())
                    },
                )
            }
            composable(HimmaDestination.ANALYTICS.route) {
                AnalyticsScreen(
                    onShowMessage = { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                )
            }
            composable(HimmaDestination.SETTINGS.route) {
                SettingsScreen(
                    onShowMessage = { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                )
            }

            composable(
                route = ExerciseRoutes.DETAIL,
                arguments = listOf(
                    navArgument("exerciseId") { type = NavType.StringType },
                ),
            ) {
                ExerciseDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditCustomClick = { id ->
                        navController.navigate(ExerciseRoutes.editCustom(id))
                    },
                    onDeleted = {
                        navController.popBackStack()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(deletedExerciseMsg)
                        }
                    },
                )
            }

            composable(
                route = ExerciseRoutes.CUSTOM_FORM,
                arguments = listOf(
                    navArgument("exerciseId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                CustomExerciseScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(savedExerciseMsg)
                        }
                    },
                )
            }

            composable(
                route = RoutineRoutes.EDITOR,
                arguments = listOf(
                    navArgument("routineId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                RoutineEditorScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(savedRoutineMsg)
                        }
                    },
                )
            }

            composable(
                route = WorkoutRoutes.ACTIVE,
                arguments = listOf(
                    navArgument("workoutId") { type = NavType.StringType },
                ),
            ) {
                ActiveWorkoutScreen(
                    onBackClick = { navController.popBackStack() },
                    onWorkoutFinished = { summary ->
                        navController.popBackStack()
                        val displayVolume = WeightUnit.toDisplayWeight(summary.totalVolumeKg, preferredUnit)
                        val volStr = if (displayVolume % 1.0 == 0.0) {
                            displayVolume.toInt().toString()
                        } else {
                            "%.1f".format(java.util.Locale.US, displayVolume)
                        }
                        val unitStr = context.getString(
                            if (preferredUnit == WeightUnit.KG) R.string.unit_kg else R.string.unit_lbs,
                        )
                        val durStr = WorkoutMath.formatTimer(summary.durationSeconds)
                        val baseMsg = context.getString(R.string.workout_finished_msg, volStr, unitStr, durStr)
                        val finalMsg = if (summary.newPrCount > 0) {
                            "$baseMsg • ${context.getString(R.string.analytics_pr_badge, summary.newPrCount)}"
                        } else {
                            baseMsg
                        }
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(finalMsg)
                        }
                    },
                    onWorkoutDiscarded = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }

    pendingConflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = workoutLauncherViewModel::dismissConflict,
            title = { Text(stringResource(id = R.string.workout_replace_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.workout_replace_dialog_body,
                        conflict.activeDraft.title,
                    ),
                )
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        workoutLauncherViewModel.resumeConflictingDraft { id ->
                            navController.navigate(WorkoutRoutes.active(id))
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.workout_resume_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        workoutLauncherViewModel.discardAndStartPending { id ->
                            navController.navigate(WorkoutRoutes.active(id))
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.workout_start_new_action))
                }
            },
        )
    }
}
