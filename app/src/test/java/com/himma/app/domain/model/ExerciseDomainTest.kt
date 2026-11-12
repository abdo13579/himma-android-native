package com.himma.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseDomainTest {

    private fun sampleExercise(mediaKey: String? = "Barbell_Squat") = Exercise(
        id = "Barbell_Squat",
        nameEn = "Barbell Squat",
        nameAr = "سكوات بالبار",
        primaryMuscle = MuscleGroup.LEGS,
        secondaryMuscles = listOf(MuscleGroup.BACK, MuscleGroup.CORE),
        equipment = EquipmentType.BARBELL,
        instructionsEn = "Step 1\n\nStep 2",
        instructionsAr = "الخطوة 1\nالخطوة 2",
        localMediaKey = mediaKey,
        defaultRestSeconds = 150,
        isCustom = false,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `display names respect active language and fallback cleanly`() {
        val ex = sampleExercise()
        assertEquals("سكوات بالبار", ex.primaryDisplayName(AppLanguage.ARABIC))
        assertEquals("Barbell Squat", ex.secondaryDisplayName(AppLanguage.ARABIC))
        assertEquals("Barbell Squat", ex.primaryDisplayName(AppLanguage.ENGLISH))
        assertEquals("سكوات بالبار", ex.secondaryDisplayName(AppLanguage.ENGLISH))
    }

    @Test
    fun `frameAssetUri returns start and end asset paths or null when absent`() {
        val bundled = sampleExercise("bench-press")
        assertEquals("file:///android_asset/exercises/bench-press/0.webp", bundled.frameAssetUri(0))
        assertEquals("file:///android_asset/exercises/bench-press/1.webp", bundled.frameAssetUri(1))
        assertEquals("file:///android_asset/exercises/bench-press/1.webp", bundled.frameAssetUri(5))

        val custom = sampleExercise(mediaKey = null)
        assertNull(custom.frameAssetUri(0))
    }

    @Test
    fun `instructionsList splits non-empty trimmed lines`() {
        val ex = sampleExercise()
        assertEquals(listOf("Step 1", "Step 2"), ex.instructionsList(AppLanguage.ENGLISH))
        assertEquals(listOf("الخطوة 1", "الخطوة 2"), ex.instructionsList(AppLanguage.ARABIC))
    }

    @Test
    fun `enum parsers handle case-insensitive names and invalid values`() {
        assertEquals(MuscleGroup.CHEST, MuscleGroup.fromNameOrNull("chest"))
        assertNull(MuscleGroup.fromNameOrNull("unknown"))
        assertEquals(EquipmentType.DUMBBELL, EquipmentType.fromNameOrNull(" Dumbbell "))
        assertNull(EquipmentType.fromNameOrNull(null))
    }
}
