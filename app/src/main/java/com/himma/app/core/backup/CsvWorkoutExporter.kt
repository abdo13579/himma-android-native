package com.himma.app.core.backup

import com.himma.app.core.database.dao.WorkoutWithSets
import com.himma.app.domain.model.WeightUnit
import com.himma.app.domain.model.WeightUnitMode
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
        "set_duration_seconds",
        "weight_unit_mode",
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
                    "",
                    "",
                )
                sb.appendLine(row.joinToString(",") { escapeCsv(it) })
            } else {
                orderedSets.forEach { setWithExercise ->
                    val set = setWithExercise.set
                    val unitMode = WeightUnitMode.fromNameOrDefault(set.weightUnitMode)
                    val displayWeight = when {
                        set.weightKg == null -> ""
                        unitMode == WeightUnitMode.STACK_PLATES ->
                            if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt().toString() else set.weightKg.toString()
                        else -> WeightUnit.toDisplayWeight(set.weightKg, weightUnit).toString()
                    }
                    val displayUnit = if (unitMode == WeightUnitMode.STACK_PLATES) "plates" else weightUnit.code
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
                        displayUnit,
                        set.reps?.toString().orEmpty(),
                        set.rpe?.toString().orEmpty(),
                        set.isCompleted.toString(),
                        set.isPr.toString(),
                        workout.notes,
                        set.durationSeconds?.toString().orEmpty(),
                        unitMode.name,
                    )
                    sb.appendLine(row.joinToString(",") { escapeCsv(it) })
                }
            }
        }

        return sb.toString()
    }

    internal fun escapeCsv(value: String): String {
        val neutralized = if (
            value.isNotEmpty() &&
            value[0].let { it == '=' || it == '+' || it == '-' || it == '@' || it == '\t' || it == '\r' }
        ) {
            "'$value"
        } else {
            value
        }
        val needsQuoting = neutralized.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return neutralized
        val escaped = neutralized.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
