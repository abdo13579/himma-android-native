package com.himma.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.himma.app.core.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:muscleFilter IS NULL OR primary_muscle = :muscleFilter)
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

    @Query("SELECT * FROM exercises WHERE name_ar_normalized = '' OR equipment = 'OTHER'")
    suspend fun getExercisesNeedingNormalization(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises WHERE is_custom = 0")
    suspend fun countSeedExercises(): Int

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

    @Query("DELETE FROM exercises WHERE id = :id AND is_custom = 1")
    suspend fun deleteCustomById(id: String)
}
