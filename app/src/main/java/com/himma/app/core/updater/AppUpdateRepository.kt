package com.himma.app.core.updater

import java.io.File

interface AppUpdateRepository {
    suspend fun checkForUpdate(): Result<AppUpdateInfo>

    suspend fun downloadUpdate(
        downloadUrl: String,
        fileName: String,
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<File>
}
