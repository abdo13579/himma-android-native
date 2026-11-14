package com.himma.app.core.updater

object SemVerHelper {

    /**
     * Compares two semantic version strings (e.g., "v0.1.2-beta" and "0.1.1-beta").
     * Returns true if [remoteTag] is strictly newer than [currentVersion].
     */
    fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

        if (cleanRemote == cleanCurrent) return false

        val remoteParsed = parseSemVer(cleanRemote)
        val currentParsed = parseSemVer(cleanCurrent)

        if (remoteParsed.major != currentParsed.major) {
            return remoteParsed.major > currentParsed.major
        }
        if (remoteParsed.minor != currentParsed.minor) {
            return remoteParsed.minor > currentParsed.minor
        }
        if (remoteParsed.patch != currentParsed.patch) {
            return remoteParsed.patch > currentParsed.patch
        }

        // When major, minor, and patch are equal:
        // A release without pre-release suffix is newer than a pre-release (e.g., 0.1.1 > 0.1.1-beta)
        if (remoteParsed.preRelease.isNullOrEmpty() && !currentParsed.preRelease.isNullOrEmpty()) {
            return true
        }
        if (!remoteParsed.preRelease.isNullOrEmpty() && currentParsed.preRelease.isNullOrEmpty()) {
            return false
        }

        // If both have pre-releases, compare lexicographically/numerically
        return comparePreRelease(remoteParsed.preRelease, currentParsed.preRelease) > 0
    }

    private data class ParsedVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: String?,
    )

    private fun parseSemVer(version: String): ParsedVersion {
        val parts = version.split("-", limit = 2)
        val core = parts[0]
        val preRelease = parts.getOrNull(1)

        val numbers = core.split(".").mapNotNull { it.toIntOrNull() }
        val major = numbers.getOrElse(0) { 0 }
        val minor = numbers.getOrElse(1) { 0 }
        val patch = numbers.getOrElse(2) { 0 }

        return ParsedVersion(major, minor, patch, preRelease)
    }

    private fun comparePreRelease(a: String?, b: String?): Int {
        if (a == b) return 0
        if (a == null) return 1
        if (b == null) return -1

        // Compare dot-separated identifiers if any
        val aParts = a.split(".")
        val bParts = b.split(".")
        val maxLen = maxOf(aParts.size, bParts.size)

        for (i in 0 until maxLen) {
            val aToken = aParts.getOrNull(i) ?: return -1
            val bToken = bParts.getOrNull(i) ?: return 1

            val aNum = aToken.toIntOrNull()
            val bNum = bToken.toIntOrNull()

            if (aNum != null && bNum != null) {
                if (aNum != bNum) return aNum.compareTo(bNum)
            } else {
                val cmp = aToken.compareTo(bToken)
                if (cmp != 0) return cmp
            }
        }
        return 0
    }
}
