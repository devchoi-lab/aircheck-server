package com.seriouschoi.aircheck.core.domain.model

data class ServiceVersion(
    val osType: String,
    val minVersionCode: Int,
    val updateUrl: String?
) {
    fun needsUpdate(currentVersionCode: Int): Boolean {
        return currentVersionCode < minVersionCode
    }
}

data class VersionCheckResult(
    val needUpdate: Boolean,
    val updateUrl: String?
)
