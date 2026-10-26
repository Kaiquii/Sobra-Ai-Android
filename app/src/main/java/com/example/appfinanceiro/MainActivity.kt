package com.example.appfinanceiro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import br.com.sobraai.app.BuildConfig
import br.com.sobraai.ui.AppController
import br.com.sobraai.ui.SobraAiApp
import br.com.sobraai.ui.runtime.UiServices
import com.example.appfinanceiro.core.data.*
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.core.security.PlayIntegrityProtection
import com.example.appfinanceiro.platform.*
import kotlinx.coroutines.launch

/** Android host: native services and system lifecycle only. All application screens are common. */
class MainActivity : FragmentActivity() {
    private lateinit var controller: AppController
    private lateinit var native: AndroidNativeFeatures
    private val fileClient by lazy { createPlatformHttpClient() }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        PlayIntegrityProtection.initialize(applicationContext)
        lifecycleScope.launch { runCatching { PlayIntegrityProtection.warmUp() } }
        val session = SessionManager(this)
        val auth = AuthRepository()
        native = AndroidNativeFeatures(this)
        controller = AppController(auth::login, session, AndroidBiometrics(this), lifecycleScope)
        val services = UiServices(session, FinanceRepository(), auth,
            KtorFinanceFiles(fileClient, ApiConfiguration(BuildConfig.API_BASE_URL)), native, controller, native, BuildConfig.API_BASE_URL,
            installationHint = "Atualize ou instale o app pela Google Play.", updates = AndroidAppUpdates(native), patrimony = AndroidApiClient.patrimony,
            investmentCategories = AndroidApiClient.patrimony)
        setContent { SobraAiApp(services) }
    }
    override fun onStart() {
        super.onStart()
        if (::controller.isInitialized) controller.enterForeground()
    }
    override fun onStop() {
        if (::native.isInitialized && !native.externalInteraction) {
            native.cancelInteractions()
            controller.enterBackground()
        }
        super.onStop()
    }
    override fun onDestroy() {
        if (::native.isInitialized) native.cancelInteractions()
        fileClient.close()
        super.onDestroy()
    }
}
