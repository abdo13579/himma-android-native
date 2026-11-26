package com.himma.app.core.updater

import android.content.Context
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** No-op installer for the Google Play Store distribution. */
@Singleton
class StoreUpdateInstaller @Inject constructor() : UpdateInstaller {
    override fun canInstallPackages(context: Context): Boolean = false

    override fun openInstallPermissionSettings(context: Context): Result<Unit> =
        Result.failure(UnsupportedOperationException("Updates are managed by Google Play"))

    override fun startApkInstallation(context: Context, apkFile: File): Result<Unit> =
        Result.failure(UnsupportedOperationException("Updates are managed by Google Play"))
}
