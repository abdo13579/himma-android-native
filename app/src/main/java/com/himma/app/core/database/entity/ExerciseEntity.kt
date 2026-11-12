package com.himma.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.text.ArabicTextNormalizer

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["primary_muscle"]),
        Index(value = ["equipment"]),
        Index(value = ["is_custom"]),
        Index(value = ["name_ar_normalized"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "name_en")
    val nameEn: String,
    @ColumnInfo(name = "name_ar")
    val nameAr: String,
    @ColumnInfo(name = "name_ar_normalized", defaultValue = "")
    val nameArNormalized: String = ArabicTextNormalizer.normalize(nameAr),
    @ColumnInfo(name = "primary_muscle")
    val primaryMuscle: String,
    @ColumnInfo(name = "secondary_muscles")
    val secondaryMuscles: String,
    val equipment: String,
    @ColumnInfo(name = "instructions_en")
    val instructionsEn: String,
    @ColumnInfo(name = "instructions_ar")
    val instructionsAr: String,
    @ColumnInfo(name = "local_media_key")
    val localMediaKey: String?,
    @ColumnInfo(name = "default_rest_seconds")
    val defaultRestSeconds: Int,
    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
) {
    fun toDomain(): Exercise = Exercise(
        id = id,
        nameEn = nameEn,
        nameAr = nameAr,
        primaryMuscle = MuscleGroup.fromNameOrNull(primaryMuscle) ?: MuscleGroup.CHEST,
        secondaryMuscles = secondaryMuscles
            .split(",")
            .mapNotNull { MuscleGroup.fromNameOrNull(it) },
        equipment = EquipmentType.fromNameOrNull(equipment) ?: EquipmentType.OTHER,
        instructionsEn = instructionsEn,
        instructionsAr = instructionsAr,
        localMediaKey = localMediaKey,
        defaultRestSeconds = defaultRestSeconds,
        isCustom = isCustom,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(exercise: Exercise): ExerciseEntity = ExerciseEntity(
            id = exercise.id,
            nameEn = exercise.nameEn,
            nameAr = exercise.nameAr,
            nameArNormalized = ArabicTextNormalizer.normalize(exercise.nameAr),
            primaryMuscle = exercise.primaryMuscle.name,
            secondaryMuscles = exercise.secondaryMuscles.joinToString(",") { it.name },
            equipment = exercise.equipment.name,
            instructionsEn = exercise.instructionsEn,
            instructionsAr = exercise.instructionsAr,
            localMediaKey = exercise.localMediaKey,
            defaultRestSeconds = exercise.defaultRestSeconds,
            isCustom = exercise.isCustom,
            createdAt = exercise.createdAt,
            updatedAt = exercise.updatedAt,
        )
    }
}
