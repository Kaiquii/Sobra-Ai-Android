package com.example.appfinanceiro.platform

import br.com.sobraai.app.BuildConfig
import br.com.sobraai.ui.NativeFeatures
import com.example.appfinanceiro.core.data.AppVersionRepository
import com.example.appfinanceiro.core.update.AppRelease
import com.example.appfinanceiro.core.update.AppUpdates

class AndroidAppUpdates(private val native: NativeFeatures) : AppUpdates {
    override val installedVersionCode = BuildConfig.VERSION_CODE
    override val installedVersionName = BuildConfig.VERSION_NAME
    override suspend fun latest(): AppRelease = AppVersionRepository().getAndroidVersion().let {
        AppRelease(it.latestVersionCode, it.latestVersionName, it.minRequiredVersionCode, it.forceUpdate,
            it.message, it.updatedAt, it.playStoreUrl)
    }
    override fun openStore(url: String) = native.openLink(url.ifBlank {
        "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
    })
}
