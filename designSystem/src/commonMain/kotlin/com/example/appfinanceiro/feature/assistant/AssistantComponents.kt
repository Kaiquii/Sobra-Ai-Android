package com.example.appfinanceiro.feature.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
@Composable
fun EmptyAssistantState() {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Como posso ajudar com suas finanças?",
                color = colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Pergunte sobre gastos, rendas ou cadastros.",
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChatBubble(message: AssistantMessage) {
    val isUser = message.role == "user"
    val isQuotaWarning = message.errorCode == "gemini_quota_exceeded"
    val colorScheme = MaterialTheme.colorScheme
    val bubbleColor = when {
        isUser -> PrimaryBlue
        isQuotaWarning -> colorScheme.errorContainer
        else -> colorScheme.surface
    }
    val contentColor = when {
        isUser -> Color.White
        isQuotaWarning -> colorScheme.onErrorContainer
        else -> colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isQuotaWarning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Limite temporário do Gemini",
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = message.content,
                    color = contentColor
                )
                message.displayTime?.let { displayTime ->
                    Text(
                        text = displayTime,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantInputBar(
    value: String,
    enabled: Boolean,
    canSend: Boolean,
    quotaRetrySeconds: Int?,
    isListening: Boolean,
    voiceError: String?,
    onValueChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onSend: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isQuotaBlocked = (quotaRetrySeconds ?: 0) > 0
    val canUseVoice = enabled && !isQuotaBlocked
    val statusText = when {
        isListening -> "Ouvindo..."
        voiceError != null -> voiceError
        else -> null
    }
    val statusColor = if (isListening) PrimaryBlue else colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.background)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        statusText?.let { text ->
            Text(
                text = text,
                color = statusColor,
                fontWeight = if (isListening) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (isListening) {
                            "Fale sua mensagem..."
                        } else if (isQuotaBlocked) {
                            "Tente novamente em ${quotaRetrySeconds}s"
                        } else {
                            "Digite sua mensagem..."
                        }
                    )
                },
                enabled = enabled && !isQuotaBlocked,
                minLines = 1,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                ),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    disabledContainerColor = colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = PrimaryBlue
                )
            )

            IconButton(
                onClick = onVoiceClick,
                enabled = canUseVoice,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Parar gravacao" else "Falar mensagem",
                    tint = when {
                        isListening -> colorScheme.error
                        canUseVoice -> PrimaryBlue
                        else -> colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (canSend) PrimaryBlue else colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

