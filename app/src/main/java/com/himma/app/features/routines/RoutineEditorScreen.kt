package com.himma.app.features.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.himma.app.R
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.RoutineExercise
import com.himma.app.domain.model.RoutineExerciseDraft
import com.himma.app.features.exercises.icon
import com.himma.app.features.exercises.labelRes

private const val HEADER_ITEMS_BEFORE_EXERCISES = 1
private val LIST_ITEM_SPACING = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form = state.form
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val itemSpacingPx = with(LocalDensity.current) { LIST_ITEM_SPACING.toPx() }

    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var draggingOffsetY by remember { mutableFloatStateOf(0f) }

    val currentExercises by rememberUpdatedState(form.exercises)
    val onMoveCallback by rememberUpdatedState(viewModel::moveExercise)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            id = if (form.routineId == null) {
                                R.string.routine_editor_create_title
                            } else {
                                R.string.routine_editor_edit_title
                            },
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                        onClick = { viewModel.saveRoutine(onSaved) },
                        enabled = !form.isSaving && !form.isLoading,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(id = R.string.routine_editor_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (form.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(LIST_ITEM_SPACING),
            ) {
                item(key = "routine_header") {
                    RoutineHeaderFields(
                        title = form.title,
                        notes = form.notes,
                        titleError = form.titleError,
                        exerciseCount = form.exercises.size,
                        onTitleChange = viewModel::updateTitle,
                        onNotesChange = viewModel::updateNotes,
                        onAddExerciseClick = { viewModel.setPickerOpen(true) },
                    )
                }

                if (form.exercises.isEmpty()) {
                    item(key = "routine_empty") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(id = R.string.routine_editor_empty_exercises),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(20.dp),
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = form.exercises,
                        key = { _, item -> item.itemId },
                    ) { _, draft ->
                        val isDragging = draggingItemId == draft.itemId
                        RoutineExerciseEditorCard(
                            draft = draft,
                            language = state.language,
                            isDragging = isDragging,
                            dragTranslationY = if (isDragging) draggingOffsetY else 0f,
                            onDragStart = {
                                draggingItemId = draft.itemId
                                draggingOffsetY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragDelta = { deltaY ->
                                val activeId = draggingItemId ?: return@RoutineExerciseEditorCard
                                draggingOffsetY += deltaY
                                val swappedDelta = resolveSwapForActiveDrag(
                                    lazyListState = lazyListState,
                                    exercises = currentExercises,
                                    draggingItemId = activeId,
                                    currentDeltaY = draggingOffsetY,
                                    itemSpacingPx = itemSpacingPx,
                                    onMove = onMoveCallback,
                                )
                                if (swappedDelta != null) {
                                    draggingOffsetY = swappedDelta
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                draggingItemId = null
                                draggingOffsetY = 0f
                            },
                            onRemove = { viewModel.removeExercise(draft.itemId) },
                            onAdjustSets = { delta -> viewModel.adjustSets(draft.itemId, delta) },
                            onAdjustRepsMin = { delta -> viewModel.adjustRepsMin(draft.itemId, delta) },
                            onAdjustRepsMax = { delta -> viewModel.adjustRepsMax(draft.itemId, delta) },
                            onAdjustRest = { delta -> viewModel.adjustRestSeconds(draft.itemId, delta) },
                        )
                    }
                }
            }
        }
    }

    if (form.isPickerOpen) {
        ExercisePickerBottomSheet(
            filter = state.pickerFilter,
            exercises = state.pickerExercises,
            selectedExerciseIds = remember(form.exercises) {
                form.exercises.map { it.exercise.id }.toSet()
            },
            language = state.language,
            onQueryChange = viewModel::updatePickerQuery,
            onSelectMuscle = viewModel::selectPickerMuscle,
            onSelectEquipment = viewModel::selectPickerEquipment,
            onClearFilters = viewModel::clearPickerFilters,
            onToggleExercise = viewModel::toggleExerciseFromPicker,
            onDismiss = { viewModel.setPickerOpen(false) },
        )
    }
}

private fun resolveSwapForActiveDrag(
    lazyListState: LazyListState,
    exercises: List<RoutineExerciseDraft>,
    draggingItemId: String,
    currentDeltaY: Float,
    itemSpacingPx: Float,
    onMove: (Int, Int) -> Unit,
): Float? {
    val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
    val currentVisible = visibleItems.firstOrNull { it.key == draggingItemId } ?: return null
    val currentIndex = exercises.indexOfFirst { it.itemId == draggingItemId }
    if (currentIndex < 0) return null

    val draggedCenterY = currentVisible.offset + currentVisible.size / 2f + currentDeltaY

    val targetNeighbor = visibleItems.firstOrNull { candidate ->
        val candidateExIndex = candidate.index - HEADER_ITEMS_BEFORE_EXERCISES
        candidate.key != draggingItemId &&
            candidateExIndex in exercises.indices &&
            if (currentDeltaY > 0f) {
                candidate.index > currentVisible.index &&
                    draggedCenterY > (candidate.offset + candidate.size / 2f)
            } else {
                candidate.index < currentVisible.index &&
                    draggedCenterY < (candidate.offset + candidate.size / 2f)
            }
    } ?: return null

    val targetIndex = targetNeighbor.index - HEADER_ITEMS_BEFORE_EXERCISES
    if (targetIndex !in exercises.indices) return null

    onMove(currentIndex, targetIndex)
    return if (targetNeighbor.index > currentVisible.index) {
        currentDeltaY - (targetNeighbor.size + itemSpacingPx)
    } else {
        currentDeltaY + (targetNeighbor.size + itemSpacingPx)
    }
}

