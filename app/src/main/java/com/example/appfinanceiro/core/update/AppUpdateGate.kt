package com.example.appfinanceiro.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.sobraai.app.BuildConfig
import com.example.appfinanceiro.core.network.AppVersionResponse
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class AppUpdateState(
    val version: AppVersionResponse,
    val mustUpdate: Boolean,
    val installedVersionName: String,
    val installedVersionCode: Int
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
                    mustUpdate = mustUpdate,
                    installedVersionName = BuildConfig.VERSION_NAME,
                    installedVersionCode = installedVersionCode
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
    val colorScheme = MaterialTheme.colorScheme
    val latestVersionLabel = "v${state.version.latestVersionName}"
    val installedVersionLabel = "v${state.installedVersionName}"
    val updateTypeLabel = if (state.mustUpdate) "Obrigatória" else "Opcional"
    val updateTypeColor = if (state.mustUpdate) Color(0xFFFF6B6B) else Color(0xFF1E88E5)
    val updatedAtLabel = state.version.updatedAt?.toBrazilianDateLabel()
    val title = if (state.mustUpdate) {
        "Atualização obrigatória"
    } else {
        "Nova versão disponível"
    }
    val message = state.version.message
        ?.takeIf { it.isNotBlank() }
        ?: "Atualize o SobraAí para aproveitar as melhorias mais recentes."

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.background,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = updateTypeColor,
                modifier = Modifier
                    .size(48.dp)
                    .background(updateTypeColor.copy(alpha = 0.14f), CircleShape)
                    .padding(10.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$installedVersionLabel → $latestVersionLabel",
                    color = updateTypeColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UpdateInfoRow(
                    label = "Tipo",
                    value = updateTypeLabel,
                    valueColor = updateTypeColor
                )
                UpdateInfoRow(
                    label = "Versão atual",
                    value = "$installedVersionLabel (${state.installedVersionCode})"
                )
                UpdateInfoRow(
                    label = "Nova versão",
                    value = "$latestVersionLabel (${state.version.latestVersionCode})"
                )

                updatedAtLabel?.let {
                    UpdateInfoRow(
                        label = "Publicada em",
                        value = it
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "O que mudou",
                        color = colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        color = colorScheme.onBackground.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 112.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                if (state.mustUpdate) {
                    Text(
                        text = "Para continuar usando o app, atualize para a versão mais recente.",
                        color = updateTypeColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            UpdateDialogActions(
                mustUpdate = state.mustUpdate,
                updateTypeColor = updateTypeColor,
                onDismiss = onDismiss,
                onUpdateClick = onUpdateClick
            )
        },
        dismissButton = null
    )
}

@Composable
private fun UpdateDialogActions(
    mustUpdate: Boolean,
    updateTypeColor: Color,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit
) {
    if (mustUpdate) {
        Button(
            onClick = onUpdateClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = updateTypeColor),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Atualizar agora")
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = updateTypeColor),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Agora não")
        }

        Button(
            onClick = onUpdateClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = updateTypeColor),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Atualizar")
        }
    }
}

@Composable
private fun UpdateInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun String.toBrazilianDateLabel(): String? {
    return runCatching {
        val date = OffsetDateTime.parse(this)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        date.format(formatter)
    }.getOrNull()
}

private fun openPlayStore(context: Context, playStoreUrl: String) {
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
