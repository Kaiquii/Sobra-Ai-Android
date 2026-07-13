package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

@Composable
fun StandardBottomBar(
    itemSelecionado: Int,
    onItemClick: (Int) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val bgColor = MaterialTheme.colorScheme.background
    val unselectedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val selectedIndicatorColor = PrimaryBlue.copy(alpha = 0.16f)

    Box(contentAlignment = Alignment.BottomCenter) {
        NavigationBar(containerColor = bgColor, contentColor = unselectedColor) {
            BottomBarItem(
                index = 0,
                selectedIndex = itemSelecionado,
                icon = Icons.Default.Home,
                label = "Início",
                unselectedColor = unselectedColor,
                selectedIndicatorColor = selectedIndicatorColor,
                onClick = onItemClick
            )

            BottomBarItem(
                index = 1,
                selectedIndex = itemSelecionado,
                icon = Icons.Default.Receipt,
                label = "Despesas",
                unselectedColor = unselectedColor,
                selectedIndicatorColor = selectedIndicatorColor,
                onClick = onItemClick
            )

            NavigationBarItem(
                icon = { },
                label = { },
                selected = false,
                onClick = { },
                enabled = false
            )

            BottomBarItem(
                index = 2,
                selectedIndex = itemSelecionado,
                icon = Icons.Default.PieChart,
                label = "Relatórios",
                unselectedColor = unselectedColor,
                selectedIndicatorColor = selectedIndicatorColor,
                onClick = onItemClick
            )

            BottomBarItem(
                index = 3,
                selectedIndex = itemSelecionado,
                icon = Icons.Default.Person,
                label = "Perfil",
                unselectedColor = unselectedColor,
                selectedIndicatorColor = selectedIndicatorColor,
                onClick = onItemClick
            )
        }

        Box(
            modifier = Modifier
                .offset(y = (-40).dp)
                .size(68.dp)
                .background(bgColor, CircleShape)
                .padding(6.dp)
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = PrimaryBlue,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar despesa",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    index: Int,
    selectedIndex: Int,
    icon: ImageVector,
    label: String,
    unselectedColor: Color,
    selectedIndicatorColor: Color,
    onClick: (Int) -> Unit
) {
    NavigationBarItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(23.dp)
            )
        },
        label = {
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp
            )
        },
        selected = selectedIndex == index,
        onClick = { onClick(index) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = PrimaryBlue,
            selectedTextColor = PrimaryBlue,
            indicatorColor = selectedIndicatorColor,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor
        )
    )
}
