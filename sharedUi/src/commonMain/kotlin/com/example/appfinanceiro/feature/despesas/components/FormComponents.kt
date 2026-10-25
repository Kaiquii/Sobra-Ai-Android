package com.example.appfinanceiro.feature.despesas.components

import com.example.appfinanceiro.core.designsystem.components.AppSelectField
import com.example.appfinanceiro.core.designsystem.components.AppSelectOption

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun ExpenseValueInput(amountText: String, onAmountChange: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val secondaryColor = colorScheme.onSurfaceVariant
    val primaryText = colorScheme.onBackground

    Text(
        text = "Valor da Despesa",
        color = secondaryColor,
        fontSize = 14.sp
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "R$",
            color = secondaryColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(8.dp))

        BasicTextField(
            value = amountText,
            onValueChange = { newValue ->
                val formatado = newValue.replace(".", ",")
                if (formatado.count { it == ',' } <= 1 && formatado.all { it.isDigit() || it == ',' }) {
                    onAmountChange(formatado)
                }
            },
            textStyle = TextStyle(
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                color = primaryText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(PrimaryBlue),
            modifier = Modifier.width(IntrinsicSize.Min),
            decorationBox = { innerTextField ->
                if (amountText.isEmpty()) {
                    Text(
                        text = "0,00",
                        color = secondaryColor,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                innerTextField()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit
) {
    AppSelectField(
        label = label, value = selectedValue,
        options = options.mapIndexed { index, text -> AppSelectOption(index, text) },
        selected = options.indexOf(selectedValue), expanded = expanded,
        onExpandedChange = onExpandedChange, onSelect = onSelect,
        anchor = { open ->
            FormLabel(label)
            Box {
                CustomInput(value = selectedValue, onValueChange = {}, icon = null, placeholder = "",
                    bgColor = MaterialTheme.colorScheme.surface, readOnly = true,
                    trailingIcon = Icons.Default.ArrowDropDown)
                Box(Modifier.matchParentSize().semantics { contentDescription = "$label: $selectedValue" }
                    .clickable(role = Role.Button, onClick = open))
            }
        }
    )
}

@Composable
fun CategoryDropdown(
    selectedValue: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    AppSelectField(
        label = "Categoria", value = selectedValue,
        options = options.mapIndexed { index, text -> AppSelectOption(index, text) },
        selected = options.indexOf(selectedValue), expanded = expanded,
        onExpandedChange = onExpandedChange, onSelect = onSelect,
        anchor = { open ->
            FormLabel("Categoria")
            Box {
                CustomInput(
                    value = selectedValue, onValueChange = {}, icon = null, placeholder = "",
                    bgColor = MaterialTheme.colorScheme.surface, readOnly = true,
                    trailingContent = {
                        Row(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = open).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = onAddCategoryClick, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Add, "Criar categoria", tint = GreenPositive)
                            }
                            Icon(Icons.Default.ArrowDropDown, "Abrir categorias", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
                Box(Modifier.matchParentSize().padding(end = 96.dp)
                    .semantics { contentDescription = "Categoria: $selectedValue" }
                    .clickable(role = Role.Button, onClick = open))
            }
        }
    )
}

@Composable
fun PaymentTypeSelector(
    selectedType: String,
    onTypeChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val inputBgColor = colorScheme.surface
    val secondaryColor = colorScheme.onSurfaceVariant
    val options = listOf("Única", "Parcelada", "Fixa")

    FormLabel("Tipo de Pagamento")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(inputBgColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        options.forEach { option ->
            val isSelected = selectedType.equals(option, ignoreCase = true)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) PrimaryBlue.copy(alpha = 0.14f) else Color.Transparent
                    )
                    .clickable { onTypeChange(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (isSelected) PrimaryBlue else secondaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InstallmentCounter(installments: Int, onInstallmentChange: (Int) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val inputBgColor = colorScheme.surface
    val textColor = colorScheme.onSurface

    Spacer(modifier = Modifier.height(16.dp))
    FormLabel("Número de Parcelas")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(inputBgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "-",
            color = textColor,
            fontSize = 24.sp,
            modifier = Modifier
                .clickable {
                    if (installments > 1) onInstallmentChange(installments - 1)
                }
                .padding(8.dp)
        )

        Text(
            text = "${installments}x",
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "+",
            color = textColor,
            fontSize = 24.sp,
            modifier = Modifier
                .clickable { onInstallmentChange(installments + 1) }
                .padding(8.dp)
        )
    }
}
