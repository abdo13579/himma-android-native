package com.himma.app.features.exercises

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.himma.app.R
import com.himma.app.domain.model.Exercise
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    onBackClick: () -> Unit,
    onEditCustomClick: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ExerciseDetailEvent.Deleted -> onDeleted()
            }
        }
    }

    val exercise = state.exercise

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = exercise?.primaryDisplayName(state.language)
                            ?: stringResource(id = R.string.exercise_detail_title),
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
                    if (exercise?.isCustom == true) {
                        IconButton(
                            onClick = { onEditCustomClick(exercise.id) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(id = R.string.exercise_detail_edit),
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(id = R.string.exercise_detail_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
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
            exercise == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(id = R.string.exercises_empty_title))
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Local 2-frame (Start / End) open-source WebP demonstration (omitted for custom exercises)
                    val frame0Uri = exercise.frameAssetUri(0)
                    val frame1Uri = exercise.frameAssetUri(1)
                    if (frame0Uri != null && frame1Uri != null) {
                        ExerciseMediaShowcaseCard(
                            exerciseId = exercise.id,
                            frame0Uri = frame0Uri,
                            frame1Uri = frame1Uri,
                            title = exercise.primaryDisplayName(state.language),
                        )
                    }

                    exercise.secondaryDisplayName(state.language)?.let { altName ->
                        Text(
                            text = altName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Target Muscles, Equipment & Default Rest Card
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Stylized Material 3 Equipment Icon Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (exercise.isCustom) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (exercise.isCustom) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = exercise.equipment.icon(),
                                                contentDescription = null,
                                                tint = if (exercise.isCustom) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSecondary
                                                },
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(id = exercise.equipment.labelRes()),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (exercise.isCustom) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            },
                                        )
                                        if (exercise.isCustom) {
                                            Text(
                                                text = stringResource(id = R.string.exercises_custom_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = stringResource(id = R.string.exercise_detail_primary_muscle),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(stringResource(id = exercise.primaryMuscle.labelRes())) },
                                )
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            "${stringResource(id = R.string.exercise_detail_rest)}: " +
                                                stringResource(
                                                    id = R.string.seconds_short,
                                                    exercise.defaultRestSeconds,
                                                ),
                                        )
                                    },
                                )
                            }

                            if (exercise.secondaryMuscles.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = stringResource(id = R.string.exercise_detail_secondary_muscles),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    exercise.secondaryMuscles.forEach { secondary ->
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(stringResource(id = secondary.labelRes())) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Execution Steps Card
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.exercise_detail_instructions),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            val steps = exercise.instructionsList(state.language)
                            if (steps.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.exercise_detail_no_instructions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                steps.forEachIndexed { index, stepText ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            )
                                        }
                                        Text(
                                            text = stepText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(id = R.string.exercise_delete_title)) },
            text = { Text(stringResource(id = R.string.exercise_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCustomExercise()
                    },
                ) {
                    Text(
                        text = stringResource(id = R.string.exercise_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(id = R.string.exercise_cancel))
                }
            },
        )
    }
}

@Composable
private fun ExerciseMediaShowcaseCard(
    exerciseId: String,
    frame0Uri: String,
    frame1Uri: String,
    title: String,
) {
    val context = LocalContext.current
    var activeFrame by remember(exerciseId) { mutableIntStateOf(0) }
    var isAnimating by remember(exerciseId) { mutableStateOf(true) }

    // Preload both WebP frames into Coil cache for zero-flicker Start <-> End switching
    LaunchedEffect(exerciseId, frame0Uri, frame1Uri) {
        listOf(frame0Uri, frame1Uri).forEach { uri ->
            context.imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(uri)
                    .build(),
            )
        }
    }

    LaunchedEffect(isAnimating, exerciseId) {
        if (isAnimating) {
            while (true) {
                delay(950L)
                activeFrame = if (activeFrame == 0) 1 else 0
            }
        }
    }

    val activeUri = if (activeFrame == 0) frame0Uri else frame1Uri

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0A110E)),
            ) {
                Crossfade(
                    targetState = activeUri,
                    animationSpec = tween(durationMillis = 200),
                    label = "ExerciseTwoFrameLoop",
                ) { uri ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(uri)
                            .crossfade(false)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                ) {
                    Text(
                        text = stringResource(
                            id = if (activeFrame == 0) {
                                R.string.exercise_detail_frame_start
                            } else {
                                R.string.exercise_detail_frame_end
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = { isAnimating = !isAnimating },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isAnimating) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            id = if (isAnimating) R.string.exercise_detail_pause else R.string.exercise_detail_play,
                        ),
                    )
                }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    listOf(
                        0 to R.string.exercise_detail_frame_start,
                        1 to R.string.exercise_detail_frame_end,
                    ).forEachIndexed { index, (frameIdx, labelRes) ->
                        SegmentedButton(
                            selected = activeFrame == frameIdx,
                            onClick = {
                                isAnimating = false
                                activeFrame = frameIdx
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = 2,
                            ),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(
                                text = stringResource(id = labelRes),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
