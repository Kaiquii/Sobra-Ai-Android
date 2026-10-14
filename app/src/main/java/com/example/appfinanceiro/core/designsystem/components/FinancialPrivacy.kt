package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Stable
class FinancialPrivacyState(initiallyHidden: Boolean = false) {
    var valuesHidden by mutableStateOf(initiallyHidden)
        private set

    fun toggle() { valuesHidden = !valuesHidden }

    companion object {
        val Saver = Saver<FinancialPrivacyState, Boolean>(
            save = { it.valuesHidden },
            restore = { FinancialPrivacyState(it) }
        )
    }
}

val LocalFinancialPrivacy = staticCompositionLocalOf { FinancialPrivacyState() }

@Composable
fun FinancialPrivacyProvider(content: @Composable () -> Unit) {
    val state = rememberSaveable(saver = FinancialPrivacyState.Saver) { FinancialPrivacyState() }
    CompositionLocalProvider(LocalFinancialPrivacy provides state, content = content)
}

@Composable
fun FinancialPrivacyButton() {
    val state = LocalFinancialPrivacy.current
    IconButton(onClick = state::toggle) {
        Icon(
            imageVector = if (state.valuesHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (state.valuesHidden) "Mostrar valores" else "Ocultar valores",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun FinancialValue(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val hidden = LocalFinancialPrivacy.current.valuesHidden
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    // Mede o mesmo texto nos dois estados, preservando largura, altura e baseline.
    val valueModifier = if (hidden) modifier
        .clearAndSetSemantics { contentDescription = "Valor oculto" }
        .drawWithContent {
            drawRoundRect(
                color = skeletonColor,
                topLeft = Offset(0f, size.height * 0.22f),
                size = Size(size.width, size.height * 0.56f),
                cornerRadius = CornerRadius(5.dp.toPx())
            )
        } else modifier
    Text(text, valueModifier, color = color, fontSize = fontSize, fontWeight = fontWeight,
        style = style, textAlign = textAlign, maxLines = maxLines, overflow = overflow)
}

@Composable
fun PrivateReportContent(title: String, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        if (LocalFinancialPrivacy.current.valuesHidden) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .clearAndSetSemantics { contentDescription = "$title. Valores ocultos" }
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                repeat(3) {
                    Box(
                        Modifier.fillMaxWidth().height(18.dp).background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                            RoundedCornerShape(6.dp)
                        )
                    )
                }
            }
        } else content()
    }
}
