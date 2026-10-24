package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

enum class AppNotificationType { Info, Success, Warning, Error }

data class AppNotificationVisuals(
    override val message: String,
    val type: AppNotificationType = AppNotificationType.Info,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = true
}

/** Shared transient feedback; persistent load failures stay in their screen. */
@Composable
fun AppNotification(data: SnackbarData, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val type = (data.visuals as? AppNotificationVisuals)?.type ?: AppNotificationType.Info
    val accent = when (type) {
        AppNotificationType.Success -> GreenPositive
        AppNotificationType.Error, AppNotificationType.Warning -> colors.error
        AppNotificationType.Info -> PrimaryBlue
    }
    val icon = when (type) {
        AppNotificationType.Success -> Icons.Default.CheckCircle
        AppNotificationType.Error -> Icons.Default.ErrorOutline
        AppNotificationType.Warning -> Icons.Default.WarningAmber
        AppNotificationType.Info -> Icons.Default.Info
    }
    val hasCloseButton = type == AppNotificationType.Error || type == AppNotificationType.Warning
    Surface(
        modifier = modifier.widthIn(max = 380.dp).padding(horizontal = 12.dp)
            .then(if (hasCloseButton) Modifier else Modifier.clickable(onClickLabel = "Fechar notificação", onClick = data::dismiss))
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(start = 12.dp, top = 8.dp, bottom = 8.dp,
                end = if (hasCloseButton) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(data.visuals.message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f, fill = false))
            if (hasCloseButton) IconButton(onClick = data::dismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fechar notificação", tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}
