package com.himma.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.himma.app.core.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

data class SeedExerciseNameRow(
    val id: String,
    val nameAr: String,
    val equipment: String,
    val secondaryMuscles: String,
)

data class SeedExerciseNameUpdate(
    val id: String,
    val nameAr: String,
    val nameArNormalized: String,
    val equipment: String,
    val secondaryMuscles: String,
    val updatedAt: Long,
)

@Dao
interface ExerciseDao {

    @Query(
        """
        SELECT * FROM exercises
        WHERE is_deleted = 0
          AND (:muscleFilter IS NULL OR primary_muscle = :muscleFilter)
          AND (:equipmentFilter IS NULL OR equipment = :equipmentFilter)
          AND (
              :query = ''
              OR name_en LIKE '%' || :query || '%' COLLATE NOCASE
              OR name_ar LIKE '%' || :query || '%'
              OR (:normalizedQuery != '' AND name_ar_normalized LIKE '%' || :normalizedQuery || '%')
          )
        ORDER BY is_custom DESC, name_ar ASC
        """
    )
    fun observeExercises(
        query: String,
        normalizedQuery: String,
        muscleFilter: String?,
        equipmentFilter: String?,
    ): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE is_custom = 0 AND is_deleted = 0 AND (name_ar_normalized = '' OR equipment = 'OTHER')")
    suspend fun getExercisesNeedingNormalization(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises WHERE is_custom = 0 AND is_deleted = 0")
    suspend fun countSeedExercises(): Int

    @Query("SELECT id, name_ar AS nameAr, equipment, secondary_muscles AS secondaryMuscles FROM exercises WHERE is_custom = 0 AND is_deleted = 0")
    suspend fun getSeedExerciseNamesOnce(): List<SeedExerciseNameRow>

    @Query(
        "UPDATE exercises SET name_ar = :nameAr, name_ar_normalized = :nameArNormalized, equipment = :equipment, secondary_muscles = :secondaryMuscles, updated_at = :updatedAt WHERE id = :id AND is_custom = 0",
    )
    suspend fun updateSeedArabicName(
        id: String,
        nameAr: String,
        nameArNormalized: String,
        equipment: String,
        secondaryMuscles: String,
        updatedAt: Long,
    )

    @Transaction
    suspend fun syncSeedArabicNames(updates: List<SeedExerciseNameUpdate>) {
        updates.forEach { item ->
            updateSeedArabicName(
                id = item.id,
                nameAr = item.nameAr,
                nameArNormalized = item.nameArNormalized,
                equipment = item.equipment,
                secondaryMuscles = item.secondaryMuscles,
                updatedAt = item.updatedAt,
            )
        }
    }

    @Transaction
    suspend fun applySeedSync(
        missingSeeds: List<ExerciseEntity>,
        nameUpdates: List<SeedExerciseNameUpdate>,
        normalizationPatches: List<ExerciseEntity>,
    ) {
        if (missingSeeds.isNotEmpty()) {
            insertAllIgnore(missingSeeds)
        }
        if (nameUpdates.isNotEmpty()) {
            syncSeedArabicNames(nameUpdates)
        }
        if (normalizationPatches.isNotEmpty()) {
            upsertAll(normalizationPatches)
        }
    }

    @Query("SELECT * FROM exercises WHERE is_custom = 1 ORDER BY created_at ASC")
    suspend fun getCustomExercisesOnce(): List<ExerciseEntity>

    @Query("SELECT id FROM exercises")
    suspend fun getAllExerciseIdsOnce(): List<String>

    @Query("DELETE FROM exercises WHERE is_custom = 1")
    suspend fun deleteAllCustomExercises()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET is_deleted = 1, updated_at = :updatedAt WHERE id = :id AND is_custom = 1")
    suspend fun deleteCustomById(id: String, updatedAt: Long)
}
