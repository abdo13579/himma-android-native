package com.himma.app.features.settings

import android.net.Uri
import com.himma.app.core.backup.AutoBackupScheduler
import com.himma.app.core.backup.BackupInspection
import com.himma.app.core.backup.BackupRepository
import com.himma.app.core.backup.BackupSerializer
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.BackupPreview
import com.himma.app.domain.model.BackupValidationError
import com.himma.app.domain.model.BackupValidationException
import com.himma.app.domain.model.HimmaBackupDocument
import com.himma.app.domain.model.RestoreMode
import com.himma.app.domain.model.RestoreSummary
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeightUnit
import com.himma.app.features.onboarding.FakeUserPreferencesRepository
import com.himma.app.features.onboarding.FakeUserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var fakeProfileRepo: FakeUserProfileRepository
    private lateinit var fakeBackupRepo: FakeBackupRepository
    private lateinit var fakeScheduler: FakeAutoBackupScheduler

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakePrefsRepo = FakeUserPreferencesRepository()
        fakeProfileRepo = FakeUserProfileRepository()
        fakeBackupRepo = FakeBackupRepository()
        fakeScheduler = FakeAutoBackupScheduler()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings updates theme, language, rest timer, and saves profile`() = runTest {
        val viewModel = SettingsViewModel(
            fakePrefsRepo,
            fakeProfileRepo,
            fakeBackupRepo,
            fakeScheduler,
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setUseDynamicColor(true)
        viewModel.setDefaultRestSeconds(180)
        viewModel.setWeightUnit(WeightUnit.KG)
        viewModel.updateDisplayName("Karim")
        viewModel.updateBodyWeightInput("82.5")
        viewModel.saveProfile()
        advanceUntilIdle()

        val prefs = fakePrefsRepo.preferences.first()
        val profile = fakeProfileRepo.profile.first()

        assertEquals(AppLanguage.ENGLISH, prefs.language)
        assertEquals(ThemeMode.LIGHT, prefs.themeMode)
        assertTrue(prefs.useDynamicColor)
        assertEquals(180, prefs.defaultRestSeconds)
        assertEquals("Karim", profile?.displayName)
        assertEquals(82.5, profile?.bodyWeightKg ?: 0.0, 0.01)

        viewModel.resetOnboarding()
        advanceUntilIdle()
        assertFalse(fakePrefsRepo.preferences.first().onboardingCompleted)

        collector.cancel()
    }

    @Test
    fun `auto backup toggle, manual snapshot, and restore inspection work cleanly`() = runTest {
        val viewModel = SettingsViewModel(
            fakePrefsRepo,
            fakeProfileRepo,
            fakeBackupRepo,
            fakeScheduler,
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val events = mutableListOf<SettingsEvent>()
        val eventCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }
        advanceUntilIdle()

        viewModel.setAutoBackupEnabled(false)
        advanceUntilIdle()
        assertFalse(fakePrefsRepo.preferences.first().autoBackupEnabled)
        assertEquals(1, fakeScheduler.cancelCount)

        viewModel.setAutoBackupEnabled(true)
        advanceUntilIdle()
        assertTrue(fakePrefsRepo.preferences.first().autoBackupEnabled)
        assertEquals(1, fakeScheduler.scheduleCount)

        viewModel.triggerManualAutoSnapshot()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasLocalAutoSnapshot)
        assertTrue(events.contains(SettingsEvent.SnapshotCreated))

        viewModel.inspectAutoSnapshotForRestore()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.pendingRestore)

        viewModel.confirmRestore(RestoreMode.MERGE)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.pendingRestore)
        assertEquals(RestoreMode.MERGE, fakeBackupRepo.lastRestoreMode)
        assertTrue(events.any { it is SettingsEvent.BackupRestored })

        collector.cancel()
        eventCollector.cancel()
    }
}

private class FakeAutoBackupScheduler : AutoBackupScheduler {
    var scheduleCount = 0
    var cancelCount = 0

    override fun scheduleDailySnapshot() {
        scheduleCount++
    }

    override fun cancelDailySnapshot() {
        cancelCount++
    }
}

private class FakeBackupRepository : BackupRepository {
    private var snapshotExists = false
    var lastRestoreMode: RestoreMode? = null

    private val sampleDoc = HimmaBackupDocument(
        exportedAt = 1_700_000_000_000L,
        appVersion = "1.0.0",
    )

    override suspend fun createBackupJson(): String = BackupSerializer.encode(sampleDoc)

    override suspend fun createCsvExport(): String = "workout_id\n"

    override suspend fun exportBackupToUri(uri: Uri): Result<Unit> = Result.success(Unit)

    override suspend fun exportCsvToUri(uri: Uri): Result<Unit> = Result.success(Unit)

    override suspend fun inspectBackupUri(uri: Uri): Result<BackupInspection> {
        return Result.success(
            BackupInspection(
                document = sampleDoc,
                preview = BackupPreview(
                    exportedAt = sampleDoc.exportedAt,
                    appVersion = sampleDoc.appVersion,
                    schemaVersion = 1,
                    hasProfile = false,
                    customExerciseCount = 0,
                    routineCount = 0,
                    workoutCount = 0,
                    setCount = 0,
                ),
            ),
        )
    }

    override suspend fun inspectLatestAutoSnapshot(): Result<BackupInspection> {
        return if (!snapshotExists) {
            Result.failure(BackupValidationException(BackupValidationError.EMPTY_FILE))
        } else {
            Result.success(
                BackupInspection(
                    document = sampleDoc,
                    preview = BackupSerializer.preview(sampleDoc),
                ),
            )
        }
    }

    override fun inspectBackupJson(rawJson: String): Result<BackupInspection> {
        return BackupSerializer.decodeAndValidate(rawJson).map {
            BackupInspection(it, BackupSerializer.preview(it))
        }
    }

    override suspend fun restoreBackup(
        document: HimmaBackupDocument,
        mode: RestoreMode,
    ): Result<RestoreSummary> {
        lastRestoreMode = mode
        return Result.success(
            RestoreSummary(
                mode = mode,
                restoredCustomExercises = 1,
                restoredRoutines = 2,
                restoredWorkouts = 3,
                restoredSets = 12,
            ),
        )
    }

    override suspend fun createLocalAutoSnapshot(): Result<Long> {
        snapshotExists = true
        return Result.success(1_700_000_000_000L)
    }

    override fun hasLocalAutoSnapshot(): Boolean = snapshotExists
}
