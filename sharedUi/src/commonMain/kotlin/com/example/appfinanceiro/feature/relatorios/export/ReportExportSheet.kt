package com.example.appfinanceiro.feature.relatorios.export

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.example.appfinanceiro.core.designsystem.components.AppSelectField
import com.example.appfinanceiro.core.designsystem.components.AppSelectOption

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.date.shiftMonth
import br.com.sobraai.ui.runtime.DateFormatSymbols
import br.com.sobraai.ui.runtime.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportExportSheet(
    month: Int,
    year: Int,
    initialCompareMonth: Int,
    initialCompareYear: Int,
    isExporting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onExport: (ReportExportRequest) -> Unit,
    onCancelExport: () -> Unit
) {
    var selectedTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFormatName by rememberSaveable { mutableStateOf<String?>(null) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var useCustomComparison by rememberSaveable { mutableStateOf(false) }
    var compareMonth by rememberSaveable { mutableStateOf(initialCompareMonth) }
    var compareYear by rememberSaveable { mutableStateOf(initialCompareYear) }
    var projectionMonths by rememberSaveable { mutableFloatStateOf(12f) }
    var includeCurrentMonthAsPaid by rememberSaveable { mutableStateOf(false) }

    val selectedType = selectedTypeName?.let(ReportExportType::valueOf)
    val selectedFormat = selectedFormatName?.let(ReportExportFormat::valueOf)

    ModalBottomSheet(
        onDismissRequest = { if (!isExporting) onDismiss() },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Exportar relatório",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Período: ${monthName(month)}/$year",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(20.dp))
            AppSelectField(
                label = "Tipo do relatório", value = selectedType?.label.orEmpty(),
                options = ReportExportType.entries.map { AppSelectOption<ReportExportType?>(it, it.label) },
                selected = selectedType, expanded = typeMenuExpanded,
                enabled = !isExporting, placeholder = "Selecione o tipo",
                onExpandedChange = { typeMenuExpanded = it },
                onSelect = { selectedTypeName = it?.name }
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("Formato", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportExportFormat.entries.forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { if (!isExporting) selectedFormatName = format.name },
                        label = { Text(format.label) },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (selectedType?.supportsComparison == true) {
                Spacer(modifier = Modifier.height(18.dp))
                SettingSwitchRow(
                    title = "Escolher período comparado",
                    subtitle = "Desativado: compara automaticamente com o mês anterior",
                    checked = useCustomComparison,
                    enabled = !isExporting,
                    onCheckedChange = { useCustomComparison = it }
                )

                if (useCustomComparison) {
                    Spacer(modifier = Modifier.height(10.dp))
                    PeriodPicker(
                        month = compareMonth,
                        year = compareYear,
                        enabled = !isExporting,
                        onChange = { amount ->
                            val changed = changeMonth(compareMonth, compareYear, amount)
                            val validPeriod = if (changed.first == month && changed.second == year) {
                                changeMonth(changed.first, changed.second, amount)
                            } else {
                                changed
                            }
                            compareMonth = validPeriod.first
                            compareYear = validPeriod.second
                        }
                    )
                }
            }

            if (selectedType?.supportsInstallments == true) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Projeção: ${projectionMonths.roundToInt()} meses",
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = projectionMonths,
                    onValueChange = { projectionMonths = it },
                    valueRange = 1f..60f,
                    steps = 58,
                    enabled = !isExporting
                )
                SettingSwitchRow(
                    title = "Considerar mês atual como pago",
                    subtitle = "Ajusta a projeção dos compromissos parcelados",
                    checked = includeCurrentMonthAsPaid,
                    enabled = !isExporting,
                    onCheckedChange = { includeCurrentMonthAsPaid = it }
                )
            }

            errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            if (isExporting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    Text("Gerando e salvando relatório...")
                }
                TextButton(
                    onClick = onCancelExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar operação", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Button(
                    onClick = {
                        val type = selectedType ?: return@Button
                        val format = selectedFormat ?: return@Button
                        onExport(
                            ReportExportRequest(
                                type = type,
                                month = month,
                                year = year,
                                format = format,
                                compareMonth = compareMonth.takeIf {
                                    type.supportsComparison && useCustomComparison
                                },
                                compareYear = compareYear.takeIf {
                                    type.supportsComparison && useCustomComparison
                                },
                                months = projectionMonths.roundToInt().takeIf {
                                    type.supportsInstallments
                                },
                                includeCurrentMonthAsPaid = includeCurrentMonthAsPaid.takeIf {
                                    type.supportsInstallments
                                }
                            )
                        )
                    },
                    enabled = selectedType != null && selectedFormat != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Baixar relatório", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun PeriodPicker(
    month: Int,
    year: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onChange(-1) }, enabled = enabled) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Mês anterior")
        }
        Text(
            text = "${monthName(month)}/$year",
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onChange(1) }, enabled = enabled) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Próximo mês")
        }
    }
}

private fun changeMonth(month: Int, year: Int, amount: Int): Pair<Int, Int> {
    val target = shiftMonth(month - 1, year, amount)
    return (target.monthIndex + 1) to target.year
}

private fun monthName(month: Int): String {
    val names = DateFormatSymbols(Locale.forLanguageTag("pt-BR")).months
    return names.getOrNull(month - 1)
        ?.replaceFirstChar { it.uppercase() }
        ?: month.toString().padStart(2, '0')
}
