package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted

enum class ExpenseCardStyle {
    Compact,
    Detailed
}

@Composable
fun ExpenseCard(
    style: ExpenseCardStyle,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    categoryName: String,
    paymentSource: String,
    type: String,
    date: String,
    value: String,
    notes: String?,
    onView: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    when (style) {
        ExpenseCardStyle.Compact -> CompactExpenseCard(
            icon = icon,
            iconColor = iconColor,
            title = title,
            categoryName = categoryName,
            paymentSource = paymentSource,
            type = type,
            date = date,
            value = value,
            notes = notes,
            onView = onView
        )

        ExpenseCardStyle.Detailed -> DetailedExpenseCard(
            icon = icon,
            iconColor = iconColor,
            title = title,
            categoryName = categoryName,
            paymentSource = paymentSource,
            type = type,
            date = date,
            value = value,
            notes = notes,
            onView = onView,
            onEdit = onEdit ?: {},
            onDelete = onDelete ?: {}
        )
    }
}

@Composable
private fun CompactExpenseCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    categoryName: String,
    paymentSource: String,
    type: String,
    date: String,
    value: String,
    notes: String?,
    onView: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardBg = MaterialTheme.colorScheme.surface
    val sourceColor = paymentSourceColor(paymentSource)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconColor.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = categoryName,
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniChip(label = type, color = TextMuted)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = date,
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
                if (!notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ExpenseNoteIndicatorIcon()
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onView() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Visualizar despesa",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .background(sourceColor.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Fonte",
                    tint = sourceColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = paymentSource,
                    color = sourceColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailedExpenseCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    categoryName: String,
    paymentSource: String,
    type: String,
    date: String,
    value: String,
    notes: String?,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardBg = colorScheme.surface
    val titleColor = colorScheme.onSurface
    val secondaryColor = colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = categoryName,
                    color = secondaryColor,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(TextMuted.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = type, color = TextMuted, fontSize = 10.sp)
                    }
                    Text(
                        text = " • $date",
                        color = secondaryColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    if (!notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        ExpenseNoteIndicatorIcon()
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Row {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onView() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Visualizar despesa",
                            tint = secondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = PrimaryBlue,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onEdit() }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Deletar",
                        tint = DangerRed,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onDelete() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = value,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = paymentSource, color = secondaryColor, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ExpenseNoteIndicatorIcon() {
    Box(
        modifier = Modifier
            .background(PrimaryBlue.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Despesa com observacao",
            tint = PrimaryBlue,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun MiniChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp)
    }
}

private fun paymentSourceColor(paymentSource: String): Color {
    return when {
        paymentSource.equals("Salario", ignoreCase = true) ||
            paymentSource.equals("Salário", ignoreCase = true) -> PrimaryBlue

        paymentSource.equals("Adiantamento", ignoreCase = true) -> Color(0xFF8B5CF6)
        paymentSource.equals("Renda Extra", ignoreCase = true) -> GreenPositive
        else -> TextMuted
    }
}
