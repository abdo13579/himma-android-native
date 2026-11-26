package com.himma.app.features.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himma.app.BuildConfig
import com.himma.app.R
import com.himma.app.core.updater.AppUpdateInfo
import com.himma.app.core.updater.UpdateCheckUiState
import com.himma.app.core.updater.UpdateDownloadState
import java.io.File
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.locale.dayOfWeekFullName
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.BackupValidationError
import com.himma.app.domain.model.CustomExportOptions
import com.himma.app.domain.model.RestoreMode
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeekScheduleHelper
import com.himma.app.domain.model.WeightUnit
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val openLinkErrorMsg = stringResource(id = R.string.settings_open_link_error)
    val openEmailErrorMsg = stringResource(id = R.string.settings_open_email_error)
    val openExternalUrl: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }
            .onFailure { onShowMessage(openLinkErrorMsg) }
    }
    val openFeedbackEmail: () -> Unit = {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, buildFeedbackEmailSubject())
        }
        runCatching { context.startActivity(emailIntent) }
            .onFailure { onShowMessage(openEmailErrorMsg) }
    }

    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingInstallPath by rememberSaveable { mutableStateOf<String?>(null) }
    var handledCompletedPath by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    if (BuildConfig.SUPPORTS_IN_APP_UPDATES) {
        LaunchedEffect(downloadState) {
            val completed = downloadState as? UpdateDownloadState.Completed ?: return@LaunchedEffect
            val path = completed.apkFile.absolutePath
            if (handledCompletedPath == path) return@LaunchedEffect
            handledCompletedPath = path
            if (viewModel.canInstallPackages(context)) {
                viewModel.startApkInstallation(context, completed.apkFile)
                    .onFailure { onShowMessage(openLinkErrorMsg) }
            } else {
                pendingInstallPath = path
                showPermissionDialog = true
            }
        }

        DisposableEffect(lifecycleOwner, pendingInstallPath) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val path = pendingInstallPath
                    if (path != null && viewModel.canInstallPackages(context)) {
                        pendingInstallPath = null
                        val file = File(path)
                        if (file.exists()) {
                            viewModel.startApkInstallation(context, file)
                                .onFailure { onShowMessage(openLinkErrorMsg) }
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportJsonToUri(uri)
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsvToUri(uri)
        }
    }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.inspectRestoreUri(uri)
        }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SettingsEvent.ProfileSaved -> context.getString(R.string.settings_profile_saved)
                SettingsEvent.JsonExported -> context.getString(R.string.backup_export_json_success)
                SettingsEvent.CsvExported -> context.getString(R.string.backup_export_csv_success)
                SettingsEvent.SnapshotCreated -> context.getString(R.string.backup_snapshot_success)
                is SettingsEvent.BackupRestored -> context.getString(
                    R.string.backup_restore_success,
                    event.summary.restoredCustomExercises,
                    event.summary.restoredRoutines,
                    event.summary.restoredWorkouts,
                    event.summary.restoredSets,
                )
                is SettingsEvent.BackupError -> {
                    val resId = when (event.validationError) {
                        BackupValidationError.EMPTY_FILE -> R.string.backup_error_empty
                        BackupValidationError.INVALID_JSON -> R.string.backup_error_invalid_json
                        BackupValidationError.UNSUPPORTED_FORMAT -> R.string.backup_error_unsupported_format
                        BackupValidationError.UNSUPPORTED_VERSION -> R.string.backup_error_unsupported_version
                        BackupValidationError.CORRUPT_STRUCTURE -> R.string.backup_error_corrupt
                        null -> R.string.backup_error_io
                    }
                    context.getString(resId)
                }
            }
            onShowMessage(message)
        }
    }

    state.pendingRestore?.let { inspection ->
        val preview = inspection.preview
        val formattedDate = formatTimestamp(preview.exportedAt)
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingRestore,
            icon = {
                Icon(imageVector = Icons.Outlined.Restore, contentDescription = null)
            },
            title = {
                Text(stringResource(id = R.string.backup_restore_dialog_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            id = R.string.backup_restore_dialog_meta,
                            formattedDate,
                            preview.appVersion,
                            preview.schemaVersion,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            id = R.string.backup_restore_dialog_counts,
                            preview.customExerciseCount,
                            preview.routineCount,
                            preview.workoutCount,
                            preview.setCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.backup_restore_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = { viewModel.confirmRestore(RestoreMode.MERGE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(id = R.string.backup_restore_merge))
                    }
                    OutlinedButton(
                        onClick = { viewModel.confirmRestore(RestoreMode.REPLACE) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(id = R.string.backup_restore_replace))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::dismissPendingRestore) {
                    Text(stringResource(id = R.string.exercise_cancel))
                }
            },
        )
    }

    if (state.showCustomExportDialog) {
        CustomExportDialog(
            options = state.customExportOptions,
            onOptionsChange = viewModel::updateCustomExportOptions,
            onDismiss = viewModel::dismissCustomExportDialog,
            onConfirmExport = {
                viewModel.dismissCustomExportDialog()
                val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                exportJsonLauncher.launch("himma-backup-$date.himma.json")
            },
        )
    }

    if (BuildConfig.SUPPORTS_IN_APP_UPDATES) {
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.update_permission_required_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.update_permission_required_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            viewModel.openInstallPermissionSettings(context)
                                .onFailure { onShowMessage(openLinkErrorMsg) }
                        },
                    ) {
                        Text(stringResource(id = R.string.update_permission_grant))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text(stringResource(id = R.string.exercise_cancel))
                    }
                },
            )
        }

        if (showUpdateDialog && updateCheckState is UpdateCheckUiState.UpdateAvailable) {
            val info = (updateCheckState as UpdateCheckUiState.UpdateAvailable).info
            AppUpdateDialog(
                info = info,
                downloadState = downloadState,
                onDownloadClick = { viewModel.startDownload(info) },
                onInstallClick = { file ->
                    if (viewModel.canInstallPackages(context)) {
                        viewModel.startApkInstallation(context, file)
                            .onFailure { onShowMessage(openLinkErrorMsg) }
                    } else {
                        pendingInstallPath = file.absolutePath
                        showPermissionDialog = true
                    }
                },
                onDismiss = viewModel::dismissUpdateDialog,
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Local Profile Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_profile),
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = state.displayNameInput,
                    onValueChange = viewModel::updateDisplayName,
                    label = { Text(stringResource(id = R.string.onboarding_name_label)) },
                    placeholder = { Text(stringResource(id = R.string.onboarding_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.bodyWeightInput,
                    onValueChange = viewModel::updateBodyWeightInput,
                    label = {
                        val unitText = when (state.preferences.weightUnit) {
                            WeightUnit.KG -> stringResource(id = R.string.unit_kg)
                            WeightUnit.LBS -> stringResource(id = R.string.unit_lbs)
                        }
                        Text("${stringResource(id = R.string.onboarding_body_weight_label)} ($unitText)")
                    },
                    placeholder = { Text(stringResource(id = R.string.onboarding_body_weight_placeholder)) },
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
                    modifier = Modifier.fillMaxWidth(),
                )

                FilledTonalButton(
                    onClick = viewModel::saveProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.settings_save_profile))
                }
            }
        }

        // Training Preferences Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_preferences),
                    style = MaterialTheme.typography.titleMedium,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_language_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        AppLanguage.entries.forEachIndexed { index, lang ->
                            SegmentedButton(
                                selected = state.preferences.language == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = AppLanguage.entries.size,
                                ),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = when (lang) {
                                        AppLanguage.ARABIC -> stringResource(id = R.string.language_ar)
                                        AppLanguage.ENGLISH -> stringResource(id = R.string.language_en)
                                    },
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_unit_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        WeightUnit.entries.forEachIndexed { index, unit ->
                            SegmentedButton(
                                selected = state.preferences.weightUnit == unit,
                                onClick = { viewModel.setWeightUnit(unit) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = WeightUnit.entries.size,
                                ),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = when (unit) {
                                        WeightUnit.KG -> stringResource(id = R.string.unit_kg)
                                        WeightUnit.LBS -> stringResource(id = R.string.unit_lbs)
                                    },
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_rest_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UserPreferences.REST_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = state.preferences.defaultRestSeconds == preset,
                                onClick = { viewModel.setDefaultRestSeconds(preset) },
                                label = {
                                    Text(stringResource(id = R.string.seconds_short, preset))
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_week_start_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WeekScheduleHelper.orderedDaysStartingFrom(DayOfWeek.SATURDAY).forEach { day ->
                            FilterChip(
                                selected = state.preferences.weekStartDay == day,
                                onClick = { viewModel.setWeekStartDay(day) },
                                label = { Text(dayOfWeekFullName(day)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
            }
        }

        // Material 3 Appearance Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_appearance),
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = stringResource(id = R.string.settings_theme_label),
                    style = MaterialTheme.typography.titleSmall,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.preferences.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = {
                                val labelRes = when (mode) {
                                    ThemeMode.AMOLED_DARK -> R.string.theme_amoled_dark
                                    ThemeMode.DARK -> R.string.theme_dark
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.FROST -> R.string.theme_frost
                                }
                                Text(stringResource(id = labelRes))
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_animations_label),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_animations_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.preferences.animationsEnabled,
                        onCheckedChange = viewModel::setAnimationsEnabled,
                    )
                }
            }
        }

        // Local Backup, Restore & Data Portability Card (Spec 5)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_backup),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(id = R.string.settings_backup_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.isBackupBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.openCustomExportDialog()
                    },
                    enabled = !state.isBackupBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Backup,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(id = R.string.backup_export_json))
                }

                OutlinedButton(
                    onClick = {
                        restoreJsonLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    enabled = !state.isBackupBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(id = R.string.backup_restore_json))
                }

                OutlinedButton(
                    onClick = {
                        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        exportCsvLauncher.launch("himma-workouts-$date.csv")
                    },
                    enabled = !state.isBackupBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TableChart,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(id = R.string.backup_export_csv))
                }
            }
        }

        // App Updates Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_updates_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (!BuildConfig.SUPPORTS_IN_APP_UPDATES) {
                    Text(
                        text = stringResource(id = R.string.settings_updates_managed_by_store),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.settings_updates_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_auto_check_updates),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(id = R.string.settings_auto_check_updates_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.preferences.autoCheckUpdates,
                            onCheckedChange = viewModel::setAutoCheckUpdates,
                        )
                    }

                    FilledTonalButton(
                        onClick = viewModel::checkForUpdates,
                        enabled = updateCheckState !is UpdateCheckUiState.Checking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        if (updateCheckState is UpdateCheckUiState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.settings_checking_updates))
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.settings_check_updates))
                        }
                    }

                    when (val check = updateCheckState) {
                        is UpdateCheckUiState.UpToDate -> {
                            Text(
                                text = stringResource(id = R.string.settings_up_to_date, check.currentVersion),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        is UpdateCheckUiState.UpdateAvailable -> {
                            Text(
                                text = stringResource(id = R.string.settings_update_available, check.info.remoteVersion),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        is UpdateCheckUiState.Error -> {
                            Text(
                                text = check.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }

        // About & GitHub Links Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(id = R.string.settings_section_about),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(
                    text = stringResource(id = R.string.settings_about_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FilledTonalButton(
                    onClick = { openExternalUrl(GITHUB_REPO_URL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(id = R.string.settings_view_github_repo))
                }

                OutlinedButton(
                    onClick = { openExternalUrl(PRIVACY_POLICY_URL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(id = R.string.settings_privacy_policy))
                }

                OutlinedButton(
                    onClick = openFeedbackEmail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Feedback,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(id = R.string.settings_send_feedback))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private const val DEVELOPER_EMAIL = "abdulrahman.a.alfeel@gmail.com"
private const val GITHUB_REPO_URL = "https://github.com/abdo13579/himma-android-native"
private const val PRIVACY_POLICY_URL =
    "https://github.com/abdo13579/himma-android-native/blob/main/PRIVACY_POLICY.md"

/** Builds an email subject containing app and device details for user feedback. */
private fun buildFeedbackEmailSubject(): String {
    val manufacturer = Build.MANUFACTURER.trim()
    val model = Build.MODEL.trim()
    val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model".trim()
    }
    return "Himma Feedback — v${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}) | " +
        "$deviceName | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}

@Composable
private fun AppUpdateDialog(
    info: AppUpdateInfo,
    downloadState: UpdateDownloadState,
    onDownloadClick: () -> Unit,
    onInstallClick: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                onDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.update_dialog_title, info.remoteVersion),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (info.releaseTitle.isNotBlank()) {
                    Text(
                        text = info.releaseTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }

                if (info.releaseNotes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                val sizeMb = info.apkSizeBytes / (1024.0 * 1024.0)
                if (sizeMb > 0) {
                    Text(
                        text = stringResource(id = R.string.update_dialog_size_mb, sizeMb),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.update_downloading),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            LinearProgressIndicator(
                                progress = { downloadState.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            val downloadedMb = downloadState.downloadedBytes / (1024.0 * 1024.0)
                            val totalMb = downloadState.totalBytes / (1024.0 * 1024.0)
                            Text(
                                text = "%.1f / %.1f MB (%d%%)".format(
                                    java.util.Locale.US,
                                    downloadedMb,
                                    totalMb,
                                    downloadState.progressPercent,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is UpdateDownloadState.Error -> {
                        Text(
                            text = stringResource(id = R.string.update_download_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    is UpdateDownloadState.Completed -> {
                        Text(
                            text = stringResource(id = R.string.update_download_ready),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    UpdateDownloadState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is UpdateDownloadState.Completed -> {
                    Button(
                        onClick = { onInstallClick(downloadState.apkFile) },
                    ) {
                        Text(stringResource(id = R.string.update_dialog_action_install))
                    }
                }
                is UpdateDownloadState.Downloading -> {
                    // Downloading in progress
                }
                else -> {
                    Button(
                        onClick = onDownloadClick,
                    ) {
                        Text(stringResource(id = R.string.update_dialog_action_download))
                    }
                }
            }
        },
        dismissButton = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.update_dialog_action_later))
                }
            }
        },
    )
}

private fun formatTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
}

@Composable
private fun CustomExportDialog(
    options: CustomExportOptions,
    onOptionsChange: (CustomExportOptions) -> Unit,
    onDismiss: () -> Unit,
    onConfirmExport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Backup,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.backup_custom_export_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.backup_custom_export_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExportOptionCheckboxRow(
                    label = stringResource(id = R.string.backup_export_routines),
                    checked = options.exportRoutines,
                    onCheckedChange = { onOptionsChange(options.copy(exportRoutines = it)) },
                )

                ExportOptionCheckboxRow(
                    label = stringResource(id = R.string.backup_export_weekly_schedule),
                    checked = options.exportWeeklySchedule,
                    onCheckedChange = { onOptionsChange(options.copy(exportWeeklySchedule = it)) },
                )

                ExportOptionCheckboxRow(
                    label = stringResource(id = R.string.backup_export_custom_exercises),
                    checked = options.exportCustomExercises,
                    onCheckedChange = { onOptionsChange(options.copy(exportCustomExercises = it)) },
                )

                ExportOptionCheckboxRow(
                    label = stringResource(id = R.string.backup_export_theme_prefs),
                    checked = options.exportPreferences,
                    onCheckedChange = { onOptionsChange(options.copy(exportPreferences = it)) },
                )

                ExportOptionCheckboxRow(
                    label = stringResource(id = R.string.backup_export_history),
                    checked = options.exportWorkouts,
                    onCheckedChange = { onOptionsChange(options.copy(exportWorkouts = it)) },
                )

                ExportOptionCheckboxRow(
                    label = stringResource(id = R.string.backup_export_profile_stats),
                    checked = options.exportProfileAndStats,
                    onCheckedChange = { onOptionsChange(options.copy(exportProfileAndStats = it)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmExport,
                enabled = options.isAnySelected,
            ) {
                Text(
                    text = stringResource(id = R.string.backup_export_action),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun ExportOptionCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
