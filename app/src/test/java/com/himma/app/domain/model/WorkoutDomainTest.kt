package com.himma.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutDomainTest {

    @Test
    fun `resolveRestSeconds follows priority routine then exercise then user default`() {
        assertEquals(150, WorkoutMath.resolveRestSeconds(150, 120, 90))
        assertEquals(120, WorkoutMath.resolveRestSeconds(null, 120, 90))
        assertEquals(90, WorkoutMath.resolveRestSeconds(0, 0, 90))
        assertEquals(15, WorkoutMath.resolveRestSeconds(5, 10, 5))
        assertEquals(600, WorkoutMath.resolveRestSeconds(900, 120, 90))
    }

    @Test
    fun `calculateTotalVolumeKg sums only completed sets with positive values`() {
        val sets = listOf(
            WorkoutSet("1", "w1", "bench", 0, 0, 90, 60.0, 10, isCompleted = true),
            WorkoutSet("2", "w1", "bench", 0, 1, 90, 70.0, 8, isCompleted = true),
            WorkoutSet("3", "w1", "bench", 0, 2, 90, 80.0, 6, isCompleted = false),
            WorkoutSet("4", "w1", "bench", 0, 3, 90, null, 12, isCompleted = true),
        )

        assertEquals(1160.0, WorkoutMath.calculateTotalVolumeKg(sets), 0.001)
    }

    @Test
    fun `formatTimer formats minutes and hours properly`() {
        assertEquals("00:00", WorkoutMath.formatTimer(0))
        assertEquals("01:30", WorkoutMath.formatTimer(90))
        assertEquals("1:05:09", WorkoutMath.formatTimer(3909))
    }

    @Test
    fun `setType parser falls back to NORMAL`() {
        assertEquals(SetType.DROP_SET, SetType.fromNameOrDefault("drop_set"))
        assertEquals(SetType.WARMUP, SetType.fromNameOrDefault("WARMUP"))
        assertEquals(SetType.NORMAL, SetType.fromNameOrDefault("invalid"))
    }
}
