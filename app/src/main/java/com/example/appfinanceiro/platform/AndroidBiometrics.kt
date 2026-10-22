package com.example.appfinanceiro.platform

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import br.com.sobraai.ui.BiometricAuthenticator
import com.example.appfinanceiro.core.biometric.BiometricAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidBiometrics(private val activity: FragmentActivity) : BiometricAuthenticator {
    override fun isAvailable() = BiometricAuth.isAvailable(activity)
    override suspend fun authenticate(): Boolean = suspendCancellableCoroutine { c ->
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { if (c.isActive) c.resume(true) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { if (c.isActive) c.resume(false) }
        })
        c.invokeOnCancellation { prompt.cancelAuthentication() }
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Entrar com biometria")
            .setSubtitle("Confirme sua identidade para continuar").setNegativeButtonText("Cancelar").build())
    }
}
