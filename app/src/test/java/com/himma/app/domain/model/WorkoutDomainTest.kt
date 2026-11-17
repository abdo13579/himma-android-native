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
    fun `calculateTotalVolumeKg sums only completed sets with positive values and ignores time sets`() {
        val sets = listOf(
            WorkoutSet("1", "w1", "bench", 0, 0, 90, 60.0, 10, isCompleted = true),
            WorkoutSet("2", "w1", "bench", 0, 1, 90, 70.0, 8, isCompleted = true),
            WorkoutSet("3", "w1", "bench", 0, 2, 90, 80.0, 6, isCompleted = false),
            WorkoutSet("4", "w1", "bench", 0, 3, 90, null, 12, isCompleted = true),
            WorkoutSet("5", "w1", "running", 1, 0, 60, weightKg = null, reps = null, durationSeconds = 1200, isCompleted = true),
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

    @Test
    fun `workoutSession progress calculation computes ratio of completed sets`() {
        val exercise = Exercise(
            id = "bench",
            nameEn = "Bench Press",
            nameAr = "بنش برس",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = emptyList(),
            equipment = EquipmentType.BARBELL,
            instructionsEn = "",
            instructionsAr = "",
            localMediaKey = null,
            defaultRestSeconds = 90,
            isCustom = false,
            createdAt = 0L,
            updatedAt = 0L,
        )
        val sets = listOf(
            WorkoutSet("s1", "w1", "bench", 0, 0, 60, 80.0, 10, isCompleted = true),
            WorkoutSet("s2", "w1", "bench", 0, 1, 60, 80.0, 10, isCompleted = true),
            WorkoutSet("s3", "w1", "bench", 0, 2, 60, 80.0, 10, isCompleted = false),
            WorkoutSet("s4", "w1", "bench", 0, 3, 60, 80.0, 10, isCompleted = false),
        )
        val session = WorkoutSession(
            id = "w1",
            routineId = null,
            title = "Chest Day",
            startTime = 1000L,
            endTime = null,
            durationSeconds = 0,
            totalVolumeKg = 1600.0,
            notes = "",
            isCompleted = false,
            exercises = listOf(WorkoutExerciseGroup(exercise, 0, 60, sets)),
        )

        assertEquals(2, session.completedSetsCount)
        assertEquals(4, session.totalSetsCount)
        assertEquals(0.5f, session.progress, 0.001f)
    }

    @Test
    fun `workoutSession progress returns zero when total sets is zero`() {
        val session = WorkoutSession(
            id = "w1",
            routineId = null,
            title = "Empty",
            startTime = 1000L,
            endTime = null,
            durationSeconds = 0,
            totalVolumeKg = 0.0,
            notes = "",
            isCompleted = false,
            exercises = emptyList(),
        )

        assertEquals(0, session.completedSetsCount)
        assertEquals(0, session.totalSetsCount)
        assertEquals(0f, session.progress, 0.001f)
    }
}
