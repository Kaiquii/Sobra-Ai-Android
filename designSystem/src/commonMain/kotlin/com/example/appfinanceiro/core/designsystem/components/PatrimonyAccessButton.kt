package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

@Composable
fun PatrimonyAccessButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.semantics { role = Role.Button },
        shape = RoundedCornerShape(16.dp), color = PrimaryBlue, contentColor = Color.White,
        shadowElevation = 4.dp) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountBalanceWallet, "Abrir patrimônio", Modifier.size(24.dp))
        }
    }
}
