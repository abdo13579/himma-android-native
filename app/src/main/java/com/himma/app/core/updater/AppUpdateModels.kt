package com.himma.app.core.updater

import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@Serializable
data class GitHubReleaseAssetDto(
    val name: String = "",
    val size: Long = 0L,
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
)

data class AppUpdateInfo(
    val remoteVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val isUpdateAvailable: Boolean,
)

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdateDownloadState
    data class Completed(val apkFile: File) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

sealed interface UpdateCheckUiState {
    data object Idle : UpdateCheckUiState
    data object Checking : UpdateCheckUiState
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckUiState
    data class UpToDate(val currentVersion: String) : UpdateCheckUiState
    data class Error(val message: String) : UpdateCheckUiState
}
