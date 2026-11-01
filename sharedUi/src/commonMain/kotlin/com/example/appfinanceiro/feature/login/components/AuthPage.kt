package com.example.appfinanceiro.feature.login.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

/** Shared authentication shell. Routes, credentials and requests belong to its consumers. */
@Composable
fun AuthPage(
    onBack: (() -> Unit)? = null,
    enabled: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, enabled = enabled,
                        modifier = Modifier.offset(x = (-12).dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
                Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null,
                        tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("SobraAi", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val viewportHeight = maxHeight
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), content = content)
                    if (footer != null) {
                        Box(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                            contentAlignment = Alignment.Center) { footer() }
                    }
                }
            }
        }
    }
}
