package com.himma.app.core.di

import com.himma.app.core.backup.AutoBackupScheduler
import com.himma.app.core.backup.BackupRepository
import com.himma.app.core.backup.LocalBackupRepository
import com.himma.app.core.backup.WorkManagerAutoBackupScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: LocalBackupRepository,
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindAutoBackupScheduler(
        impl: WorkManagerAutoBackupScheduler,
    ): AutoBackupScheduler
}
