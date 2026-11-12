package com.himma.app.core.database

import com.himma.app.core.database.dao.ExerciseDao
import com.himma.app.core.database.entity.ExerciseEntity
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.text.ArabicTextNormalizer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface ExerciseRepository {
    suspend fun ensureSeeded()

    fun observeExercises(
        query: String,
        muscleFilter: MuscleGroup?,
        equipmentFilter: EquipmentType?,
    ): Flow<List<Exercise>>

    fun observeExerciseById(id: String): Flow<Exercise?>

    suspend fun getExerciseById(id: String): Exercise?

    suspend fun saveCustomExercise(
        existingId: String?,
        nameAr: String,
        nameEn: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscles: List<MuscleGroup>,
        equipment: EquipmentType,
        defaultRestSeconds: Int,
        instructionsAr: String,
        instructionsEn: String,
    ): String

    suspend fun deleteCustomExercise(id: String)
}

@Singleton
class RoomExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val seedLoader: ExerciseSeedLoader,
) : ExerciseRepository {

    private val seedMutex = Mutex()
    @Volatile
    private var seededChecked = false

    override suspend fun ensureSeeded() {
        if (seededChecked) return
        seedMutex.withLock {
            if (seededChecked) return
            withContext(Dispatchers.IO) {
                val entities = seedLoader.loadSeedEntities()
                if (exerciseDao.countSeedExercises() < entities.size) {
                    exerciseDao.insertAllIgnore(entities)
                }
                val needingNormalization = exerciseDao.getExercisesNeedingNormalization()
                if (needingNormalization.isNotEmpty()) {
                    val patched = needingNormalization.map { row ->
                        val normalizedEquipment = when {
                            row.id == "One-Arm_Kettlebell_Swings" -> EquipmentType.DUMBBELL.name
                            row.equipment == EquipmentType.OTHER.name -> EquipmentType.BODYWEIGHT.name
                            else -> row.equipment
                        }
                        row.copy(
                            nameArNormalized = ArabicTextNormalizer.normalize(row.nameAr),
                            equipment = normalizedEquipment,
                        )
                    }
                    exerciseDao.upsertAll(patched)
                }
            }
            seededChecked = true
        }
    }

    override fun observeExercises(
        query: String,
        muscleFilter: MuscleGroup?,
        equipmentFilter: EquipmentType?,
    ): Flow<List<Exercise>> {
        val trimmed = query.trim()
        val normalizedArabic = ArabicTextNormalizer.normalize(trimmed)
        return exerciseDao.observeExercises(
            query = trimmed,
            normalizedQuery = normalizedArabic,
            muscleFilter = muscleFilter?.name,
            equipmentFilter = equipmentFilter?.name,
        ).map { list -> list.map { it.toDomain() } }
    }

    override fun observeExerciseById(id: String): Flow<Exercise?> {
        return exerciseDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun getExerciseById(id: String): Exercise? {
        return exerciseDao.getByIdOnce(id)?.toDomain()
    }

    override suspend fun saveCustomExercise(
        existingId: String?,
        nameAr: String,
        nameEn: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscles: List<MuscleGroup>,
        equipment: EquipmentType,
        defaultRestSeconds: Int,
        instructionsAr: String,
        instructionsEn: String,
    ): String {
        val now = System.currentTimeMillis()
        val existing = existingId?.let { exerciseDao.getByIdOnce(it) }
        val resolvedId = existing?.id ?: "custom_${UUID.randomUUID()}"
        val trimmedAr = nameAr.trim()
        val trimmedEn = nameEn.trim()
        val finalAr = trimmedAr.ifEmpty { trimmedEn }
        val finalEn = trimmedEn.ifEmpty { trimmedAr }
        val sanitizedSecondary = secondaryMuscles.filter { it != primaryMuscle }.distinct()

        exerciseDao.upsert(
            ExerciseEntity(
                id = resolvedId,
                nameEn = finalEn,
                nameAr = finalAr,
                nameArNormalized = ArabicTextNormalizer.normalize(finalAr),
                primaryMuscle = primaryMuscle.name,
                secondaryMuscles = sanitizedSecondary.joinToString(",") { it.name },
                equipment = equipment.name,
                instructionsEn = instructionsEn.trim(),
                instructionsAr = instructionsAr.trim(),
                localMediaKey = existing?.localMediaKey,
                defaultRestSeconds = defaultRestSeconds.coerceIn(15, 600),
                isCustom = true,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        return resolvedId
    }

    override suspend fun deleteCustomExercise(id: String) {
        exerciseDao.deleteCustomById(id)
    }
}
