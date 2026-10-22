package com.example.appfinanceiro.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted

@Composable
fun MonthSelector(
    monthIndex: Int,
    currentYear: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    centerSuffix: String? = null
) {
    val meses = arrayOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")
    val prevMonthName = if (monthIndex == 0) meses[11] else meses[monthIndex - 1]
    val currMonthName = meses[monthIndex]
    val nextMonthName = if (monthIndex == 11) meses[0] else meses[monthIndex + 1]
    val shortYear = currentYear.toString().takeLast(2)
    val centerMonthName = if (centerSuffix != null) currMonthName.take(3) else currMonthName
    val centerText = centerSuffix?.let { "$centerMonthName/$shortYear • $it" } ?: "$centerMonthName/$shortYear"

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onPrevClick() }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior", tint = TextMuted)
            Text(prevMonthName.take(3), color = TextMuted)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Mês atual", tint = PrimaryBlue)
            Text(text = centerText, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.width(40.dp).padding(top = 4.dp), color = PrimaryBlue, thickness = 2.dp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNextClick() }) {
            Text(nextMonthName.take(3), color = TextMuted)
            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo", tint = TextMuted)
        }
    }
}
