package com.himma.app.domain.model

data class Routine(
    val id: String,
    val title: String,
    val notes: String,
    val orderIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val exercises: List<RoutineExercise> = emptyList(),
) {
    val totalTargetSets: Int
        get() = exercises.sumOf { it.targetSets }

    val primaryMuscles: List<MuscleGroup>
        get() = exercises.map { it.exercise.primaryMuscle }.distinct()
}

data class RoutineExercise(
    val id: String,
    val routineId: String,
    val exercise: Exercise,
    val orderIndex: Int,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetRestSeconds: Int,
) {
    fun formattedRepsRange(): String =
        if (targetRepsMin == targetRepsMax) "$targetRepsMin" else "$targetRepsMin–$targetRepsMax"

    companion object {
        const val MIN_SETS = 1
        const val MAX_SETS = 20
        const val MIN_REPS = 1
        const val MAX_REPS = 100
        const val MIN_REST_SECONDS = 15
        const val MAX_REST_SECONDS = 600
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS_MIN = 8
        const val DEFAULT_REPS_MAX = 12
    }
}

data class RoutineExerciseDraft(
    val itemId: String,
    val exercise: Exercise,
    val targetSets: Int = RoutineExercise.DEFAULT_SETS,
    val targetRepsMin: Int = RoutineExercise.DEFAULT_REPS_MIN,
    val targetRepsMax: Int = RoutineExercise.DEFAULT_REPS_MAX,
    val targetRestSeconds: Int = exercise.defaultRestSeconds.coerceIn(
        RoutineExercise.MIN_REST_SECONDS,
        RoutineExercise.MAX_REST_SECONDS,
    ),
) {
    fun normalized(): RoutineExerciseDraft {
        val clampedMin = targetRepsMin.coerceIn(RoutineExercise.MIN_REPS, RoutineExercise.MAX_REPS)
        val clampedMax = targetRepsMax.coerceIn(clampedMin, RoutineExercise.MAX_REPS)
        return copy(
            targetSets = targetSets.coerceIn(RoutineExercise.MIN_SETS, RoutineExercise.MAX_SETS),
            targetRepsMin = clampedMin,
            targetRepsMax = clampedMax,
            targetRestSeconds = targetRestSeconds.coerceIn(
                RoutineExercise.MIN_REST_SECONDS,
                RoutineExercise.MAX_REST_SECONDS,
            ),
        )
    }

    fun formattedRepsRange(): String =
        if (targetRepsMin == targetRepsMax) "$targetRepsMin" else "$targetRepsMin–$targetRepsMax"
}
