package com.himma.app.core.database

import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.MuscleGroup
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSeedAssetsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `seed catalog normalizes muscles and equipment and includes local webp frames`() {
        val assetsDir = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
        ).firstOrNull { File(it, ExerciseSeedLoader.SEED_ASSET_FILE).exists() }
            ?: error("Unable to locate assets directory from ${File(".").absolutePath}")

        val seedFile = File(assetsDir, ExerciseSeedLoader.SEED_ASSET_FILE)
        val entries = json.decodeFromString<List<SeedExerciseDto>>(seedFile.readText())
        assertEquals(302, entries.size)

        entries.forEach { dto ->
            val muscle = MuscleGroup.fromNameOrNull(dto.primaryMuscle)
            assertNotNull("Invalid primaryMuscle for ${dto.id}", muscle)

            val equipment = EquipmentType.fromNameOrNull(dto.equipment)
            assertNotNull("Invalid equipment for ${dto.id}", equipment)
            assertTrue(
                "Equipment ${dto.equipment} for ${dto.id} must be in 5 core types",
                equipment in EquipmentType.coreTypes,
            )

            assertTrue("Invalid exerciseType for ${dto.id}", dto.exerciseType in listOf("REPS", "TIME"))

            assertFalse("Missing localMediaKey for ${dto.id}", dto.localMediaKey.isNullOrBlank())
            val frame0 = File(assetsDir, "exercises/${dto.localMediaKey}/0.webp")
            val frame1 = File(assetsDir, "exercises/${dto.localMediaKey}/1.webp")
            assertTrue("Missing 0.webp for ${dto.id}", frame0.exists() && frame0.length() > 0L)
            assertTrue("Missing 1.webp for ${dto.id}", frame1.exists() && frame1.length() > 0L)
        }
    }

    @Test
    fun `cardio and hold exercises are configured as TIME type`() {
        val assetsDir = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
        ).firstOrNull { File(it, ExerciseSeedLoader.SEED_ASSET_FILE).exists() }
            ?: error("Unable to locate assets directory")

        val seedFile = File(assetsDir, ExerciseSeedLoader.SEED_ASSET_FILE)
        val entries = json.decodeFromString<List<SeedExerciseDto>>(seedFile.readText())

        val byId = entries.associateBy { it.id }
        assertEquals("TIME", byId["running"]?.exerciseType)
        assertEquals("TIME", byId["cycling"]?.exerciseType)
        assertEquals("TIME", byId["walking"]?.exerciseType)
        assertEquals("TIME", byId["rowing"]?.exerciseType)
        assertEquals("TIME", byId["plank"]?.exerciseType)
        assertEquals("TIME", byId["side-plank"]?.exerciseType)
        assertEquals("TIME", byId["dead-hang"]?.exerciseType)

        assertEquals("REPS", byId["bench-press"]?.exerciseType)
        assertEquals("REPS", byId["squat"]?.exerciseType)
        assertEquals("REPS", byId["deadlift"]?.exerciseType)
        assertTrue(
            "deadlift should include LEGS in secondaryMuscles",
            byId["deadlift"]?.secondaryMuscles?.contains("LEGS") == true,
        )

        entries.filter {
            it.id.contains("band", ignoreCase = true) ||
                it.nameEn.contains("band", ignoreCase = true)
        }.forEach { bandExercise ->
            assertFalse(
                "Resistance band ${bandExercise.id} must not be CABLE so stack plates stays hidden",
                bandExercise.equipment == "CABLE",
            )
        }
    }
}
