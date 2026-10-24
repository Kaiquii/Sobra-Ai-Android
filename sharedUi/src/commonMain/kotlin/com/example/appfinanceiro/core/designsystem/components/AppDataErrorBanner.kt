package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AppDataErrorBanner(
    message: String,
    onRetry: () -> Unit,
    isRetrying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )
            IconButton(
                onClick = onRetry,
                enabled = !isRetrying,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = if (isRetrying) "Tentando novamente" else "Tentar novamente"
                }
            ) {
                if (isRetrying) {
                    AppLoadingIndicator(size = 18.dp, color = colorScheme.error)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = colorScheme.error,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

fun dataRequestErrorMessage(
    errorMessage: String,
    showingPreviousData: Boolean,
    dataLabel: String = "dados"
): String {
    val isTimeout = errorMessage.contains("demorou", ignoreCase = true)
    val isConnectionFailure = errorMessage.contains("internet", ignoreCase = true) ||
            errorMessage.contains("conectar", ignoreCase = true)

    return when {
        isTimeout && showingPreviousData ->
            "O servidor demorou para responder. Exibindo dados salvos."
        isTimeout -> errorMessage
        isConnectionFailure && showingPreviousData ->
            "Falha de conexão com o servidor. Exibindo dados salvos."
        isConnectionFailure ->
            "Falha de conexão com o servidor. Não foi possível carregar $dataLabel."
        showingPreviousData ->
            "$errorMessage Exibindo dados salvos."
        else -> errorMessage
    }
}
