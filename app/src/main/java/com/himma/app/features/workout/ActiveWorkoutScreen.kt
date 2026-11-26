package com.himma.app.features.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.himma.app.R
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.ExerciseType
import com.himma.app.domain.model.FinishedWorkoutSummary
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.PreviousSetHint
import com.himma.app.domain.model.SetType
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WeightUnitMode
import com.himma.app.domain.model.WorkoutExerciseGroup
import com.himma.app.domain.model.WorkoutMath
import com.himma.app.domain.model.WorkoutSet
import com.himma.app.features.exercises.icon
import com.himma.app.features.exercises.labelRes
import com.himma.app.features.routines.RoutinePickerFilterState
import com.himma.app.features.shell.LocalAnimationsEnabled
import com.himma.app.service.RestTimerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onBackClick: () -> Unit,
    onWorkoutFinished: (FinishedWorkoutSummary) -> Unit,
    onWorkoutDiscarded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var menuExpanded by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val unitLabel = when (state.weightUnit) {
        WeightUnit.KG -> stringResource(id = R.string.unit_kg)
        WeightUnit.LBS -> stringResource(id = R.string.unit_lbs)
    }

    val activeSession = state.session
    val elapsedSeconds by produceState(
        initialValue = if (activeSession != null) {
            if (activeSession.isCompleted) {
                activeSession.durationSeconds
            } else {
                WorkoutMath.calculateDurationSeconds(activeSession.startTime, System.currentTimeMillis())
            }
        } else {
            0
        },
        key1 = activeSession?.id,
        key2 = activeSession?.isCompleted,
    ) {
        val session = activeSession ?: return@produceState
        if (session.isCompleted) {
            value = session.durationSeconds
        } else {
            while (true) {
                value = WorkoutMath.calculateDurationSeconds(session.startTime, System.currentTimeMillis())
                delay(1_000L)
            }
        }
    }

    val completedSets = state.session?.completedSetsCount ?: 0
    val totalSets = state.session?.totalSetsCount ?: 0
    val targetProgress = state.session?.progress ?: 0f
    val animationsEnabled = LocalAnimationsEnabled.current
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (animationsEnabled) tween(durationMillis = 350) else snap(),
        label = "workoutProgress",
    )
    val progressPercent = (targetProgress * 100).toInt()
    val progressDescription = stringResource(
        id = R.string.workout_progress_desc,
        progressPercent,
        completedSets,
        totalSets,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.session?.title.orEmpty(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = WorkoutMath.formatTimer(elapsedSeconds),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (totalSets > 0) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    Text(
                                        text = stringResource(
                                            id = R.string.workout_progress_percent,
                                            progressPercent,
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.exercise_detail_back),
                            )
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = { showFinishDialog = true },
                            enabled = state.session != null,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(id = R.string.workout_finish_cta))
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(id = R.string.routines_more_actions),
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(id = R.string.workout_discard_action),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showDiscardDialog = true
                                    },
                                )
                            }
                        }
                    },
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .semantics {
                            contentDescription = progressDescription
                        },
                )
            }
        },
        bottomBar = {
            if (animationsEnabled) {
                AnimatedVisibility(
                    visible = state.restTimer.isRunning,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    RestTimerFloatingBar(
                        restTimer = state.restTimer,
                        onAdjust = viewModel::adjustRestTimer,
                        onSkip = viewModel::skipRestTimer,
                    )
                }
            } else if (state.restTimer.isRunning) {
                RestTimerFloatingBar(
                    restTimer = state.restTimer,
                    onAdjust = viewModel::adjustRestTimer,
                    onSkip = viewModel::skipRestTimer,
                )
            }
        },
    ) { innerPadding ->
        val session = state.session
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            session == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(id = R.string.exercise_detail_back))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item(key = "session_notes") {
                        var notesText by remember(session.id) { mutableStateOf(session.notes) }
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = {
                                notesText = it
                                viewModel.updateNotes(it)
                            },
                            label = { Text(stringResource(id = R.string.workout_notes_label)) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (session.exercises.isEmpty()) {
                        item(key = "empty_workout") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.workout_empty_exercises),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp),
                                )
                            }
                        }
                    } else {
                        items(
                            items = session.exercises,
                            key = { "${it.exercise.id}_${it.exerciseOrder}" },
                        ) { group ->
                            WorkoutExerciseCard(
                                group = group,
                                language = state.language,
                                weightUnit = state.weightUnit,
                                unitLabel = unitLabel,
                                previousHints = state.previousHints,
                                onWeightChange = { set, input ->
                                    viewModel.updateWeightFromInput(set, input)
                                },
                                onRepsChange = { set, input ->
                                    viewModel.updateRepsFromInput(set, input)
                                },
                                onDurationChange = { set, seconds ->
                                    viewModel.updateDurationSeconds(set, seconds)
                                },
                                onSetTypeChange = { set, type ->
                                    viewModel.updateSetType(set, type)
                                },
                                onToggleComplete = { set ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleSetCompleted(
                                        set = set,
                                        exerciseDisplayName = group.exercise.primaryDisplayName(state.language),
                                    )
                                },
                                onWeightUnitModeChange = { mode ->
                                    viewModel.updateExerciseWeightUnitMode(group, mode)
                                },
                                onRemoveSet = { set -> viewModel.removeSet(set.id) },
                                onAddSet = { viewModel.addSet(group) },
                                onRemoveExercise = {
                                    viewModel.removeExercise(group.exercise.id, group.exerciseOrder)
                                },
                            )
                        }
                    }

                    item(key = "add_exercise_cta") {
                        FilledTonalButton(
                            onClick = { viewModel.setPickerOpen(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.workout_add_exercise))
                        }
                    }
                }
            }
        }
    }

    if (showFinishDialog && state.session != null) {
        val currentSession = state.session!!
        val allSets = currentSession.exercises.flatMap { it.sets }
        val liveVolumeKg = WorkoutMath.calculateTotalVolumeKg(allSets)
        val displayVolume = WeightUnit.toDisplayWeight(liveVolumeKg, state.weightUnit)

        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(stringResource(id = R.string.workout_finish_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.workout_finish_body,
                        WorkoutMath.formatTimer(elapsedSeconds),
                        formatCleanNumber(displayVolume),
                        unitLabel,
                        currentSession.completedSetsCount,
                    ),
                )
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showFinishDialog = false
                        viewModel.finishWorkout(onWorkoutFinished)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.workout_finish_cta))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinishDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.exercise_cancel))
                }
            },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(id = R.string.workout_discard_title)) },
            text = { Text(stringResource(id = R.string.workout_discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout(onWorkoutDiscarded)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.workout_discard_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.exercise_cancel))
                }
            },
        )
    }

    if (state.isPickerOpen) {
        WorkoutExercisePickerSheet(
            filter = state.pickerFilter,
            exercises = state.pickerExercises,
            language = state.language,
            onQueryChange = viewModel::updatePickerQuery,
            onSelectMuscle = viewModel::selectPickerMuscle,
            onSelectEquipment = viewModel::selectPickerEquipment,
            onClearFilters = viewModel::clearPickerFilters,
            onPickExercise = viewModel::addExerciseFromPicker,
            onDismiss = { viewModel.setPickerOpen(false) },
        )
    }
}

