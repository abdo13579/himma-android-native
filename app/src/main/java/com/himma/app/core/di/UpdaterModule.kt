package com.himma.app.core.di

import com.himma.app.core.updater.AppUpdateRepository
import com.himma.app.core.updater.GitHubAppUpdateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdaterModule {

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(
        impl: GitHubAppUpdateRepository,
    ): AppUpdateRepository
}
