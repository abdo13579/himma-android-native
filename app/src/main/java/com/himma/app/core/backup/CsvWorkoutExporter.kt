package com.himma.app.core.backup

import com.himma.app.core.database.dao.WorkoutWithSets
import com.himma.app.domain.model.WeightUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvWorkoutExporter {

    private const val UTF8_BOM = "\uFEFF"

    private val headers = listOf(
        "workout_id",
        "date_iso",
        "workout_title",
        "duration_seconds",
        "exercise_order",
        "exercise_id",
        "exercise_name_ar",
        "exercise_name_en",
        "set_index",
        "set_type",
        "weight",
        "weight_unit",
        "reps",
        "rpe",
        "is_completed",
        "is_pr",
        "workout_notes",
    )

    fun exportWorkouts(
        workouts: List<WorkoutWithSets>,
        weightUnit: WeightUnit,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val sb = StringBuilder(UTF8_BOM)
        sb.appendLine(headers.joinToString(",") { escapeCsv(it) })

        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        workouts.forEach { workoutWithSets ->
            val workout = workoutWithSets.workout
            val dateIso = Instant.ofEpochMilli(workout.startTime)
                .atZone(zoneId)
                .format(formatter)

            val orderedSets = workoutWithSets.sets
                .sortedWith(compareBy({ it.set.exerciseOrder }, { it.set.setIndex }))

            if (orderedSets.isEmpty()) {
                val row = listOf(
                    workout.id,
                    dateIso,
                    workout.title,
                    workout.durationSeconds.toString(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    weightUnit.code,
                    "",
                    "",
                    "",
                    "",
                    workout.notes,
                )
                sb.appendLine(row.joinToString(",") { escapeCsv(it) })
            } else {
                orderedSets.forEach { setWithExercise ->
                    val set = setWithExercise.set
                    val displayWeight = set.weightKg?.let { kg ->
                        WeightUnit.toDisplayWeight(kg, weightUnit).toString()
                    }.orEmpty()
                    val row = listOf(
                        workout.id,
                        dateIso,
                        workout.title,
                        workout.durationSeconds.toString(),
                        set.exerciseOrder.toString(),
                        set.exerciseId,
                        setWithExercise.exercise?.nameAr.orEmpty(),
                        setWithExercise.exercise?.nameEn.orEmpty(),
                        (set.setIndex + 1).toString(),
                        set.setType,
                        displayWeight,
                        weightUnit.code,
                        set.reps?.toString().orEmpty(),
                        set.rpe?.toString().orEmpty(),
                        set.isCompleted.toString(),
                        set.isPr.toString(),
                        workout.notes,
                    )
                    sb.appendLine(row.joinToString(",") { escapeCsv(it) })
                }
            }
        }

        return sb.toString()
    }

    internal fun escapeCsv(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
