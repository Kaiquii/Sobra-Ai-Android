package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        color = colorScheme.errorContainer,
        contentColor = colorScheme.onErrorContainer,
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
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = onRetry,
                enabled = !isRetrying,
                modifier = Modifier.heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (isRetrying) {
                    AppLoadingIndicator(
                        size = 14.dp,
                        strokeWidth = 2.dp,
                        color = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (isRetrying) "Tentando..." else "Tentar novamente",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun dataRequestErrorMessage(
    errorMessage: String,
    showingPreviousData: Boolean,
    dataLabel: String = "dados"
): String {
    val isConnectionFailure = errorMessage.contains("internet", ignoreCase = true) ||
            errorMessage.contains("conectar", ignoreCase = true) ||
            errorMessage.contains("demorou", ignoreCase = true)

    return when {
        isConnectionFailure && showingPreviousData ->
            "Sem conexão. Exibindo os últimos dados carregados."
        isConnectionFailure ->
            "Sem conexão. Não foi possível carregar $dataLabel."
        showingPreviousData ->
            "Não foi possível atualizar os dados. Exibindo os últimos dados carregados."
        else -> errorMessage
    }
}
