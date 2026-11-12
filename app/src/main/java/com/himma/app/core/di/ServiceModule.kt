package com.himma.app.core.di

import com.himma.app.service.AndroidRestTimerController
import com.himma.app.service.RestTimerController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindRestTimerController(
        impl: AndroidRestTimerController,
    ): RestTimerController
}
