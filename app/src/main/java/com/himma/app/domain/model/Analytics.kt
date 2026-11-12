package com.himma.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class PersonalRecord(
    val id: String,
    val exerciseId: String,
    val exerciseNameAr: String = "",
    val exerciseNameEn: String = "",
    val maxWeightKg: Double? = null,
    val maxVolumeSetKg: Double? = null,
    val estimated1rmKg: Double? = null,
    val achievedAt: Long = 0L,
    val workoutId: String? = null,
)

data class BodyWeightEntry(
    val id: String,
    val weightKg: Double,
    val recordedAt: Long,
)

data class WorkoutHistorySummary(
    val id: String,
    val routineId: String?,
    val title: String,
    val startTime: Long,
    val endTime: Long?,
    val durationSeconds: Int,
    val totalVolumeKg: Double,
    val completedSetsCount: Int,
    val prCount: Int,
)

data class ProgressionPoint(
    val timestamp: Long,
    val value: Double,
    val reps: Int? = null,
    val workoutTitle: String = "",
)

data class MuscleVolumeDistribution(
    val muscleGroup: MuscleGroup,
    val completedSets: Int,
    val totalVolumeKg: Double,
    val percentageOfSets: Double,
)

enum class InsightType {
    PROGRESSIVE_OVERLOAD,
    MUSCLE_IMBALANCE,
    MILESTONE,
}

data class ProgressiveInsight(
    val type: InsightType,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val exerciseId: String? = null,
)

object BrzyckiMath {

    /**
     * Brzycki Formula: 1RM = weight / (1.0278 - 0.0278 * reps)
     * Reps are clamped to 1..30 to avoid denominator explosion or negative values.
     */
    fun calculateEstimated1Rm(weightKg: Double?, reps: Int?): Double {
        if (weightKg == null || reps == null || weightKg <= 0.0 || reps <= 0) {
            return 0.0
        }
        if (reps == 1) return (weightKg * 10.0).roundToInt() / 10.0

        val clampedReps = reps.coerceIn(1, 30)
        val denominator = 1.0278 - (0.0278 * clampedReps)
        if (denominator <= 0.001) return weightKg

        val estimate = weightKg / denominator
        return (estimate * 10.0).roundToInt() / 10.0
    }

    fun calculateSetVolume(weightKg: Double?, reps: Int?): Double {
        if (weightKg == null || reps == null || weightKg <= 0.0 || reps <= 0) {
            return 0.0
        }
        return (weightKg * reps * 10.0).roundToInt() / 10.0
    }

    /**
     * Checks if a completed set surpasses the existing PR for an exercise.
     * Evaluates Max Weight, Max Set Volume (weight * reps), and Estimated 1RM.
     */
    fun isSetPersonalRecord(
        weightKg: Double?,
        reps: Int?,
        existingPr: PersonalRecord?,
    ): Boolean {
        if (weightKg == null || reps == null || weightKg <= 0.0 || reps <= 0) {
            return false
        }
        val estimated1rm = calculateEstimated1Rm(weightKg, reps)
        val setVolume = calculateSetVolume(weightKg, reps)

        if (existingPr == null) {
            return true
        }

        val exceedsWeight = (existingPr.maxWeightKg ?: 0.0) < weightKg - 0.001
        val exceedsVolume = (existingPr.maxVolumeSetKg ?: 0.0) < setVolume - 0.001
        val exceeds1Rm = (existingPr.estimated1rmKg ?: 0.0) < estimated1rm - 0.001

        return exceedsWeight || exceedsVolume || exceeds1Rm
    }

    /**
     * Merges a set's metrics into an updated PersonalRecord record.
     */
    fun updateRecord(
        existing: PersonalRecord?,
        exerciseId: String,
        weightKg: Double,
        reps: Int,
        achievedAt: Long,
        workoutId: String?,
    ): PersonalRecord {
        val estimated1rm = calculateEstimated1Rm(weightKg, reps)
        val setVolume = calculateSetVolume(weightKg, reps)

        val newMaxWeight = maxOf(existing?.maxWeightKg ?: 0.0, weightKg)
        val newMaxVolume = maxOf(existing?.maxVolumeSetKg ?: 0.0, setVolume)
        val newMax1Rm = maxOf(existing?.estimated1rmKg ?: 0.0, estimated1rm)

        return PersonalRecord(
            id = existing?.id ?: "pr_$exerciseId",
            exerciseId = exerciseId,
            exerciseNameAr = existing?.exerciseNameAr.orEmpty(),
            exerciseNameEn = existing?.exerciseNameEn.orEmpty(),
            maxWeightKg = newMaxWeight,
            maxVolumeSetKg = newMaxVolume,
            estimated1rmKg = newMax1Rm,
            achievedAt = achievedAt,
            workoutId = workoutId ?: existing?.workoutId,
        )
    }