@Composable
private fun RoutineHeaderFields(
    title: String,
    notes: String,
    titleError: Boolean,
    exerciseCount: Int,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddExerciseClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(id = R.string.routine_editor_title_label)) },
            placeholder = { Text(stringResource(id = R.string.routine_editor_title_placeholder)) },
            isError = titleError,
            supportingText = if (titleError) {
                { Text(stringResource(id = R.string.routine_editor_title_required)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(id = R.string.routine_editor_notes_label)) },
            placeholder = { Text(stringResource(id = R.string.routine_editor_notes_placeholder)) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.routine_editor_exercises_header, exerciseCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            FilledTonalButton(
                onClick = onAddExerciseClick,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(id = R.string.routine_editor_add_exercise))
            }
        }
    }
}

@Composable
private fun RoutineExerciseEditorCard(
    draft: RoutineExerciseDraft,
    language: AppLanguage,
    isDragging: Boolean,
    dragTranslationY: Float,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onRemove: () -> Unit,
    onAdjustSets: (Int) -> Unit,
    onAdjustRepsMin: (Int) -> Unit,
    onAdjustRepsMax: (Int) -> Unit,
    onAdjustRest: (Int) -> Unit,
) {
    val context = LocalContext.current
    val thumbUri = draft.exercise.frameAssetUri(0)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragTranslationY
                if (isDragging) {
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(draft.itemId) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.y)
                                },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = stringResource(id = R.string.routine_editor_drag_handle),
                        tint = if (isDragging) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

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
                            contentDescription = draft.exercise.primaryDisplayName(language),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                        )
                    } else {
                        Icon(
                            imageVector = draft.exercise.equipment.icon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.exercise.primaryDisplayName(language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${stringResource(id = draft.exercise.primaryMuscle.labelRes())} • ${
                            stringResource(id = draft.exercise.equipment.labelRes())
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(id = R.string.routine_editor_remove_exercise),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            StepperSettingRow(
                label = stringResource(id = R.string.routine_editor_sets_label),
                valueText = draft.targetSets.toString(),
                canDecrease = draft.targetSets > RoutineExercise.MIN_SETS,
                canIncrease = draft.targetSets < RoutineExercise.MAX_SETS,
                onDecrease = { onAdjustSets(-1) },
                onIncrease = { onAdjustSets(1) },
            )

            StepperSettingRow(
                label = stringResource(id = R.string.routine_editor_reps_min_label),
                valueText = draft.targetRepsMin.toString(),
                canDecrease = draft.targetRepsMin > RoutineExercise.MIN_REPS,
                canIncrease = draft.targetRepsMin < RoutineExercise.MAX_REPS,
                onDecrease = { onAdjustRepsMin(-1) },
                onIncrease = { onAdjustRepsMin(1) },
            )

            StepperSettingRow(
                label = stringResource(id = R.string.routine_editor_reps_max_label),
                valueText = draft.targetRepsMax.toString(),
                canDecrease = draft.targetRepsMax > RoutineExercise.MIN_REPS,
                canIncrease = draft.targetRepsMax < RoutineExercise.MAX_REPS,
                onDecrease = { onAdjustRepsMax(-1) },
                onIncrease = { onAdjustRepsMax(1) },
            )

            StepperSettingRow(
                label = stringResource(id = R.string.routine_editor_rest_label),
                valueText = stringResource(id = R.string.seconds_short, draft.targetRestSeconds),
                canDecrease = draft.targetRestSeconds > RoutineExercise.MIN_REST_SECONDS,
                canIncrease = draft.targetRestSeconds < RoutineExercise.MAX_REST_SECONDS,
                onDecrease = { onAdjustRest(-15) },
                onIncrease = { onAdjustRest(15) },
            )
        }
    }
}

@Composable
private fun StepperSettingRow(
    label: String,
    valueText: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalIconButton(
                onClick = onDecrease,
                enabled = canDecrease,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "$label -",
                )
            }

            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(68.dp),
            )

            FilledTonalIconButton(
                onClick = onIncrease,
                enabled = canIncrease,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "$label +",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerBottomSheet(
    filter: RoutinePickerFilterState,
    exercises: List<Exercise>,
    selectedExerciseIds: Set<String>,
    language: AppLanguage,
    onQueryChange: (String) -> Unit,
    onSelectMuscle: (MuscleGroup?) -> Unit,
    onSelectEquipment: (EquipmentType?) -> Unit,
    onClearFilters: () -> Unit,
    onToggleExercise: (Exercise) -> Unit,
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
                    text = stringResource(id = R.string.routine_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.routine_picker_done,
                            selectedExerciseIds.size,
                        ),
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
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                    )
                },
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
                        val isSelected = exercise.id in selectedExerciseIds
                        val thumbUri = exercise.frameAssetUri(0)
                        Surface(
                            onClick = { onToggleExercise(exercise) },
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
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

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Rounded.CheckCircle
                                    } else {
                                        Icons.Rounded.AddCircleOutline
                                    },
                                    contentDescription = if (isSelected) {
                                        stringResource(id = R.string.routine_picker_added)
                                    } else {
                                        stringResource(id = R.string.routine_editor_add_exercise)
                                    },
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
