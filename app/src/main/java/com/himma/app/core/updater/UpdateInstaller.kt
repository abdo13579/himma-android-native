package com.himma.app.core.updater

import android.content.Context
import java.io.File

/** Flavor-abstracted APK installation contract for self-hosted distributions. */
interface UpdateInstaller {
    fun canInstallPackages(context: Context): Boolean

    fun openInstallPermissionSettings(context: Context): Result<Unit>

    fun startApkInstallation(context: Context, apkFile: File): Result<Unit>
}