@Composable
private fun WorkoutExerciseCard(
    group: WorkoutExerciseGroup,
    language: AppLanguage,
    weightUnit: WeightUnit,
    unitLabel: String,
    previousHints: Map<Pair<String, Int>, PreviousSetHint>,
    onWeightChange: (WorkoutSet, String) -> Unit,
    onRepsChange: (WorkoutSet, String) -> Unit,
    onDurationChange: (WorkoutSet, Int?) -> Unit,
    onSetTypeChange: (WorkoutSet, SetType) -> Unit,
    onToggleComplete: (WorkoutSet) -> Unit,
    onWeightUnitModeChange: (WeightUnitMode) -> Unit = {},
    onRemoveSet: (WorkoutSet) -> Unit,
    onAddSet: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    val context = LocalContext.current
    val thumbUri = group.exercise.frameAssetUri(0)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (thumbUri != null) {
                                Color(0xFF0A110E)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumbUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbUri)
                                .crossfade(false)
                                .build(),
                            contentDescription = group.exercise.primaryDisplayName(language),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                        )
                    } else {
                        Icon(
                            imageVector = group.exercise.equipment.icon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.exercise.primaryDisplayName(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.seconds_short, group.restSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (group.exercise.supportsStackPlates) {
                            val isPlates = group.sets.firstOrNull()?.weightUnitMode == WeightUnitMode.STACK_PLATES
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 48.dp)
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        val nextMode = if (isPlates) WeightUnitMode.WEIGHT else WeightUnitMode.STACK_PLATES
                                        onWeightUnitModeChange(nextMode)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (isPlates) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        text = if (isPlates) {
                                            stringResource(id = R.string.weight_mode_plates)
                                        } else {
                                            stringResource(id = R.string.weight_mode_weight)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPlates) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onRemoveExercise,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(id = R.string.workout_remove_exercise),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            group.sets.forEach { set ->
                key(set.id) {
                    val prevHint = previousHints[set.exerciseId to set.setIndex]
                    WorkoutSetRow(
                        set = set,
                        exerciseType = group.exercise.exerciseType,
                        weightUnit = weightUnit,
                        unitLabel = unitLabel,
                        previousHint = prevHint,
                        canRemove = true,
                        onWeightChange = { onWeightChange(set, it) },
                        onRepsChange = { onRepsChange(set, it) },
                        onDurationChange = { onDurationChange(set, it) },
                        onSetTypeChange = { onSetTypeChange(set, it) },
                        onToggleComplete = { onToggleComplete(set) },
                        onRemoveSet = {
                            if (group.sets.size > 1) {
                                onRemoveSet(set)
                            } else {
                                onRemoveExercise()
                            }
                        },
                    )
                }
            }

            OutlinedButton(
                onClick = onAddSet,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(id = R.string.workout_add_set))
            }
        }
    }
}

