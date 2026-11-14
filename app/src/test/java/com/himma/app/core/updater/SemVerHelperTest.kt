package com.himma.app.core.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerHelperTest {

    @Test
    fun `detects newer patch versions`() {
        assertTrue(SemVerHelper.isNewer("v0.1.2-beta", "0.1.1-beta"))
        assertTrue(SemVerHelper.isNewer("0.1.2-beta", "0.1.1-beta"))
        assertFalse(SemVerHelper.isNewer("0.1.0-beta", "0.1.1-beta"))
        assertFalse(SemVerHelper.isNewer("v0.1.1-beta", "0.1.1-beta"))
    }

    @Test
    fun `detects newer minor and major versions`() {
        assertTrue(SemVerHelper.isNewer("v0.2.0-beta", "0.1.1-beta"))
        assertTrue(SemVerHelper.isNewer("1.0.0", "0.1.1-beta"))
        assertFalse(SemVerHelper.isNewer("0.0.9", "0.1.1-beta"))
    }

    @Test
    fun `stable release is newer than pre-release with same numbers`() {
        assertTrue(SemVerHelper.isNewer("v0.1.1", "0.1.1-beta"))
        assertFalse(SemVerHelper.isNewer("v0.1.1-beta", "0.1.1"))
    }

    @Test
    fun `pre-release number progression`() {
        assertTrue(SemVerHelper.isNewer("0.1.1-beta.2", "0.1.1-beta.1"))
        assertFalse(SemVerHelper.isNewer("0.1.1-beta.1", "0.1.1-beta.2"))
    }
}
