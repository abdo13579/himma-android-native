package com.himma.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.himma.app.core.backup.AutoBackupScheduler
import com.himma.app.core.datastore.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class HimmaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var autoBackupScheduler: AutoBackupScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            if (preferencesRepository.preferences.first().autoBackupEnabled) {
                autoBackupScheduler.scheduleDailySnapshot()
            } else {
                autoBackupScheduler.cancelDailySnapshot()
            }
        }
    }
}
