package com.example.appfinanceiro.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

@Composable
fun StandardBottomBar(
    itemSelecionado: Int,
    onItemClick: (Int) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val barColor = MaterialTheme.colorScheme.background.copy(alpha = 0.93f)
    val unselectedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
    val selectedIndicatorColor = PrimaryBlue.copy(alpha = 0.14f)
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.035f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(82.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 2.dp)
                    .blur(
                        radius = 12.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                    ),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.48f),
                tonalElevation = 0.dp
            ) {}

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(26.dp),
                color = barColor,
                contentColor = unselectedColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 10.dp,
                tonalElevation = 0.dp,
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentColor = unselectedColor,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
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
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(60.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f)
            ),
            shadowElevation = 3.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp,
                        focusedElevation = 8.dp,
                        hoveredElevation = 8.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar despesa",
                        tint = Color.White,
                        modifier = Modifier.size(29.dp)
                    )
                }
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
    val isSelected = selectedIndex == index
    val interactionSource = remember { MutableInteractionSource() }
    val itemColor = if (isSelected) PrimaryBlue else unselectedColor

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = { onClick(index) }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .widthIn(max = 52.dp)
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    color = if (isSelected) selectedIndicatorColor else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = itemColor,
                modifier = Modifier.size(23.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = itemColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
