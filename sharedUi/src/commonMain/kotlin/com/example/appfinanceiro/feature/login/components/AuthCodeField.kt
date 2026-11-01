package com.example.appfinanceiro.feature.login.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One editable value (including paste/backspace), represented by six visual cells. */
@Composable
fun AuthCodeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var focused by remember { mutableStateOf(false) }
    Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .onFocusChanged { focused = it.isFocused }.semantics { contentDescription = label },
        decorationBox = { input ->
            Box {
                Row(Modifier.fillMaxWidth().clearAndSetSemantics { },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        val active = focused && enabled && index == value.length.coerceAtMost(5)
                        Surface(Modifier.weight(1f).heightIn(min = 56.dp),
                            shape = RoundedCornerShape(12.dp), color = authFieldSurface(),
                            border = BorderStroke(if (active) 2.dp else 1.dp,
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(value.getOrNull(index)?.toString().orEmpty(), fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
                Box(Modifier.matchParentSize()) { input() }
            }
        }
    )
}
