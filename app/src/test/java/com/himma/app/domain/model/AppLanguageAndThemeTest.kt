package com.himma.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageAndThemeTest {

    @Test
    fun `AppLanguage fromTag defaults to Arabic for unknown tags`() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag("AR"))
    }

    @Test
    fun `ThemeMode fromKey defaults to AMOLED_DARK`() {
        assertEquals(ThemeMode.AMOLED_DARK, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.AMOLED_DARK, ThemeMode.fromKey("unknown"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("system"))
        assertEquals(ThemeMode.FROST, ThemeMode.fromKey("frost"))
        assertEquals(ThemeMode.FROST, ThemeMode.fromKey("liquid_glass"))
    }
}
