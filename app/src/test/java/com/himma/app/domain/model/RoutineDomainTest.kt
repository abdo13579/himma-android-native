package com.himma.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineDomainTest {

    private fun sampleExercise(id: String, muscle: MuscleGroup, restSeconds: Int = 90) = Exercise(
        id = id,
        nameEn = "Sample $id",
        nameAr = "تمرين $id",
        primaryMuscle = muscle,
        secondaryMuscles = emptyList(),
        equipment = EquipmentType.BARBELL,
        instructionsEn = "",
        instructionsAr = "",
        localMediaKey = id,
        defaultRestSeconds = restSeconds,
        isCustom = false,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `draft normalized clamps sets, reps range, and rest bounds`() {
        val raw = RoutineExerciseDraft(
            itemId = "item-1",
            exercise = sampleExercise("bench", MuscleGroup.CHEST),
            targetSets = 99,
            targetRepsMin = 15,
            targetRepsMax = 8,
            targetRestSeconds = 5,
        )

        val normalized = raw.normalized()

        assertEquals(RoutineExercise.MAX_SETS, normalized.targetSets)
        assertEquals(15, normalized.targetRepsMin)
        assertEquals(15, normalized.targetRepsMax)
        assertEquals(RoutineExercise.MIN_REST_SECONDS, normalized.targetRestSeconds)
        assertEquals("15", normalized.formattedRepsRange())
    }

    @Test
    fun `routine aggregates totalTargetSets and distinct primaryMuscles`() {
        val routine = Routine(
            id = "r1",
            title = "Push Day",
            notes = "",
            orderIndex = 0,
            createdAt = 1L,
            updatedAt = 1L,
            exercises = listOf(
                RoutineExercise(
                    id = "re1",
                    routineId = "r1",
                    exercise = sampleExercise("bench", MuscleGroup.CHEST),
                    orderIndex = 0,
                    targetSets = 4,
                    targetRepsMin = 6,
                    targetRepsMax = 8,
                    targetRestSeconds = 120,
                ),
                RoutineExercise(
                    id = "re2",
                    routineId = "r1",
                    exercise = sampleExercise("fly", MuscleGroup.CHEST),
                    orderIndex = 1,
                    targetSets = 3,
                    targetRepsMin = 10,
                    targetRepsMax = 12,
                    targetRestSeconds = 90,
                ),
                RoutineExercise(
                    id = "re3",
                    routineId = "r1",
                    exercise = sampleExercise("pushdown", MuscleGroup.ARMS),
                    orderIndex = 2,
                    targetSets = 3,
                    targetRepsMin = 12,
                    targetRepsMax = 15,
                    targetRestSeconds = 60,
                ),
            ),
        )

        assertEquals(10, routine.totalTargetSets)
        assertEquals(listOf(MuscleGroup.CHEST, MuscleGroup.ARMS), routine.primaryMuscles)
        assertEquals("6–8", routine.exercises.first().formattedRepsRange())
    }
}
