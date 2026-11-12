package com.himma.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.himma.app.core.database.dao.UserProfileDao
import com.himma.app.core.database.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
    ],
    version = HimmaDatabase.DATABASE_VERSION,
    exportSchema = true,
)
abstract class HimmaDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        const val DATABASE_NAME = "himma.db"
        const val DATABASE_VERSION = 1

        /**
         * Explicit migrations array; new specs append versioned migrations here
         * to guarantee zero data loss across updates.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
