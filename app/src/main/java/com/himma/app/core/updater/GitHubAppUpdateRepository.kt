package com.himma.app.core.updater

import android.content.Context
import com.himma.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class GitHubAppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppUpdateRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun checkForUpdate(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(RELEASES_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    IOException("GitHub API error: HTTP $responseCode ${connection.responseMessage}")
                )
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val releases = json.decodeFromString<List<GitHubReleaseDto>>(responseBody)

            val latestRelease = releases.firstOrNull()
                ?: return@withContext Result.failure(IllegalStateException("No releases found on repository"))

            val apkAsset = latestRelease.assets.firstOrNull { asset ->
                asset.name.endsWith(".apk", ignoreCase = true)
            } ?: return@withContext Result.failure(IllegalStateException("No APK asset found in latest release"))

            val isNewer = SemVerHelper.isNewer(
                remoteTag = latestRelease.tagName,
                currentVersion = BuildConfig.VERSION_NAME,
            )

            val updateInfo = AppUpdateInfo(
                remoteVersion = latestRelease.tagName.removePrefix("v"),
                releaseTitle = latestRelease.name?.ifBlank { latestRelease.tagName } ?: latestRelease.tagName,
                releaseNotes = latestRelease.body.orEmpty(),
                apkDownloadUrl = apkAsset.browserDownloadUrl,
                apkSizeBytes = apkAsset.size,
                isUpdateAvailable = isNewer,
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadUpdate(
        downloadUrl: String,
        fileName: String,
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(updateDir, fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val connection = openConnectionFollowingRedirects(downloadUrl)
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    IOException("Failed to download APK: HTTP $responseCode")
                )
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastReportedPercent = -1

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val percent = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        if (percent != lastReportedPercent || downloadedBytes == totalBytes) {
                            lastReportedPercent = percent
                            onProgress(percent, downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openConnectionFollowingRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        var hops = 0
        while (hops < MAX_REDIRECT_HOPS) {
            val url = URL(currentUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val code = connection.responseCode
            if (code in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308,
                )
            ) {
                val newLocation = connection.getHeaderField("Location")
                    ?: throw IOException("Redirect status $code with missing Location header")
                connection.disconnect()
                currentUrl = newLocation
                hops++
            } else {
                return connection
            }
        }
        throw IOException("Too many redirects (max $MAX_REDIRECT_HOPS)")
    }

    companion object {
        private const val RELEASES_API_URL =
            "https://api.github.com/repos/abdo13579/himma-android-native/releases?per_page=1"
        private const val USER_AGENT = "Himma-Android-App/${BuildConfig.VERSION_NAME}"
        private const val TIMEOUT_MILLIS = 15_000
        private const val MAX_REDIRECT_HOPS = 5
    }
}
