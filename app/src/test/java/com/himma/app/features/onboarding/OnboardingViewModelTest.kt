package com.himma.app.features.onboarding

import com.himma.app.core.database.UserProfileRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.UserProfile
import com.himma.app.domain.model.WeightUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var fakeProfileRepo: FakeUserProfileRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakePrefsRepo = FakeUserPreferencesRepository()
        fakeProfileRepo = FakeUserProfileRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeOnboarding saves profile in kg when entered in lbs and marks completed`() = runTest {
        val viewModel = OnboardingViewModel(fakePrefsRepo, fakeProfileRepo)
        advanceUntilIdle()

        viewModel.selectLanguage(AppLanguage.ENGLISH)
        viewModel.selectWeightUnit(WeightUnit.LBS)
        viewModel.selectRestSeconds(120)
        viewModel.updateDisplayName("Abdo")
        viewModel.updateBodyWeightInput("220.5")

        viewModel.completeOnboarding()
        advanceUntilIdle()

        val prefs = fakePrefsRepo.preferences.first()
        val profile = fakeProfileRepo.profile.first()

        assertTrue(prefs.onboardingCompleted)
        assertEquals(AppLanguage.ENGLISH, prefs.language)
        assertEquals(WeightUnit.LBS, prefs.weightUnit)
        assertEquals(120, prefs.defaultRestSeconds)
        assertEquals("Abdo", profile?.displayName)
        assertEquals(100.0, profile?.bodyWeightKg ?: 0.0, 0.1)
    }

    @Test
    fun `completeOnboarding flags error on invalid weight and stays incomplete`() = runTest {
        val viewModel = OnboardingViewModel(fakePrefsRepo, fakeProfileRepo)
        advanceUntilIdle()

        viewModel.updateBodyWeightInput("-5")
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.bodyWeightError)
        assertFalse(fakePrefsRepo.preferences.first().onboardingCompleted)
    }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val state = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = state

    override suspend fun completeOnboarding(
        language: AppLanguage,
        weightUnit: WeightUnit,
        defaultRestSeconds: Int,
    ) {
        state.value = state.value.copy(
            language = language,
            weightUnit = weightUnit,
            defaultRestSeconds = defaultRestSeconds,
            onboardingCompleted = true,
        )
    }

    override suspend fun setLanguage(language: AppLanguage) {
        state.value = state.value.copy(language = language)
    }

    override suspend fun setWeightUnit(weightUnit: WeightUnit) {
        state.value = state.value.copy(weightUnit = weightUnit)
    }

    override suspend fun setDefaultRestSeconds(seconds: Int) {
        state.value = state.value.copy(defaultRestSeconds = seconds)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        state.value = state.value.copy(themeMode = themeMode)
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        state.value = state.value.copy(useDynamicColor = enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        state.value = state.value.copy(onboardingCompleted = completed)
    }
}

class FakeUserProfileRepository : UserProfileRepository {
    private val state = MutableStateFlow<UserProfile?>(null)
    override val profile: Flow<UserProfile?> = state

    override suspend fun saveProfile(displayName: String?, bodyWeightKg: Double?) {
        val normalized = displayName?.trim()?.takeIf { it.isNotEmpty() }
        state.value = UserProfile(
            id = 1,
            displayName = normalized,
            bodyWeightKg = bodyWeightKg,
            createdAt = 1L,
            updatedAt = 2L,
        )
    }
}
