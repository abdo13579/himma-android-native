package com.himma.app.features.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.backup.AutoBackupScheduler
import com.himma.app.core.backup.BackupInspection
import com.himma.app.core.backup.BackupRepository
import com.himma.app.core.database.UserProfileRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.core.updater.AppUpdateInfo
import com.himma.app.core.updater.AppUpdateRepository
import com.himma.app.core.updater.UpdateCheckUiState
import com.himma.app.core.updater.UpdateDownloadState
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.BackupValidationError
import com.himma.app.domain.model.BackupValidationException
import com.himma.app.domain.model.RestoreMode
import com.himma.app.domain.model.RestoreSummary
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeightUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val displayNameInput: String = "",
    val bodyWeightInput: String = "",
    val bodyWeightError: Boolean = false,
    val isInitialized: Boolean = false,
    val isBackupBusy: Boolean = false,
    val hasLocalAutoSnapshot: Boolean = false,
    val pendingRestore: BackupInspection? = null,
)

sealed interface SettingsEvent {
    data object ProfileSaved : SettingsEvent
    data object JsonExported : SettingsEvent
    data object CsvExported : SettingsEvent
    data object SnapshotCreated : SettingsEvent
    data class BackupRestored(val summary: RestoreSummary) : SettingsEvent
    data class BackupError(val validationError: BackupValidationError?) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val profileRepository: UserProfileRepository,
    private val backupRepository: BackupRepository,
    private val autoBackupScheduler: AutoBackupScheduler,
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {

    private val _updateCheckState = MutableStateFlow<UpdateCheckUiState>(UpdateCheckUiState.Idle)
    val updateCheckState: StateFlow<UpdateCheckUiState> = _updateCheckState

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog

    private data class DraftInputs(
        val displayName: String? = null,
        val bodyWeight: String? = null,
        val bodyWeightError: Boolean = false,
    )

    private data class BackupUiStatus(
        val isBusy: Boolean = false,
        val hasSnapshot: Boolean = false,
        val pendingRestore: BackupInspection? = null,
    )

    private val drafts = MutableStateFlow(DraftInputs())
    private val backupStatus = MutableStateFlow(
        BackupUiStatus(hasSnapshot = backupRepository.hasLocalAutoSnapshot()),
    )
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        profileRepository.profile,
        drafts,
        backupStatus,
    ) { prefs, profile, draft, backup ->
        val defaultWeightText = profile?.bodyWeightKg?.let { kg ->
            WeightUnit.toDisplayWeight(kg, prefs.weightUnit).toString()
        }.orEmpty()

        SettingsUiState(
            preferences = prefs,
            displayNameInput = draft.displayName ?: profile?.displayName.orEmpty(),
            bodyWeightInput = draft.bodyWeight ?: defaultWeightText,
            bodyWeightError = draft.bodyWeightError,
            isInitialized = true,
            isBackupBusy = backup.isBusy,
            hasLocalAutoSnapshot = backup.hasSnapshot || prefs.lastAutoBackupAt != null,
            pendingRestore = backup.pendingRestore,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            preferencesRepository.setWeightUnit(unit)
            drafts.update { it.copy(bodyWeight = null, bodyWeightError = false) }
        }
    }

