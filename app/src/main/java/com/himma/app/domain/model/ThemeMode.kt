package com.himma.app.domain.model

enum class ThemeMode(val key: String) {
    AMOLED_DARK("amoled_dark"),
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system"),
    FROST("frost");

    companion object {
        fun fromKey(key: String?): ThemeMode {
            if (key.equals("liquid_glass", ignoreCase = true)) return FROST
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: AMOLED_DARK
        }
    }
}
