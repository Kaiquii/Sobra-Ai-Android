package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.designsystem.theme.datePickerContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    state: DatePickerState,
    onConfirm: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface,
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = colorScheme.outline,
        focusedLabelColor = PrimaryBlue,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        cursorColor = PrimaryBlue
    )
    val datePickerColors = DatePickerDefaults.colors(
        containerColor = colorScheme.datePickerContainer,
        titleContentColor = colorScheme.onSurfaceVariant,
        headlineContentColor = colorScheme.onSurface,
        weekdayContentColor = colorScheme.onSurface,
        subheadContentColor = colorScheme.onSurface,
        navigationContentColor = colorScheme.onSurface,
        yearContentColor = colorScheme.onSurface,
        disabledYearContentColor = colorScheme.onSurface.copy(alpha = 0.38f),
        currentYearContentColor = PrimaryBlue,
        selectedYearContentColor = Color.White,
        disabledSelectedYearContentColor = Color.White.copy(alpha = 0.38f),
        selectedYearContainerColor = PrimaryBlue,
        disabledSelectedYearContainerColor = PrimaryBlue.copy(alpha = 0.38f),
        dayContentColor = colorScheme.onSurface,
        disabledDayContentColor = colorScheme.onSurface.copy(alpha = 0.38f),
        selectedDayContentColor = Color.White,
        disabledSelectedDayContentColor = Color.White.copy(alpha = 0.38f),
        selectedDayContainerColor = PrimaryBlue,
        disabledSelectedDayContainerColor = PrimaryBlue.copy(alpha = 0.38f),
        todayContentColor = PrimaryBlue,
        todayDateBorderColor = PrimaryBlue,
        dayInSelectionRangeContentColor = colorScheme.onSurface,
        dayInSelectionRangeContainerColor = PrimaryBlue.copy(alpha = 0.16f),
        dividerColor = colorScheme.outlineVariant,
        dateTextFieldColors = textFieldColors
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.selectedDateMillis) }) {
                Text("OK", color = PrimaryBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMuted)
            }
        },
        tonalElevation = 0.dp,
        colors = datePickerColors
    ) {
        Box {
            key(state.displayMode) {
                DatePicker(
                    state = state,
                    showModeToggle = false,
                    colors = datePickerColors
                )
            }

            val inputMode = state.displayMode == DisplayMode.Input
            IconButton(
                onClick = {
                    state.displayMode = if (inputMode) DisplayMode.Picker else DisplayMode.Input
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 42.dp, end = 12.dp)
            ) {
                Icon(
                    imageVector = if (inputMode) Icons.Default.DateRange else Icons.Default.Edit,
                    contentDescription = if (inputMode) {
                        "Selecionar data no calendario"
                    } else {
                        "Digitar data"
                    },
                    tint = colorScheme.onSurface
                )
            }
        }
    }
}
