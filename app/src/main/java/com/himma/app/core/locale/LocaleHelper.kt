package com.himma.app.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.himma.app.domain.model.AppLanguage

object LocaleHelper {
    /**
     * Applies the selected app language via AppCompatDelegate without redundant calls
     * when the active application locale already matches [language].
     */
    fun applyLanguage(language: AppLanguage) {
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (!currentTags.equals(language.tag, ignoreCase = true)) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag),
            )
        }
    }
}
