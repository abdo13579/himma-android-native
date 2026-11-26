package com.himma.app.core.updater

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** No-op updater for the Google Play Store distribution; updates are managed by Google Play. */
@Singleton
class StoreAppUpdateRepository @Inject constructor() : AppUpdateRepository {
    override suspend fun checkForUpdate(): Result<AppUpdateInfo> =
        Result.failure(UnsupportedOperationException("Updates are managed by Google Play"))

    override suspend fun downloadUpdate(
        downloadUrl: String,
        fileName: String,
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<File> =
        Result.failure(UnsupportedOperationException("Updates are managed by Google Play"))
}
