package com.himma.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightUnitTest {

    @Test
    fun `toDisplayWeight converts kg to lbs with 1 decimal rounding`() {
        val lbs = WeightUnit.toDisplayWeight(100.0, WeightUnit.LBS)
        assertEquals(220.5, lbs, 0.01)
    }

    @Test
    fun `toStorageKg converts lbs back to kg`() {
        val kg = WeightUnit.toStorageKg(220.46226, WeightUnit.LBS)
        assertEquals(100.0, kg, 0.01)
    }

    @Test
    fun `parseOptionalPositiveWeight returns null for blank input`() {
        val result = WeightUnit.parseOptionalPositiveWeight("   ")
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `parseOptionalPositiveWeight supports Arabic decimal separator and comma`() {
        val arabicDecimal = WeightUnit.parseOptionalPositiveWeight("75٫5")
        val commaDecimal = WeightUnit.parseOptionalPositiveWeight("75,5")

        assertEquals(75.5, arabicDecimal.getOrThrow()!!, 0.001)
        assertEquals(75.5, commaDecimal.getOrThrow()!!, 0.001)
    }

    @Test
    fun `parseOptionalPositiveWeight rejects zero, negative, and invalid strings`() {
        assertTrue(WeightUnit.parseOptionalPositiveWeight("0").isFailure)
        assertTrue(WeightUnit.parseOptionalPositiveWeight("-12").isFailure)
        assertTrue(WeightUnit.parseOptionalPositiveWeight("abc").isFailure)
        assertTrue(WeightUnit.parseOptionalPositiveWeight("999").isFailure)
    }
}
