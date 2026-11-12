package com.himma.app.core.database

import android.content.Context
import com.himma.app.core.database.entity.ExerciseEntity
import com.himma.app.domain.text.ArabicTextNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SeedExerciseDto(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String,
    val instructionsEn: String = "",
    val instructionsAr: String = "",
    val localMediaKey: String? = null,
    val defaultRestSeconds: Int = 90,
    val isCustom: Boolean = false,
)

@Singleton
class ExerciseSeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadSeedEntities(nowMs: Long = System.currentTimeMillis()): List<ExerciseEntity> {
        val raw = context.assets.open(SEED_ASSET_FILE).bufferedReader().use { it.readText() }
        val entries = json.decodeFromString<List<SeedExerciseDto>>(raw)
        return entries.map { dto ->
            ExerciseEntity(
                id = dto.id,
                nameEn = dto.nameEn,
                nameAr = dto.nameAr,
                nameArNormalized = ArabicTextNormalizer.normalize(dto.nameAr),
                primaryMuscle = dto.primaryMuscle,
                secondaryMuscles = dto.secondaryMuscles.joinToString(","),
                equipment = dto.equipment,
                instructionsEn = dto.instructionsEn,
                instructionsAr = dto.instructionsAr,
                localMediaKey = dto.localMediaKey,
                defaultRestSeconds = dto.defaultRestSeconds,
                isCustom = dto.isCustom,
                createdAt = nowMs,
                updatedAt = nowMs,
            )
        }
    }

    companion object {
        const val SEED_ASSET_FILE = "exercises_seed.json"
    }
}
