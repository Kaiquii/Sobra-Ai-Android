package com.example.appfinanceiro.feature.despesas.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.appfinanceiro.core.designsystem.theme.*
import com.example.appfinanceiro.feature.home.components.MonthSelector

@Composable
fun ExpenseListControls(searchQuery: String, onSearchQueryChange: (String) -> Unit,
    selectedFilter: String, onFilterSelect: (String) -> Unit, filtersActive: Boolean, onShowFilters: () -> Unit,
    currentMonthIndex: Int, currentYear: Int, centerSuffix: String?, onPrevClick: () -> Unit, onNextClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val inputBgColor = colorScheme.surface
    val surfaceTextColor = colorScheme.onSurface
    val secondaryTextColor = colorScheme.onSurfaceVariant
    val expenseFilters = listOf("Todas", "Parceladas", "Únicas", "Fixas")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f),
                    placeholder = {
                        Text("Buscar despesa...", color = secondaryTextColor)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            tint = secondaryTextColor,
                            contentDescription = null
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = inputBgColor,
                        unfocusedContainerColor = inputBgColor,
                        disabledContainerColor = inputBgColor,
                        focusedTextColor = surfaceTextColor,
                        unfocusedTextColor = surfaceTextColor,
                        cursorColor = PrimaryBlue,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedPlaceholderColor = secondaryTextColor,
                        unfocusedPlaceholderColor = secondaryTextColor,
                        focusedLeadingIconColor = secondaryTextColor,
                        unfocusedLeadingIconColor = secondaryTextColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                ExpenseFilterButton(
                    active = filtersActive,
                    onClick = {
                        onShowFilters()
                    }
                )
            }

            val filterChipBg = TextMuted.copy(alpha = 0.2f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                expenseFilters.forEach { filter ->
                    val isSelected = selectedFilter == filter

                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) PrimaryBlue else filterChipBg,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onFilterSelect(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            MonthSelector(
                monthIndex = currentMonthIndex,
                currentYear = currentYear,
                onPrevClick = onPrevClick,
                onNextClick = onNextClick,
                centerSuffix = centerSuffix
            )


}
