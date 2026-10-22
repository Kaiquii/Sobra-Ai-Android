package com.example.appfinanceiro.core.update

/** Store/version protocols are native capabilities; update decisions and UI are common. */
interface AppUpdates {
    val installedVersionCode: Int
    val installedVersionName: String
    suspend fun latest(): AppRelease
    fun openStore(url: String)
}

data class AppRelease(val latestVersionCode: Int, val latestVersionName: String,
    val minRequiredVersionCode: Int, val forceUpdate: Boolean, val message: String?,
    val updatedAt: String?, val storeUrl: String)

internal data class AppUpdateState(val version: AppRelease, val mustUpdate: Boolean,
    val installedVersionName: String, val installedVersionCode: Int)

internal fun appUpdateState(installedName: String, installedCode: Int, release: AppRelease): AppUpdateState? {
    val mustUpdate = release.forceUpdate || installedCode < release.minRequiredVersionCode
    return if (mustUpdate || installedCode < release.latestVersionCode)
        AppUpdateState(release, mustUpdate, installedName, installedCode) else null
}
