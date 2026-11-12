package com.himma.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.core.locale.LocaleHelper
import com.himma.app.core.theme.HimmaTheme
import com.himma.app.features.shell.HimmaRootNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        var isReady by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !isReady }

        setContent {
            val prefs by preferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = null,
            )

            prefs?.let { currentPrefs ->
                isReady = true

                LaunchedEffect(currentPrefs.language) {
                    LocaleHelper.applyLanguage(currentPrefs.language)
                }

                HimmaTheme(
                    themeMode = currentPrefs.themeMode,
                    useDynamicColor = currentPrefs.useDynamicColor,
                    language = currentPrefs.language,
                ) {
                    HimmaRootNavHost(
                        onboardingCompleted = currentPrefs.onboardingCompleted,
                    )
                }
            }
        }
    }
}
