package com.himma.app.domain.model

enum class SetType {
    NORMAL,
    WARMUP,
    DROP_SET,
    FAILURE;

    companion object {
        fun fromNameOrDefault(raw: String?): SetType {
            return entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: NORMAL
        }
    }
}

data class PreviousSetHint(
    val exerciseId: String,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
)

data class WorkoutSet(
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseOrder: Int,
    val setIndex: Int,
    val restSeconds: Int,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double? = null,
    val setType: SetType = SetType.NORMAL,
    val isCompleted: Boolean = false,
    val isPr: Boolean = false,
    val completedAt: Long? = null,
)

data class WorkoutExerciseGroup(
    val exercise: Exercise,
    val exerciseOrder: Int,
    val restSeconds: Int,
    val sets: List<WorkoutSet>,
)

data class WorkoutSession(
    val id: String,
    val routineId: String?,
    val title: String,
    val startTime: Long,
    val endTime: Long?,
    val durationSeconds: Int,
    val totalVolumeKg: Double,
    val notes: String,
    val isCompleted: Boolean,
    val exercises: List<WorkoutExerciseGroup>,
) {
    val completedSetsCount: Int
        get() = exercises.sumOf { group -> group.sets.count { it.isCompleted } }

    val totalSetsCount: Int
        get() = exercises.sumOf { it.sets.size }
}

data class FinishedWorkoutSummary(
    val workoutId: String,
    val title: String,
    val durationSeconds: Int,
    val totalVolumeKg: Double,
    val completedSets: Int,
)

object WorkoutMath {
    fun calculateTotalVolumeKg(sets: Iterable<WorkoutSet>): Double {
        return sets
            .filter { it.isCompleted }
            .sumOf { set ->
                val weight = (set.weightKg ?: 0.0).coerceAtLeast(0.0)
                val reps = (set.reps ?: 0).coerceAtLeast(0)
                weight * reps
            }
    }

    fun calculateDurationSeconds(startTimeMillis: Long, endTimeMillis: Long): Int {
        return ((endTimeMillis - startTimeMillis) / 1000L).coerceAtLeast(0L).toInt()
    }

    fun resolveRestSeconds(
        routineRestSeconds: Int?,
        exerciseDefaultRestSeconds: Int?,
        userDefaultRestSeconds: Int,
    ): Int {
        val chosen = routineRestSeconds?.takeIf { it > 0 }
            ?: exerciseDefaultRestSeconds?.takeIf { it > 0 }
            ?: userDefaultRestSeconds
        return chosen.coerceIn(15, 600)
    }

    fun formatTimer(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
