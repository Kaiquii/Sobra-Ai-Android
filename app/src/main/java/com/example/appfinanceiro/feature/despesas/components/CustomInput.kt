package com.example.appfinanceiro.feature.despesas.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun CustomInput(
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector?,
    placeholder: String,
    bgColor: Color,
    readOnly: Boolean = false,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable (() -> Unit))? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    val colorScheme = MaterialTheme.colorScheme
    val secondaryColor = colorScheme.onSurfaceVariant

    val leading: (@Composable () -> Unit)? = if (icon != null) {
        { Icon(icon, contentDescription = null, tint = secondaryColor) }
    } else {
        null
    }

    val trailing: (@Composable () -> Unit)? = when {
        trailingContent != null -> trailingContent
        trailingIcon != null -> {
            {
                if (onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick) {
                        Icon(trailingIcon, contentDescription = null, tint = secondaryColor)
                    }
                } else {
                    Icon(trailingIcon, contentDescription = null, tint = secondaryColor)
                }
            }
        }
        else -> null
    }

    TextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        placeholder = {
            Text(
                placeholder,
                color = secondaryColor
            )
        },
        leadingIcon = leading,
        trailingIcon = trailing,
        modifier = modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = bgColor,
            unfocusedContainerColor = bgColor,
            disabledContainerColor = bgColor,
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            disabledTextColor = colorScheme.onSurface,
            cursorColor = colorScheme.primary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = secondaryColor,
            unfocusedPlaceholderColor = secondaryColor,
            disabledPlaceholderColor = secondaryColor,
            focusedLeadingIconColor = secondaryColor,
            unfocusedLeadingIconColor = secondaryColor,
            disabledLeadingIconColor = secondaryColor,
            focusedTrailingIconColor = secondaryColor,
            unfocusedTrailingIconColor = secondaryColor,
            disabledTrailingIconColor = secondaryColor
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions
    )
}
