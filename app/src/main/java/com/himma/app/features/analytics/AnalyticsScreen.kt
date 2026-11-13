package com.himma.app.features.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himma.app.R
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.InsightType
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.PersonalRecord
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WorkoutHistorySummary
import com.himma.app.domain.model.WorkoutMath
import com.himma.app.domain.model.WorkoutSession
import com.himma.app.domain.text.ArabicTextNormalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isArabic = state.preferences.language == AppLanguage.ARABIC
    val unit = state.preferences.weightUnit
    val unitLabel = when (unit) {
        WeightUnit.KG -> stringResource(id = R.string.unit_kg)
        WeightUnit.LBS -> stringResource(id = R.string.unit_lbs)
    }

    var showExercisePicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                AnalyticsEvent.BodyWeightLogged -> {
                    onShowMessage(context.getString(R.string.analytics_weight_logged_msg))
                }
                AnalyticsEvent.BodyWeightDeleted -> {
                    onShowMessage(context.getString(R.string.analytics_weight_deleted_msg))
                }
            }
        }
    }

    state.selectedWorkoutDetail?.let { detailSession ->
        ModalBottomSheet(
            onDismissRequest = viewModel::closeWorkoutDetail,
        ) {
            WorkoutDetailSheetContent(
                session = detailSession,
                unit = unit,
                unitLabel = unitLabel,
                isArabic = isArabic,
            )
        }
    }

    if (showExercisePicker) {
        ModalBottomSheet(
            onDismissRequest = { showExercisePicker = false },
        ) {
            ExercisePickerSheetContent(
                exercises = state.allExercises,
                isArabic = isArabic,
                onSelect = { exercise ->
                    viewModel.selectExerciseForProgression(exercise.id)
                    showExercisePicker = false
                },
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            edgePadding = 12.dp,
        ) {
            AnalyticsTab.entries.forEach { tab ->
                val labelRes = when (tab) {
                    AnalyticsTab.HISTORY -> R.string.analytics_tab_history
                    AnalyticsTab.PR_LEDGER -> R.string.analytics_tab_prs
                    AnalyticsTab.PROGRESSION -> R.string.analytics_tab_progression
                    AnalyticsTab.BODY_WEIGHT -> R.string.analytics_tab_body_weight
                }
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = stringResource(id = labelRes),
                            maxLines = 1,
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }

        when (state.selectedTab) {
            AnalyticsTab.HISTORY -> HistoryTabContent(
                history = state.workoutHistory,
                unit = unit,
                unitLabel = unitLabel,
                onWorkoutClick = viewModel::openWorkoutDetail,
            )

            AnalyticsTab.PR_LEDGER -> PrLedgerTabContent(
                records = state.personalRecords,
                unit = unit,
                unitLabel = unitLabel,
                isArabic = isArabic,
                onPrClick = { pr ->
                    viewModel.selectExerciseForProgression(pr.exerciseId)
                },
            )

            AnalyticsTab.PROGRESSION -> ProgressionTabContent(
                state = state,
                unit = unit,
                unitLabel = unitLabel,
                isArabic = isArabic,
                onPickExercise = { showExercisePicker = true },
                onModeChange = viewModel::setProgressionMode,
            )

            AnalyticsTab.BODY_WEIGHT -> BodyWeightTabContent(
                state = state,
                unit = unit,
                unitLabel = unitLabel,
                onInputChange = viewModel::updateBodyWeightInput,
                onLogWeight = viewModel::logBodyWeight,
                onRangeChange = viewModel::setBodyWeightDays,
                onDeleteEntry = viewModel::deleteBodyWeight,
            )
        }
    }
}

@Composable
private fun HistoryTabContent(
    history: List<WorkoutHistorySummary>,
    unit: WeightUnit,
    unitLabel: String,
    onWorkoutClick: (String) -> Unit,
) {
    if (history.isEmpty()) {
        EmptyAnalyticsState(
            icon = Icons.Outlined.History,
            message = stringResource(id = R.string.analytics_history_empty),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(history, key = { it.id }) { workout ->
            ElevatedCard(
                onClick = { onWorkoutClick(workout.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workout.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatShortDateTime(workout.startTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (workout.prCount > 0) {
                            AssistChip(
                                onClick = { onWorkoutClick(workout.id) },
                                label = {
                                    Text(
                                        text = stringResource(
                                            id = R.string.analytics_pr_badge,
                                            workout.prCount,
                                        ),
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val displayVol = WeightUnit.toDisplayWeight(workout.totalVolumeKg, unit)
                        Text(
                            text = "${WorkoutMath.formatTimer(workout.durationSeconds)} • $displayVol $unitLabel",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(id = R.string.analytics_sets_count, workout.completedSetsCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrLedgerTabContent(
    records: List<PersonalRecord>,
    unit: WeightUnit,
    unitLabel: String,
    isArabic: Boolean,
    onPrClick: (PersonalRecord) -> Unit,
) {
    if (records.isEmpty()) {
        EmptyAnalyticsState(
            icon = Icons.Rounded.EmojiEvents,
            message = stringResource(id = R.string.analytics_prs_empty),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(records, key = { it.id }) { pr ->
            val name = if (isArabic) {
                pr.exerciseNameAr.ifBlank { pr.exerciseNameEn }
            } else {
                pr.exerciseNameEn.ifBlank { pr.exerciseNameAr }
            }

            ElevatedCard(
                onClick = { onPrClick(pr) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = formatShortDate(pr.achievedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        MetricColumn(
                            label = stringResource(id = R.string.analytics_metric_max_weight),
                            value = pr.maxWeightKg?.let {
                                "${WeightUnit.toDisplayWeight(it, unit)} $unitLabel"
                            } ?: "—",
                        )
                        MetricColumn(
                            label = stringResource(id = R.string.analytics_metric_estimated_1rm),
                            value = pr.estimated1rmKg?.let {
                                "${WeightUnit.toDisplayWeight(it, unit)} $unitLabel"
                            } ?: "—",
                        )
                        MetricColumn(
                            label = stringResource(id = R.string.analytics_metric_max_volume),
                            value = pr.maxVolumeSetKg?.let {
                                "${WeightUnit.toDisplayWeight(it, unit)} $unitLabel"
                            } ?: "—",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressionTabContent(
    state: AnalyticsUiState,
    unit: WeightUnit,
    unitLabel: String,
    isArabic: Boolean,
    onPickExercise: () -> Unit,
    onModeChange: (ProgressionChartMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Exercise Progression Curve Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val exName = state.selectedExercise?.let {
                    if (isArabic) it.nameAr else it.nameEn
                } ?: stringResource(id = R.string.analytics_select_exercise)

                FilledTonalButton(
                    onClick = onPickExercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = exName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    ProgressionChartMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = state.progressionMode == mode,
                            onClick = { onModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = ProgressionChartMode.entries.size,
                            ),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            val labelRes = when (mode) {
                                ProgressionChartMode.ESTIMATED_1RM -> R.string.analytics_chart_1rm_mode
                                ProgressionChartMode.MAX_WEIGHT -> R.string.analytics_chart_weight_mode
                            }
                            Text(stringResource(id = labelRes))
                        }
                    }
                }

                val chartPoints = state.progressionPoints.map { pt ->
                    ChartDataPoint(
                        timestamp = pt.timestamp,
                        displayValue = WeightUnit.toDisplayWeight(pt.value, unit),
                    )
                }

                if (chartPoints.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.analytics_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    TrendLineChart(
                        points = chartPoints,
                        unitSuffix = unitLabel,
                    )
                }
            }
        }

        // Weekly Muscle Volume Distribution Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.analytics_weekly_muscle_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider()
                if (state.muscleDistributions.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.analytics_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MuscleVolumeBarList(
                        distributions = state.muscleDistributions,
                        isArabic = isArabic,
                    )
                }
            }
        }

        // Local Progression & Balance Insights
        if (state.insights.isNotEmpty()) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.analytics_insights_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    HorizontalDivider()
                    state.insights.forEach { insight ->
                        val title = if (isArabic) insight.titleAr else insight.titleEn
                        val desc = if (isArabic) insight.descriptionAr else insight.descriptionEn
                        val tint = when (insight.type) {
                            InsightType.MILESTONE -> MaterialTheme.colorScheme.primary
                            InsightType.MUSCLE_IMBALANCE -> MaterialTheme.colorScheme.tertiary
                            InsightType.PROGRESSIVE_OVERLOAD -> MaterialTheme.colorScheme.secondary
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = tint,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BodyWeightTabContent(
    state: AnalyticsUiState,
    unit: WeightUnit,
    unitLabel: String,
    onInputChange: (String) -> Unit,
    onLogWeight: () -> Unit,
    onRangeChange: (Int) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Log Body Weight Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.bodyWeightEntries.lastOrNull()?.let { latest ->
                    val display = WeightUnit.toDisplayWeight(latest.weightKg, unit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.analytics_body_weight_current),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "$display $unitLabel",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    OutlinedTextField(
                        value = state.bodyWeightInput,
                        onValueChange = onInputChange,
                        label = {
                            Text("${stringResource(id = R.string.onboarding_body_weight_label)} ($unitLabel)")
                        },
                        isError = state.bodyWeightError,
                        supportingText = if (state.bodyWeightError) {
                            { Text(stringResource(id = R.string.validation_invalid_weight)) }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    FilledTonalButton(
                        onClick = onLogWeight,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .heightIn(min = 52.dp),
                    ) {
                        Text(stringResource(id = R.string.analytics_log_weight_cta))
                    }
                }
            }
        }

        // Body Weight Trend Chart Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val ranges = listOf(
                    7 to R.string.analytics_range_7d,
                    30 to R.string.analytics_range_30d,
                    90 to R.string.analytics_range_90d,
                    0 to R.string.analytics_range_all,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ranges.forEach { (days, resId) ->
                        FilterChip(
                            selected = state.bodyWeightDays == days,
                            onClick = { onRangeChange(days) },
                            label = { Text(stringResource(id = resId)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }

                val points = state.bodyWeightEntries.map {
                    ChartDataPoint(
                        timestamp = it.recordedAt,
                        displayValue = WeightUnit.toDisplayWeight(it.weightKg, unit),
                    )
                }

                if (points.isEmpty()) {
                    EmptyAnalyticsState(
                        icon = Icons.Outlined.MonitorWeight,
                        message = stringResource(id = R.string.analytics_body_weight_empty),
                        compact = true,
                    )
                } else {
                    TrendLineChart(
                        points = points,
                        unitSuffix = unitLabel,
                    )
                }
            }
        }

        // Recent Entries List
        if (state.bodyWeightEntries.isNotEmpty()) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.bodyWeightEntries.reversed().forEach { entry ->
                        val display = WeightUnit.toDisplayWeight(entry.weightKg, unit)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "$display $unitLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = formatShortDateTime(entry.recordedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = { onDeleteEntry(entry.id) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutDetailSheetContent(
    session: WorkoutSession,
    unit: WeightUnit,
    unitLabel: String,
    isArabic: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = session.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        val vol = WeightUnit.toDisplayWeight(session.totalVolumeKg, unit)
        Text(
            text = "${formatShortDateTime(session.startTime)} • ${WorkoutMath.formatTimer(session.durationSeconds)} • $vol $unitLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (session.notes.isNotBlank()) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        HorizontalDivider()

        session.exercises.forEach { group ->
            val exName = if (isArabic) group.exercise.nameAr else group.exercise.nameEn
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = exName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                group.sets.filter { it.isCompleted }.forEachIndexed { idx, set ->
                    val w = set.weightKg?.let { WeightUnit.toDisplayWeight(it, unit).toString() } ?: "0"
                    val r = set.reps?.toString() ?: "0"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "#${idx + 1}  •  $w $unitLabel × $r",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (set.isPr) {
                            Text(
                                text = "PR 🏆",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ExercisePickerSheetContent(
    exercises: List<Exercise>,
    isArabic: Boolean,
    onSelect: (Exercise) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = remember(query) { ArabicTextNormalizer.normalize(query) }
    val filtered = remember(exercises, query, normalizedQuery) {
        if (query.isBlank()) {
            exercises
        } else {
            exercises.filter { ex ->
                ex.nameEn.contains(query, ignoreCase = true) ||
                    ex.nameAr.contains(query) ||
                    (normalizedQuery.isNotEmpty() && ArabicTextNormalizer.normalize(ex.nameAr).contains(normalizedQuery))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(id = R.string.exercises_search_placeholder)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filtered, key = { it.id }) { ex ->
                val title = if (isArabic) ex.nameAr else ex.nameEn
                val muscleLabelRes = when (ex.primaryMuscle) {
                    MuscleGroup.CHEST -> R.string.muscle_chest
                    MuscleGroup.BACK -> R.string.muscle_back
                    MuscleGroup.LEGS -> R.string.muscle_legs
                    MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
                    MuscleGroup.ARMS -> R.string.muscle_arms
                    MuscleGroup.CORE -> R.string.muscle_core
                }
                val subtitle = stringResource(id = muscleLabelRes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable { onSelect(ex) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyAnalyticsState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    compact: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.padding(vertical = 24.dp) else Modifier.fillMaxSize().padding(32.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.height(44.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatShortDateTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
}

private fun formatShortDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}
