package com.example.appfinanceiro.core.update

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import br.com.sobraai.app.BuildConfig
import com.example.appfinanceiro.core.network.AppVersionResponse
import com.example.appfinanceiro.core.network.auth.RetrofitClient

private data class AppUpdateState(
    val version: AppVersionResponse,
    val mustUpdate: Boolean
)

@Composable
fun AppUpdateGate() {
    val context = LocalContext.current
    var updateState by remember { mutableStateOf<AppUpdateState?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.financeApi.getAppVersion(platform = "android")
            val installedVersionCode = BuildConfig.VERSION_CODE
            val hasUpdate = installedVersionCode < response.latestVersionCode
            val mustUpdate = response.forceUpdate ||
                installedVersionCode < response.minRequiredVersionCode

            if (mustUpdate || hasUpdate) {
                updateState = AppUpdateState(
                    version = response,
                    mustUpdate = mustUpdate
                )
            }
        } catch (e: Exception) {
            Log.w("APP_UPDATE", "Falha ao consultar versão do app", e)
        }
    }

    updateState?.let { state ->
        AppUpdateDialog(
            state = state,
            onDismiss = {
                if (!state.mustUpdate) {
                    updateState = null
                }
            },
            onUpdateClick = {
                openPlayStore(context, state.version.playStoreUrl)
            }
        )
    }
}

@Composable
private fun AppUpdateDialog(
    state: AppUpdateState,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit
) {
    val title = if (state.mustUpdate) {
        "Atualização obrigatória"
    } else {
        "Nova atualização disponível"
    }

    val message = if (state.mustUpdate) {
        state.version.message
            ?: "Para continuar usando o app, atualize para a versão mais recente."
    } else {
        state.version.message
            ?: "Uma nova versão do SobraAi está disponível."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        confirmButton = {
            Button(onClick = onUpdateClick) {
                Text("Atualizar")
            }
        },
        dismissButton = if (state.mustUpdate) {
            null
        } else {
            {
                TextButton(onClick = onDismiss) {
                    Text("Agora não")
                }
            }
        }
    )
}

private fun openPlayStore(context: android.content.Context, playStoreUrl: String) {
    val url = playStoreUrl.ifBlank {
        "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
    }

    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.onFailure { error ->
        Log.w("APP_UPDATE", "Falha ao abrir Play Store", error)
    }
}
