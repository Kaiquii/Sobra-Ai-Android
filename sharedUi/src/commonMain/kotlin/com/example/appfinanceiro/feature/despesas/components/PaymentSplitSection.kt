package com.example.appfinanceiro.feature.despesas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import br.com.sobraai.ui.runtime.NumberFormat
import br.com.sobraai.ui.runtime.Locale

data class PaymentSplitInput(
    val source: String,
    val amountText: String
)

fun paymentSplitValidationMessage(amount: Double, splits: List<PaymentSplitInput>): String? {
    if (splits.isEmpty()) return "Adicione pelo menos uma origem de pagamento."
    if (splits.map { it.source }.distinct().size != splits.size) {
        return "Não repita uma origem de pagamento."
    }

    val splitTotal = splits.sumOf { it.amountText.toAmount() ?: 0.0 }
    if (splits.any { (it.amountText.toAmount() ?: 0.0) <= 0.0 }) {
        return "Cada valor da divisão deve ser maior que zero."
    }
    if (kotlin.math.abs(splitTotal - amount) > 0.009) {
        return "Distribua todo o valor da despesa entre as origens."
    }
    return null
}

fun String.toAmount(): Double? = replace(",", ".").toDoubleOrNull()

@Composable
fun PaymentSplitSection(
    sources: List<String>,
    splits: List<PaymentSplitInput>,
    totalAmount: Double,
    onSplitsChange: (List<PaymentSplitInput>) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val formattedTotal = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(totalAmount)
    val distributed = splits.sumOf { it.amountText.toAmount() ?: 0.0 }
    val isComplete = kotlin.math.abs(distributed - totalAmount) <= 0.009 &&
        splits.all { (it.amountText.toAmount() ?: 0.0) > 0.0 }
    val formattedDistributed = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(distributed)
    val distributionLabel = when {
        totalAmount <= 0.0 -> "Informe o valor da despesa para dividir o pagamento."
        isComplete -> "Pagamento distribuído: $formattedDistributed de $formattedTotal"
        else -> "Distribuído: $formattedDistributed de $formattedTotal"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FormLabel("Pagamento dividido")
        Text(
            "Escolha as origens e distribua o valor da despesa.",
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )

        splits.forEachIndexed { index, split ->
            PaymentSplitRow(
                index = index,
                sources = sources,
                selectedSources = splits.map { it.source },
                split = split,
                canRemove = splits.size > 1,
                onChange = { updated ->
                    onSplitsChange(splits.toMutableList().apply { this[index] = updated })
                },
                onRemove = { onSplitsChange(splits.filterIndexed { itemIndex, _ -> itemIndex != index }) }
            )
        }

        if (splits.size < sources.size) {
            TextButton(
                onClick = {
                    val availableSource = sources.first { source -> splits.none { it.source == source } }
                    onSplitsChange(splits + PaymentSplitInput(availableSource, ""))
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue)
                Spacer(Modifier.width(4.dp))
                Text("Adicionar origem", color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = distributionLabel,
            color = when {
                totalAmount <= 0.0 -> colorScheme.onSurfaceVariant
                isComplete -> PrimaryBlue
                else -> colorScheme.error
            },
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PaymentSplitRow(
    index: Int,
    sources: List<String>,
    selectedSources: List<String>,
    split: PaymentSplitInput,
    canRemove: Boolean,
    onChange: (PaymentSplitInput) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val availableSources = sources.filter { source ->
        source == split.source || source !in selectedSources
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CustomDropdown(
            label = if (index == 0) "Primeira origem" else "Origem ${index + 1}",
            selectedValue = split.source,
            options = availableSources,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onSelect = {
                onChange(split.copy(source = availableSources[it]))
                expanded = false
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CustomInput(
                value = split.amountText,
                onValueChange = { value ->
                    val formatted = value.replace(".", ",")
                    if (formatted.count { it == ',' } <= 1 && formatted.all { it.isDigit() || it == ',' }) {
                        onChange(split.copy(amountText = formatted))
                    }
                },
                icon = null,
                placeholder = "Valor",
                bgColor = MaterialTheme.colorScheme.surface,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            if (canRemove) {
                androidx.compose.material3.IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remover origem ${index + 1}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
