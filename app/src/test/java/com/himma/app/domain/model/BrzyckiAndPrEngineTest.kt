package com.himma.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrzyckiAndPrEngineTest {

    @Test
    fun `brzycki 1RM formula computes accurate values and handles edge cases`() {
        assertEquals(100.0, BrzyckiMath.calculateEstimated1Rm(100.0, 1), 0.01)
        assertEquals(112.5, BrzyckiMath.calculateEstimated1Rm(100.0, 5), 0.1)
        assertEquals(106.7, BrzyckiMath.calculateEstimated1Rm(80.0, 10), 0.1)

        // Clamped safely at > 30 reps (no division by zero or negative)
        assertTrue(BrzyckiMath.calculateEstimated1Rm(50.0, 50) > 50.0)

        // Zero or null inputs return 0.0
        assertEquals(0.0, BrzyckiMath.calculateEstimated1Rm(0.0, 10), 0.001)
        assertEquals(0.0, BrzyckiMath.calculateEstimated1Rm(100.0, 0), 0.001)
        assertEquals(0.0, BrzyckiMath.calculateEstimated1Rm(null, 5), 0.001)
    }

    @Test
    fun `PR detection triggers on higher weight, volume, or estimated 1RM`() {
        // First set ever -> always a PR
        assertTrue(BrzyckiMath.isSetPersonalRecord(weightKg = 100.0, reps = 5, existingPr = null))

        val initialPr = BrzyckiMath.updateRecord(
            existing = null,
            exerciseId = "Barbell_Squat",
            weightKg = 100.0,
            reps = 5, // volume = 500, 1RM = 112.5
            achievedAt = 1000L,
            workoutId = "w1",
        )
        assertEquals(100.0, initialPr.maxWeightKg!!, 0.01)
        assertEquals(500.0, initialPr.maxVolumeSetKg!!, 0.01)
        assertEquals(112.5, initialPr.estimated1rmKg!!, 0.1)

        // Equal or weaker set -> NOT a PR
        assertFalse(BrzyckiMath.isSetPersonalRecord(100.0, 5, initialPr))
        assertFalse(BrzyckiMath.isSetPersonalRecord(90.0, 5, initialPr))

        // Higher reps at same weight -> beats set volume & 1RM -> IS a PR
        assertTrue(BrzyckiMath.isSetPersonalRecord(100.0, 6, initialPr))

        // Heavier weight at lower reps -> beats maxWeight -> IS a PR
        assertTrue(BrzyckiMath.isSetPersonalRecord(105.0, 2, initialPr))

        // Merge heavier weight + check max retention
        val updated = BrzyckiMath.updateRecord(
            existing = initialPr,
            exerciseId = "Barbell_Squat",
            weightKg = 105.0,
            reps = 2, // volume = 210 (< 500), 1RM = 108.0 (< 112.5)
            achievedAt = 2000L,
            workoutId = "w2",
        )
        assertEquals(105.0, updated.maxWeightKg!!, 0.01)
        assertEquals(500.0, updated.maxVolumeSetKg!!, 0.01) // keeps higher historical volume
        assertEquals(112.5, updated.estimated1rmKg!!, 0.1) // keeps higher historical 1RM
    }

    @Test
    fun `local insights detect muscle imbalance, progressive overload, and PR milestones`() {
        val dist = listOf(
            MuscleVolumeDistribution(MuscleGroup.CHEST, completedSets = 12, totalVolumeKg = 5000.0, percentageOfSets = 60.0),
            MuscleVolumeDistribution(MuscleGroup.BACK, completedSets = 8, totalVolumeKg = 4000.0, percentageOfSets = 35.0),
            MuscleVolumeDistribution(MuscleGroup.LEGS, completedSets = 1, totalVolumeKg = 200.0, percentageOfSets = 5.0),
        )
        val pr = PersonalRecord(
            id = "pr1",
            exerciseId = "bench",
            exerciseNameAr = "بنش بار",
            exerciseNameEn = "Bench Press",
            maxWeightKg = 100.0,
            maxVolumeSetKg = 800.0,
            estimated1rmKg = 124.2,
            achievedAt = 1000L,
        )
        val benchEx = Exercise(
            id = "bench",
            nameEn = "Bench Press",
            nameAr = "بنش بار",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = emptyList(),
            equipment = EquipmentType.BARBELL,
            instructionsEn = "",
            instructionsAr = "",
            localMediaKey = null,
            defaultRestSeconds = 90,
            isCustom = false,
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        val recentSets = listOf(
            WorkoutSet("s1", "w1", "bench", 0, 0, 90, 80.0, 10, isCompleted = true),
            WorkoutSet("s2", "w1", "bench", 0, 1, 90, 80.0, 10, isCompleted = true),
            WorkoutSet("s3", "w1", "bench", 0, 2, 90, 80.0, 10, isCompleted = true),
        )

        val insights = BrzyckiMath.generateInsights(
            distributions = dist,
            recentPrs = listOf(pr),
            topExercisesWithSets = mapOf(benchEx to recentSets),
        )

        assertTrue(insights.any { it.type == InsightType.MUSCLE_IMBALANCE })
        assertTrue(insights.any { it.type == InsightType.PROGRESSIVE_OVERLOAD })
        assertTrue(insights.any { it.type == InsightType.MILESTONE })
    }
}
