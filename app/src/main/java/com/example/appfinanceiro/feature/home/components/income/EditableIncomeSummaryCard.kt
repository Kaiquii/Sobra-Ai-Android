package com.example.appfinanceiro.feature.home.components.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.designsystem.components.FinancialValue

@Composable
fun EditableIncomeSummaryCard(
    title: String,
    value: String,
    bgColor: Color,
    valueColor: Color,
    existingIncome: Income?,
    modifier: Modifier = Modifier,
    valueSize: TextUnit = 18.sp,
    icon: ImageVector? = null,
    onCreate: () -> Unit,
    onEdit: (Income) -> Unit,
    onDelete: (Income) -> Unit,
    actionsEnabled: Boolean = true
) {
    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )


                if (existingIncome != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onEdit(existingIncome) },
                            enabled = actionsEnabled,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onDelete(existingIncome) },
                            enabled = actionsEnabled,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onCreate,
                        enabled = actionsEnabled,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Criar",
                            tint = GreenPositive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FinancialValue(
                    value,
                    color = valueColor,
                    fontSize = valueSize,
                    fontWeight = FontWeight.Bold
                )

                if (icon != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = valueColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
