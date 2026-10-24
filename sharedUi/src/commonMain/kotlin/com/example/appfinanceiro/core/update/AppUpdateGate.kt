package com.example.appfinanceiro.core.update

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
import androidx.compose.foundation.isSystemInDarkTheme
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
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppUpdateGate() {
    val services = LocalUiServices.current
    val source = services.updates ?: return
    var updateState by remember(source) { mutableStateOf<AppUpdateState?>(null) }
    LaunchedEffect(source) {
        try {
            updateState = appUpdateState(source.installedVersionName, source.installedVersionCode, source.latest())
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { /* Keep the existing non-blocking behavior when version lookup fails. */ }
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
                try { source.openStore(state.version.storeUrl) }
                catch (_: Exception) { services.notify("Não foi possível abrir a loja de aplicativos.", com.example.appfinanceiro.core.designsystem.components.AppNotificationType.Error) }
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
        ?: "Atualize o SobraAi para aproveitar as melhorias mais recentes."

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
    val actionTextColor = if (isSystemInDarkTheme()) Color.Black else Color.White

    if (mustUpdate) {
        Button(
            onClick = onUpdateClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = updateTypeColor,
                contentColor = actionTextColor
            ),
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
            colors = ButtonDefaults.buttonColors(
                containerColor = updateTypeColor,
                contentColor = actionTextColor
            ),
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
        val parts = substringBefore('T').split('-')
        require(parts.size == 3)
        "${parts[2]}/${parts[1]}/${parts[0]}"
    }.getOrNull()
}
