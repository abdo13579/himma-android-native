package com.himma.app.features.settings

import com.himma.app.domain.model.AppLanguage
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `settings updates theme, language, rest timer, and saves profile`() = runTest {
        val viewModel = SettingsViewModel(fakePrefsRepo, fakeProfileRepo)
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
}