    /**
     * Generates transparent, 100% offline local progression insights.
     */
    fun generateInsights(
        distributions: List<MuscleVolumeDistribution>,
        recentPrs: List<PersonalRecord>,
        topExercisesWithSets: Map<Exercise, List<WorkoutSet>>,
    ): List<ProgressiveInsight> {
        val insights = mutableListOf<ProgressiveInsight>()

        // 1. Muscle Imbalance Insight
        if (distributions.isNotEmpty()) {
            val totalSets = distributions.sumOf { it.completedSets }
            if (totalSets >= 10) {
                val threshold = (totalSets.toDouble() / (MuscleGroup.entries.size * 2.0)).coerceAtLeast(1.0)
                val neglected = distributions.filter {
                    it.completedSets.toDouble() <= threshold
                }
                if (neglected.isNotEmpty()) {
                    val muscleNamesAr = neglected.joinToString("، ") { it.muscleGroup.nameArLabel() }
                    val muscleNamesEn = neglected.joinToString(", ") { it.muscleGroup.nameEnLabel() }
                    insights.add(
                        ProgressiveInsight(
                            type = InsightType.MUSCLE_IMBALANCE,
                            titleAr = "توازن المجموعات العضلية",
                            titleEn = "Muscle Balance Alert",
                            descriptionAr = "لاحظنا انخفاض حجم التمرين لمجموعة ($muscleNamesAr) مقارنة ببقية العضلات هذا الأسبوع. احرص على توازن روتينك التدريبي لتفادي الإصابات.",
                            descriptionEn = "Low training volume detected for ($muscleNamesEn) relative to your overall weekly workload. Balance your routine to promote even symmetry.",
                        ),
                    )
                }
            }
        }

        // 2. Progressive Overload Recommendations for top exercises
        topExercisesWithSets.forEach { (exercise, sets) ->
            if (sets.size >= 3) {
                val lastSet = sets.last()
                val lastWeight = lastSet.weightKg ?: 0.0
                val lastReps = lastSet.reps ?: 0
                if (lastWeight > 0.0 && lastReps >= 8) {
                    insights.add(
                        ProgressiveInsight(
                            type = InsightType.PROGRESSIVE_OVERLOAD,
                            titleAr = "حمل تدريجي مقترح: ${exercise.nameAr}",
                            titleEn = "Progressive Overload: ${exercise.nameEn}",
                            descriptionAr = "أنجزت $lastReps تكراراً بوزن $lastWeight كغ. في جلستك القادمة، جرّب زيادة 1-2.5 كغ أو إضافة تكرار إضافي لتطبيق مبدأ الزيادة التدريجية المستمرة.",
                            descriptionEn = "You logged $lastReps reps at ${lastWeight}kg. In your next session, try adding 1-2.5kg or 1 extra rep to apply continuous progressive overload.",
                            exerciseId = exercise.id,
                        ),
                    )
                }
            }
        }

        // 3. Recent PR Celebration Milestone
        if (recentPrs.isNotEmpty()) {
            val latest = recentPrs.maxByOrNull { it.achievedAt }
            if (latest != null) {
                val name = latest.exerciseNameAr.ifBlank { latest.exerciseNameEn }
                val weight = latest.maxWeightKg ?: 0.0
                insights.add(
                    ProgressiveInsight(
                        type = InsightType.MILESTONE,
                        titleAr = "إنجاز قياسي جديد 🏆",
                        titleEn = "New PR Achieved 🏆",
                        descriptionAr = "حطمت رقماً قياسياً شخصياً في $name بوزن $weight كغ. استمر في العطاء!",
                        descriptionEn = "You smashed a personal record in $name with ${weight}kg. Keep going strong!",
                        exerciseId = latest.exerciseId,
                    ),
                )
            }
        }

        return insights.take(4)
    }

    private fun MuscleGroup.nameArLabel(): String = when (this) {
        MuscleGroup.CHEST -> "الصدر"
        MuscleGroup.BACK -> "الظهر"
        MuscleGroup.LEGS -> "الأرجل"
        MuscleGroup.SHOULDERS -> "الأكتاف"
        MuscleGroup.ARMS -> "الذراعين"
        MuscleGroup.CORE -> "البطن والجذع"
    }

    private fun MuscleGroup.nameEnLabel(): String = when (this) {
        MuscleGroup.CHEST -> "Chest"
        MuscleGroup.BACK -> "Back"
        MuscleGroup.LEGS -> "Legs"
        MuscleGroup.SHOULDERS -> "Shoulders"
        MuscleGroup.ARMS -> "Arms"
        MuscleGroup.CORE -> "Core"
    }
}
