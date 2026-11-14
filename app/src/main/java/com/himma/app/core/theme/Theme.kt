package com.himma.app.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.ThemeMode

@Composable
fun HimmaTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED_DARK,
    useDynamicColor: Boolean = false,
    language: AppLanguage = AppLanguage.ARABIC,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.AMOLED_DARK, ThemeMode.DARK, ThemeMode.FROST -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isDark && themeMode == ThemeMode.AMOLED_DARK) {
                base.copy(background = AmoledBlack, surface = AmoledBlack)
            } else {
                base
            }
        }
        themeMode == ThemeMode.FROST -> HimmaFrostColorScheme
        themeMode == ThemeMode.AMOLED_DARK -> HimmaAmoledDarkColorScheme
        isDark -> HimmaDarkColorScheme
        else -> HimmaLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    val layoutDirection = when (language) {
        AppLanguage.ARABIC -> LayoutDirection.Rtl
        AppLanguage.ENGLISH -> LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = himmaTypography(language),
            shapes = HimmaShapes,
            content = content,
        )
    }
}
