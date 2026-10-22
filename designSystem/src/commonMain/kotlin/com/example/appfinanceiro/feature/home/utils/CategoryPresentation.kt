package com.example.appfinanceiro.feature.home.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
fun getCategoryIconAndColor(categoryName: String): Pair<ImageVector, Color> {
    return when (categoryName.lowercase()) {
        "aluguel", "moradia" -> Pair(Icons.Default.Home, DangerRed)
        "supermercado", "alimentação" -> Pair(Icons.Default.ShoppingCart, PrimaryBlue)
        "celular", "eletrônicos" -> Pair(Icons.Default.PhoneIphone, Color(0xFF9C27B0))
        "energia elétrica", "luz", "contas" -> Pair(Icons.Default.FlashOn, Color(0xFFFF9800))
        else -> Pair(Icons.Default.Receipt, PrimaryBlue)
    }
}