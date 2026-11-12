package com.himma.app.features.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
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
import com.himma.app.domain.model.FinishedWorkoutSummary
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.PreviousSetHint
import com.himma.app.domain.model.SetType
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WorkoutExerciseGroup
import com.himma.app.domain.model.WorkoutMath
import com.himma.app.domain.model.WorkoutSet
import com.himma.app.features.exercises.icon
import com.himma.app.features.exercises.labelRes
import com.himma.app.features.routines.RoutinePickerFilterState
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.session?.title.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = WorkoutMath.formatTimer(elapsedSeconds),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
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
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.restTimer.isRunning,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                RestTimerDockedBar(
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
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
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
                                onQuickIncrement = { set, delta ->
                                    viewModel.quickIncrementWeight(set, delta)
                                },
                                onRepsChange = { set, input ->
                                    viewModel.updateRepsFromInput(set, input)
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
                                onRemoveSet = { set -> viewModel.removeSet(set.id) },
                                onAddSet = { viewModel.addSet(group) },
                                onRemoveExercise = { viewModel.removeExercise(group.exercise.id) },
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
    onQuickIncrement: (WorkoutSet, Double) -> Unit,
    onRepsChange: (WorkoutSet, String) -> Unit,
    onSetTypeChange: (WorkoutSet, SetType) -> Unit,
    onToggleComplete: (WorkoutSet) -> Unit,
    onRemoveSet: (WorkoutSet) -> Unit,
    onAddSet: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    val context = LocalContext.current
    val thumbUri = group.exercise.frameAssetUri(0)

    var selectedSetIdForIncrement by remember(group.sets.size) {
        mutableStateOf(
            group.sets.firstOrNull { !it.isCompleted }?.id ?: group.sets.lastOrNull()?.id,
        )
    }
    val activeTargetSet = group.sets.firstOrNull { it.id == selectedSetIdForIncrement }
        ?: group.sets.firstOrNull { !it.isCompleted }
        ?: group.sets.lastOrNull()

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
                    Text(
                        text = stringResource(id = R.string.seconds_short, group.restSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
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

            if (activeTargetSet != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(1.0, 2.5, 5.0).forEach { delta ->
                        AssistChip(
                            onClick = { onQuickIncrement(activeTargetSet, delta) },
                            label = {
                                Text("+${formatCleanNumber(delta)} $unitLabel")
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            group.sets.forEach { set ->
                val prevHint = previousHints[set.exerciseId to set.setIndex]
                WorkoutSetRow(
                    set = set,
                    weightUnit = weightUnit,
                    unitLabel = unitLabel,
                    previousHint = prevHint,
                    canRemove = group.sets.size > 1,
                    onFocusSet = { selectedSetIdForIncrement = set.id },
                    onWeightChange = { onWeightChange(set, it) },
                    onRepsChange = { onRepsChange(set, it) },
                    onSetTypeChange = { onSetTypeChange(set, it) },
                    onToggleComplete = { onToggleComplete(set) },
                    onRemoveSet = { onRemoveSet(set) },
                )
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
    weightUnit: WeightUnit,
    unitLabel: String,
    previousHint: PreviousSetHint?,
    canRemove: Boolean,
    onFocusSet: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onToggleComplete: () -> Unit,
    onRemoveSet: () -> Unit,
) {
    var isWeightFocused by remember { mutableStateOf(false) }
    var isRepsFocused by remember { mutableStateOf(false) }

    val formattedExternalWeight = remember(set.weightKg, weightUnit) {
        set.weightKg?.let { kg ->
            formatCleanNumber(WeightUnit.toDisplayWeight(kg, weightUnit))
        }.orEmpty()
    }
    var weightText by remember(set.id, weightUnit) { mutableStateOf(formattedExternalWeight) }

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

    var typeMenuExpanded by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "#${set.setIndex + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    if (set.isPr) {
                        Text(
                            text = "PR 🏆",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Box {
                        Surface(
                            color = setTypeBadgeContainerColor(set.setType),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .clickable { typeMenuExpanded = true },
                        ) {
                            Text(
                                text = stringResource(id = set.setType.labelRes()),
                                style = MaterialTheme.typography.labelMedium,
                                color = setTypeBadgeContentColor(set.setType),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
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
                }

                val prevText = if (previousHint?.weightKg != null || previousHint?.reps != null) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prevText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (canRemove) {
                        IconButton(
                            onClick = onRemoveSet,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(id = R.string.workout_remove_set),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        onWeightChange(it)
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.workout_weight_label, unitLabel),
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
                            if (focusState.isFocused) onFocusSet()
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
                            if (focusState.isFocused) onFocusSet()
                        },
                )

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
private fun RestTimerDockedBar(
    restTimer: RestTimerState,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { restTimer.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = WorkoutMath.formatTimer(restTimer.remainingSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
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

                OutlinedButton(
                    onClick = { onAdjust(-15) },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.rest_timer_minus_15))
                }

                OutlinedButton(
                    onClick = { onAdjust(15) },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.rest_timer_plus_15))
                }

                FilledIconButton(
                    onClick = onSkip,
                    modifier = Modifier.heightIn(min = 48.dp),
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
