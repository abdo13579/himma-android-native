package com.himma.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.himma.app.core.database.dao.AnalyticsDao
import com.himma.app.core.database.dao.ExerciseDao
import com.himma.app.core.database.dao.RoutineDao
import com.himma.app.core.database.dao.UserProfileDao
import com.himma.app.core.database.dao.WorkoutDao
import com.himma.app.core.database.entity.BodyWeightEntity
import com.himma.app.core.database.entity.ExerciseEntity
import com.himma.app.core.database.entity.PersonalRecordEntity
import com.himma.app.core.database.entity.RoutineEntity
import com.himma.app.core.database.entity.RoutineExerciseEntity
import com.himma.app.core.database.entity.UserProfileEntity
import com.himma.app.core.database.entity.WorkoutEntity
import com.himma.app.core.database.entity.WorkoutSetEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        PersonalRecordEntity::class,
        BodyWeightEntity::class,
    ],
    version = HimmaDatabase.DATABASE_VERSION,
    exportSchema = true,
)
abstract class HimmaDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        const val DATABASE_NAME = "himma.db"
        const val DATABASE_VERSION = 7

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercises` (
                        `id` TEXT NOT NULL,
                        `name_en` TEXT NOT NULL,
                        `name_ar` TEXT NOT NULL,
                        `primary_muscle` TEXT NOT NULL,
                        `secondary_muscles` TEXT NOT NULL,
                        `equipment` TEXT NOT NULL,
                        `instructions_en` TEXT NOT NULL,
                        `instructions_ar` TEXT NOT NULL,
                        `local_media_key` TEXT,
                        `default_rest_seconds` INTEGER NOT NULL,
                        `is_custom` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercises_primary_muscle` ON `exercises` (`primary_muscle`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercises_equipment` ON `exercises` (`equipment`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercises_is_custom` ON `exercises` (`is_custom`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `exercises` ADD COLUMN `name_ar_normalized` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercises_name_ar_normalized` ON `exercises` (`name_ar_normalized`)",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipment` = 'BODYWEIGHT' WHERE `id` = 'Ab_Roller'",
                )
                db.execSQL(
                    "UPDATE `exercises` SET `equipment` = 'DUMBBELL' WHERE `id` = 'One-Arm_Kettlebell_Swings'",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Refresh built-in seed rows to open-source CC BY-SA 4.0 catalog; custom exercises preserved.
                db.execSQL("DELETE FROM `exercises` WHERE `is_custom` = 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routines` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `order_index` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_routines_is_deleted_order_index` ON `routines` (`is_deleted`, `order_index`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routine_exercises` (
                        `id` TEXT NOT NULL,
                        `routine_id` TEXT NOT NULL,
                        `exercise_id` TEXT NOT NULL,
                        `order_index` INTEGER NOT NULL,
                        `target_sets` INTEGER NOT NULL,
                        `target_reps_min` INTEGER NOT NULL,
                        `target_reps_max` INTEGER NOT NULL,
                        `target_rest_seconds` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`routine_id`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_routine_exercises_routine_id_order_index` ON `routine_exercises` (`routine_id`, `order_index`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_routine_exercises_exercise_id` ON `routine_exercises` (`exercise_id`)",
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workouts` (
                        `id` TEXT NOT NULL,
                        `routine_id` TEXT,
                        `title` TEXT NOT NULL,
                        `start_time` INTEGER NOT NULL,
                        `end_time` INTEGER,
                        `duration_seconds` INTEGER NOT NULL,
                        `total_volume_kg` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        `is_completed` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`routine_id`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workouts_routine_id` ON `workouts` (`routine_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workouts_is_completed_start_time` ON `workouts` (`is_completed`, `start_time`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_sets` (
                        `id` TEXT NOT NULL,
                        `workout_id` TEXT NOT NULL,
                        `exercise_id` TEXT NOT NULL,
                        `exercise_order` INTEGER NOT NULL,
                        `set_index` INTEGER NOT NULL,
                        `rest_seconds` INTEGER NOT NULL,
                        `weight_kg` REAL,
                        `reps` INTEGER,
                        `rpe` REAL,
                        `set_type` TEXT NOT NULL,
                        `is_completed` INTEGER NOT NULL,
                        `is_pr` INTEGER NOT NULL,
                        `completed_at` INTEGER,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`workout_id`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sets_workout_id_exercise_order_set_index` ON `workout_sets` (`workout_id`, `exercise_order`, `set_index`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sets_exercise_id_set_index_is_completed` ON `workout_sets` (`exercise_id`, `set_index`, `is_completed`)",
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `personal_records` (
                        `id` TEXT NOT NULL,
                        `exercise_id` TEXT NOT NULL,
                        `max_weight_kg` REAL,
                        `max_volume_set_kg` REAL,
                        `estimated_1rm_kg` REAL,
                        `achieved_at` INTEGER NOT NULL,
                        `workout_id` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`workout_id`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_personal_records_exercise_id` ON `personal_records` (`exercise_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_personal_records_achieved_at` ON `personal_records` (`achieved_at`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_personal_records_workout_id` ON `personal_records` (`workout_id`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `body_weights` (
                        `id` TEXT NOT NULL,
                        `weight_kg` REAL NOT NULL,
                        `recorded_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_body_weights_recorded_at` ON `body_weights` (`recorded_at`)",
                )
            }
        }

        /**
         * Explicit migrations array; new specs append versioned migrations here
         * to guarantee zero data loss across updates.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
        )
    }
}
