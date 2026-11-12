package com.himma.app.core.di

import android.content.Context
import androidx.room.Room
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.database.HimmaDatabase
import com.himma.app.core.database.RoomExerciseRepository
import com.himma.app.core.database.RoomRoutineRepository
import com.himma.app.core.database.RoomUserProfileRepository
import com.himma.app.core.database.RoomWorkoutRepository
import com.himma.app.core.database.RoutineRepository
import com.himma.app.core.database.UserProfileRepository
import com.himma.app.core.database.WorkoutRepository
import com.himma.app.core.database.dao.ExerciseDao
import com.himma.app.core.database.dao.RoutineDao
import com.himma.app.core.database.dao.UserProfileDao
import com.himma.app.core.database.dao.WorkoutDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: RoomUserProfileRepository,
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        impl: RoomExerciseRepository,
    ): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(
        impl: RoomRoutineRepository,
    ): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        impl: RoomWorkoutRepository,
    ): WorkoutRepository

    companion object {
        @Provides
        @Singleton
        fun provideHimmaDatabase(
            @ApplicationContext context: Context,
        ): HimmaDatabase {
            return Room.databaseBuilder(
                context,
                HimmaDatabase::class.java,
                HimmaDatabase.DATABASE_NAME,
            )
                .addMigrations(*HimmaDatabase.MIGRATIONS)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        @Provides
        fun provideUserProfileDao(database: HimmaDatabase): UserProfileDao {
            return database.userProfileDao()
        }

        @Provides
        fun provideExerciseDao(database: HimmaDatabase): ExerciseDao {
            return database.exerciseDao()
        }

        @Provides
        fun provideRoutineDao(database: HimmaDatabase): RoutineDao {
            return database.routineDao()
        }

        @Provides
        fun provideWorkoutDao(database: HimmaDatabase): WorkoutDao {
            return database.workoutDao()
        }
    }
}