@Composable
private fun WorkoutSetRow(
    set: WorkoutSet,
    exerciseType: ExerciseType = ExerciseType.REPS,
    weightUnit: WeightUnit,
    unitLabel: String,
    previousHint: PreviousSetHint?,
    canRemove: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDurationChange: (Int?) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onToggleComplete: () -> Unit,
    onRemoveSet: () -> Unit,
) {
    var isWeightFocused by remember(set.id) { mutableStateOf(false) }
    var isRepsFocused by remember(set.id) { mutableStateOf(false) }
    var isMinFocused by remember(set.id) { mutableStateOf(false) }
    var isSecFocused by remember(set.id) { mutableStateOf(false) }

    val isStackPlates = set.weightUnitMode == WeightUnitMode.STACK_PLATES
    val formattedExternalWeight = remember(set.weightKg, weightUnit, isStackPlates) {
        if (isStackPlates) {
            set.weightKg?.let { formatCleanNumber(it) }.orEmpty()
        } else {
            set.weightKg?.let { kg ->
                formatCleanNumber(WeightUnit.toDisplayWeight(kg, weightUnit))
            }.orEmpty()
        }
    }
    var weightText by remember(set.id, weightUnit, isStackPlates) { mutableStateOf(formattedExternalWeight) }

    LaunchedEffect(formattedExternalWeight, isWeightFocused) {
        if (!isWeightFocused && weightText != formattedExternalWeight) {
            weightText = formattedExternalWeight
        }
    }

    val formattedExternalReps = set.reps?.toString().orEmpty()
    var repsText by remember(set.id) { mutableStateOf(formattedExternalReps) }

    LaunchedEffect(formattedExternalReps, isRepsFocused) {
        if (!isRepsFocused && repsText != formattedExternalReps) {
            repsText = formattedExternalReps
        }
    }

    val formattedMin = set.durationSeconds?.let { (it / 60).toString() }.orEmpty()
    val formattedSec = set.durationSeconds?.let { (it % 60).toString() }.orEmpty()

    var minText by remember(set.id) { mutableStateOf(formattedMin) }
    var secText by remember(set.id) { mutableStateOf(formattedSec) }

    LaunchedEffect(formattedMin, isMinFocused) {
        if (!isMinFocused && minText != formattedMin) {
            minText = formattedMin
        }
    }
    LaunchedEffect(formattedSec, isSecFocused) {
        if (!isSecFocused && secText != formattedSec) {
            secText = formattedSec
        }
    }

    var typeMenuExpanded by remember(set.id) { mutableStateOf(false) }

    Surface(
        color = if (set.isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val prevText = if (exerciseType == ExerciseType.TIME) {
                if (previousHint?.durationSeconds != null) {
                    stringResource(
                        id = R.string.workout_previous_time_hint,
                        WorkoutMath.formatTimer(previousHint.durationSeconds),
                    )
                } else {
                    stringResource(id = R.string.workout_previous_none)
                }
            } else if (isStackPlates || previousHint?.weightUnitMode == WeightUnitMode.STACK_PLATES) {
                if (previousHint?.weightKg != null || previousHint?.reps != null) {
                    val prevPlates = previousHint?.weightKg?.let { formatCleanNumber(it) } ?: "0"
                    val prevReps = previousHint?.reps?.toString() ?: "0"
                    stringResource(
                        id = R.string.workout_previous_plates_hint,
                        prevPlates,
                        prevReps,
                    )
                } else {
                    stringResource(id = R.string.workout_previous_none)
                }
            } else {
                if (previousHint?.weightKg != null || previousHint?.reps != null) {
                    val prevWeight = previousHint.weightKg?.let {
                        formatCleanNumber(WeightUnit.toDisplayWeight(it, weightUnit))
                    } ?: "0"
                    val prevReps = previousHint.reps?.toString() ?: "0"
                    stringResource(
                        id = R.string.workout_previous_hint,
                        "$prevWeight $unitLabel × $prevReps",
                    )
                } else {
                    stringResource(id = R.string.workout_previous_none)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "#${set.setIndex + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    if (set.isPr) {
                        Text(
                            text = stringResource(id = R.string.workout_pr_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .widthIn(min = 48.dp)
                                .heightIn(min = 48.dp)
                                .clickable { typeMenuExpanded = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                color = setTypeBadgeContainerColor(set.setType),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    text = stringResource(id = set.setType.labelRes()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = setTypeBadgeContentColor(set.setType),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false },
                        ) {
                            SetType.entries.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = candidate.labelRes())) },
                                    onClick = {
                                        typeMenuExpanded = false
                                        onSetTypeChange(candidate)
                                    },
                                )
                            }
                        }
                    }

                    if (prevText.isNotBlank()) {
                        Text(
                            text = prevText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (canRemove) {
                    IconButton(
                        onClick = onRemoveSet,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(id = R.string.workout_remove_set),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (exerciseType == ExerciseType.TIME) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = {
                            minText = it
                            val m = it.trim().toIntOrNull()
                            val s = secText.trim().toIntOrNull()
                            if (m == null && s == null) {
                                onDurationChange(null)
                            } else {
                                onDurationChange(((m ?: 0) * 60 + (s ?: 0)).coerceIn(0, 86400))
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(id = R.string.workout_minutes_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                isMinFocused = focusState.isFocused
                            },
                    )

                    OutlinedTextField(
                        value = secText,
                        onValueChange = {
                            secText = it
                            val m = minText.trim().toIntOrNull()
                            val s = it.trim().toIntOrNull()
                            if (m == null && s == null) {
                                onDurationChange(null)
                            } else {
                                onDurationChange(((m ?: 0) * 60 + (s ?: 0)).coerceIn(0, 86400))
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(id = R.string.workout_seconds_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                isSecFocused = focusState.isFocused
                            },
                    )
                } else {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = {
                            weightText = it
                            onWeightChange(it)
                        },
                        label = {
                            Text(
                                text = if (isStackPlates) {
                                    stringResource(id = R.string.workout_plates_label)
                                } else {
                                    stringResource(id = R.string.workout_weight_label, unitLabel)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                isWeightFocused = focusState.isFocused
                            },
                    )

                    OutlinedTextField(
                        value = repsText,
                        onValueChange = {
                            repsText = it
                            onRepsChange(it)
                        },
                        label = {
                            Text(
                                text = stringResource(id = R.string.workout_reps_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.85f)
                            .onFocusChanged { focusState ->
                                isRepsFocused = focusState.isFocused
                            },
                    )
                }

                FilledTonalIconToggleButton(
                    checked = set.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(id = R.string.workout_mark_done),
                    )
                }
            }
        }
    }
}

@Composable
private fun RestTimerFloatingBar(
    restTimer: RestTimerState,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
) {
    val barShape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = barShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            ),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(barShape),
            ) {
                LinearProgressIndicator(
                    progress = { restTimer.progress },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = WorkoutMath.formatTimer(restTimer.remainingSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                        if (restTimer.exerciseName.isNotBlank()) {
                            Text(
                                text = restTimer.exerciseName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { onAdjust(-15) },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.rest_timer_minus_15),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    FilledTonalButton(
                        onClick = { onAdjust(15) },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.rest_timer_plus_15),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(id = R.string.rest_timer_skip),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutExercisePickerSheet(
    filter: RoutinePickerFilterState,
    exercises: List<Exercise>,
    language: AppLanguage,
    onQueryChange: (String) -> Unit,
    onSelectMuscle: (MuscleGroup?) -> Unit,
    onSelectEquipment: (EquipmentType?) -> Unit,
    onClearFilters: () -> Unit,
    onPickExercise: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.workout_add_exercise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(id = R.string.exercise_cancel),
                    )
                }
            }

            OutlinedTextField(
                value = filter.query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.exercises_search_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(id = R.string.exercises_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = filter.selectedMuscle == null,
                        onClick = { onSelectMuscle(null) },
                        label = { Text(stringResource(id = R.string.exercises_filter_all)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                items(MuscleGroup.entries) { muscle ->
                    FilterChip(
                        selected = filter.selectedMuscle == muscle,
                        onClick = { onSelectMuscle(muscle) },
                        label = { Text(stringResource(id = muscle.labelRes())) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = filter.selectedEquipment == null,
                        onClick = { onSelectEquipment(null) },
                        label = { Text(stringResource(id = R.string.exercises_filter_all)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                items(EquipmentType.coreTypes) { equipment ->
                    FilterChip(
                        selected = filter.selectedEquipment == equipment,
                        onClick = { onSelectEquipment(equipment) },
                        label = { Text(stringResource(id = equipment.labelRes())) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            if (exercises.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.exercises_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(id = R.string.exercises_reset_filters))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = exercises,
                        key = { it.id },
                    ) { exercise ->
                        val thumbUri = exercise.frameAssetUri(0)
                        Surface(
                            onClick = { onPickExercise(exercise) },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (thumbUri != null) {
                                                Color(0xFF0A110E)
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (thumbUri != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(thumbUri)
                                                .crossfade(false)
                                                .build(),
                                            contentDescription = exercise.primaryDisplayName(language),
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = exercise.equipment.icon(),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.primaryDisplayName(language),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${stringResource(id = exercise.primaryMuscle.labelRes())} • ${
                                            stringResource(id = exercise.equipment.labelRes())
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(id = R.string.workout_add_exercise),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun SetType.labelRes(): Int = when (this) {
    SetType.NORMAL -> R.string.set_type_normal
    SetType.WARMUP -> R.string.set_type_warmup
    SetType.DROP_SET -> R.string.set_type_drop_set
    SetType.FAILURE -> R.string.set_type_failure
}

@Composable
private fun setTypeBadgeContainerColor(setType: SetType): Color = when (setType) {
    SetType.NORMAL -> MaterialTheme.colorScheme.surfaceContainerHighest
    SetType.WARMUP -> MaterialTheme.colorScheme.tertiaryContainer
    SetType.DROP_SET -> MaterialTheme.colorScheme.secondaryContainer
    SetType.FAILURE -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun setTypeBadgeContentColor(setType: SetType): Color = when (setType) {
    SetType.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    SetType.WARMUP -> MaterialTheme.colorScheme.onTertiaryContainer
    SetType.DROP_SET -> MaterialTheme.colorScheme.onSecondaryContainer
    SetType.FAILURE -> MaterialTheme.colorScheme.onErrorContainer
}

private fun formatCleanNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(java.util.Locale.US, value)
    }
}
