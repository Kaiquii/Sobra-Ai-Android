package com.example.appfinanceiro.feature.home.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import br.com.sobraai.ui.runtime.NumberFormat
import br.com.sobraai.ui.runtime.Locale

fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return format.format(value)
}

fun formatExpenseDate(dateString: String): String {
    return try {
        val parts = dateString.split("T")[0].split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}" else dateString
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        dateString
    }
}
