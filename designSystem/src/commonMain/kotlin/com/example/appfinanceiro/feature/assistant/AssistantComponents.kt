package com.example.appfinanceiro.feature.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

@Composable
fun EmptyAssistantState(modifier: Modifier = Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = if (compact) 12.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = PrimaryBlue.copy(alpha = 0.12f),
            shape = RoundedCornerShape(if (compact) 16.dp else 24.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue,
                modifier = Modifier.padding(if (compact) 12.dp else 22.dp)
                    .size(if (compact) 24.dp else 36.dp)
            )
        }
        Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
        Text(
            "Como posso ajudar?",
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = colors.onBackground, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Text(
            "Pergunte sobre seus gastos, rendas ou cadastros e organize suas finanças do dia a dia.",
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 360.dp)
        )
    }
}

@Composable
fun ChatBubble(message: AssistantMessage) {
    val isUser = message.role == "user"
    val isQuotaWarning = message.errorCode == "gemini_quota_exceeded"
    val colors = MaterialTheme.colorScheme
    val bubbleColor = when {
        isUser -> PrimaryBlue
        isQuotaWarning -> colors.errorContainer
        else -> colors.surface
    }
    val contentColor = when {
        isUser -> Color.White
        isQuotaWarning -> colors.onErrorContainer
        else -> colors.onSurface
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.widthIn(max = maxWidth * if (isUser) 0.85f else 0.94f)
                .align(if (isUser) Alignment.CenterEnd else Alignment.CenterStart),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomEnd = if (isUser) 6.dp else 18.dp,
                    bottomStart = if (isUser) 18.dp else 6.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (isQuotaWarning) Icons.Default.Warning else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isQuotaWarning) contentColor else PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                if (isQuotaWarning) "Limite temporário do Gemini" else "Assistente SobraAi",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isQuotaWarning) contentColor else PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    SelectionContainer {
                        Text(message.content, color = contentColor, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            message.displayTime?.let { time ->
                Text(
                    time, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
fun AssistantThinkingIndicator() {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppLoadingIndicator()
            Text("Pensando...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
    onSend: () -> Unit,
    showDisclaimer: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val isQuotaBlocked = (quotaRetrySeconds ?: 0) > 0
    val canUseVoice = enabled && !isQuotaBlocked
    val statusText = if (isListening) "Ouvindo..." else voiceError
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.background)
            .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statusText?.let {
            Text(
                it, color = if (isListening) PrimaryBlue else colors.error,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        Surface(color = colors.surface, shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled && !isQuotaBlocked,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).padding(horizontal = 10.dp),
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(PrimaryBlue),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text
                    ),
                    decorationBox = { innerTextField ->
                        Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    when {
                                        isListening -> "Fale sua mensagem..."
                                        isQuotaBlocked -> "Tente novamente em ${quotaRetrySeconds}s"
                                        else -> "Escreva sua mensagem..."
                                    },
                                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                IconButton(onClick = onVoiceClick, enabled = canUseVoice, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Parar gravação" else "Falar mensagem",
                        tint = when {
                            isListening -> colors.error
                            canUseVoice -> PrimaryBlue
                            else -> colors.onSurfaceVariant
                        }
                    )
                }
                FilledIconButton(
                    onClick = onSend, enabled = canSend, modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PrimaryBlue, contentColor = Color.White,
                        disabledContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                        disabledContentColor = colors.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Enviar mensagem")
                }
            }
        }
        if (showDisclaimer) {
            Text(
                "Revise as informações importantes sobre suas finanças.",
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }
    }
}

