package com.himma.app.domain.model

import java.util.Locale

enum class AppLanguage(val tag: String) {
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ARABIC
        }

        fun defaultFromSystem(locale: Locale = Locale.getDefault()): AppLanguage {
            return if (locale.language.equals("en", ignoreCase = true)) ENGLISH else ARABIC
        }
    }
}