    fun setDefaultRestSeconds(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultRestSeconds(seconds)
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseDynamicColor(enabled)
        }
    }

    fun updateDisplayName(name: String) {
        drafts.update { it.copy(displayName = name) }
    }

    fun updateBodyWeightInput(input: String) {
        drafts.update { it.copy(bodyWeight = input, bodyWeightError = false) }
    }

    fun saveProfile() {
        val draftSnapshot = drafts.value
        val stateSnapshot = uiState.value
        val rawName = draftSnapshot.displayName ?: stateSnapshot.displayNameInput
        val rawWeight = draftSnapshot.bodyWeight ?: stateSnapshot.bodyWeightInput

        val parsedResult = WeightUnit.parseOptionalPositiveWeight(rawWeight)
        if (parsedResult.isFailure) {
            drafts.update { it.copy(bodyWeightError = true) }
            return
        }

        viewModelScope.launch {
            val activeUnit = preferencesRepository.preferences.first().weightUnit
            val weightKg = parsedResult.getOrNull()?.let { entered ->
                WeightUnit.toStorageKg(entered, activeUnit)
            }
            profileRepository.saveProfile(
                displayName = rawName,
                bodyWeightKg = weightKg,
            )
            drafts.value = DraftInputs()
            _events.emit(SettingsEvent.ProfileSaved)
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoBackupEnabled(enabled)
            if (enabled) {
                autoBackupScheduler.scheduleDailySnapshot()
            } else {
                autoBackupScheduler.cancelDailySnapshot()
            }
        }
    }

    fun exportJsonToUri(uri: Uri) {
        runBackupOperation {
            backupRepository.exportBackupToUri(uri)
                .onSuccess { _events.emit(SettingsEvent.JsonExported) }
                .onFailure { emitError(it) }
        }
    }

    fun exportCsvToUri(uri: Uri) {
        runBackupOperation {
            backupRepository.exportCsvToUri(uri)
                .onSuccess { _events.emit(SettingsEvent.CsvExported) }
                .onFailure { emitError(it) }
        }
    }

    fun inspectRestoreUri(uri: Uri) {
        runBackupOperation {
            backupRepository.inspectBackupUri(uri)
                .onSuccess { inspection ->
                    backupStatus.update { it.copy(pendingRestore = inspection) }
                }
                .onFailure { emitError(it) }
        }
    }

    fun inspectAutoSnapshotForRestore() {
        runBackupOperation {
            backupRepository.inspectLatestAutoSnapshot()
                .onSuccess { inspection ->
                    backupStatus.update { it.copy(pendingRestore = inspection) }
                }
                .onFailure { emitError(it) }
        }
    }

    fun dismissPendingRestore() {
        backupStatus.update { it.copy(pendingRestore = null) }
    }

    fun confirmRestore(mode: RestoreMode) {
        val inspection = backupStatus.value.pendingRestore ?: return
        backupStatus.update { it.copy(pendingRestore = null) }
        runBackupOperation {
            backupRepository.restoreBackup(inspection.document, mode)
                .onSuccess { summary ->
                    drafts.value = DraftInputs()
                    _events.emit(SettingsEvent.BackupRestored(summary))
                }
                .onFailure { emitError(it) }
        }
    }

    fun triggerManualAutoSnapshot() {
        runBackupOperation {
            backupRepository.createLocalAutoSnapshot()
                .onSuccess {
                    backupStatus.update { it.copy(hasSnapshot = true) }
                    _events.emit(SettingsEvent.SnapshotCreated)
                }
                .onFailure { emitError(it) }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckUiState.Checking
            appUpdateRepository.checkForUpdate()
                .onSuccess { info ->
                    if (info.isUpdateAvailable) {
                        _updateCheckState.value = UpdateCheckUiState.UpdateAvailable(info)
                        _showUpdateDialog.value = true
                    } else {
                        _updateCheckState.value = UpdateCheckUiState.UpToDate(info.remoteVersion)
                    }
                }
                .onFailure { error ->
                    _updateCheckState.value = UpdateCheckUiState.Error(
                        error.message ?: "Failed to check for updates"
                    )
                }
        }
    }

    fun startDownload(updateInfo: AppUpdateInfo) {
        val fileName = "himma-v${updateInfo.remoteVersion}.apk"
        viewModelScope.launch {
            _downloadState.value = UpdateDownloadState.Downloading(0, 0, updateInfo.apkSizeBytes)
            appUpdateRepository.downloadUpdate(
                downloadUrl = updateInfo.apkDownloadUrl,
                fileName = fileName,
                onProgress = { percent, downloaded, total ->
                    _downloadState.value = UpdateDownloadState.Downloading(percent, downloaded, total)
                },
            ).onSuccess { file ->
                _downloadState.value = UpdateDownloadState.Completed(file)
            }.onFailure { error ->
                _downloadState.value = UpdateDownloadState.Error(
                    error.message ?: "Download failed"
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        _downloadState.value = UpdateDownloadState.Idle
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoCheckUpdates(enabled)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
        }
    }

    private fun runBackupOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            backupStatus.update { it.copy(isBusy = true) }
            try {
                block()
            } finally {
                backupStatus.update {
                    it.copy(
                        isBusy = false,
                        hasSnapshot = backupRepository.hasLocalAutoSnapshot(),
                    )
                }
            }
        }
    }

    private suspend fun emitError(throwable: Throwable) {
        val validationError = (throwable as? BackupValidationException)?.error
        _events.emit(SettingsEvent.BackupError(validationError))
    }
}
