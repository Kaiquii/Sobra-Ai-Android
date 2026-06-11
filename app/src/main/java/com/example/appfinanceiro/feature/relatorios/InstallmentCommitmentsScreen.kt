package com.example.appfinanceiro.feature.relatorios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.GreenPositive
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.InstallmentCommitmentsResponse
import com.example.appfinanceiro.core.network.InstallmentParcel
import com.example.appfinanceiro.core.network.InstallmentPurchase
import com.example.appfinanceiro.core.network.InstallmentTimelineMonth
import com.example.appfinanceiro.feature.relatorios.utils.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentCommitmentsScreen(
    onNavigateBack: () -> Unit,
    onSessionExpired: () -> Unit = {},
    viewModel: InstallmentCommitmentsViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var includeCurrentMonthAsPaid by remember { mutableStateOf(false) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    LaunchedEffect(userToken, includeCurrentMonthAsPaid) {
        userToken?.let { token ->
            viewModel.loadCommitments(
                token = token,
                includeCurrentMonthAsPaid = includeCurrentMonthAsPaid
            )
        }
    }

    LaunchedEffect(uiState.isSessionExpired) {
        if (uiState.isSessionExpired) {
            sessionManager.clearSession()
            viewModel.clearSessionExpired()
            onSessionExpired()
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compromissos Parcelados",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { paddingValues ->
        val data = uiState.data

        when {
            uiState.isLoading && data == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }

            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage ?: "Erro ao carregar compromissos parcelados",
                    onRetry = {
                        userToken?.let { token ->
                            viewModel.loadCommitments(
                                token = token,
                                includeCurrentMonthAsPaid = includeCurrentMonthAsPaid
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                if (data == null || data.resumo.total_compras == 0) {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = paddingValues.calculateTopPadding() + 12.dp,
                            bottom = paddingValues.calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isLoading) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = PrimaryBlue,
                                    trackColor = PrimaryBlue.copy(alpha = 0.12f)
                                )
                            }
                        }

                        item {
                            CompactCurrentMonthPaymentToggle(
                                checked = includeCurrentMonthAsPaid,
                                onCheckedChange = { includeCurrentMonthAsPaid = it }
                            )
                        }

                        item {
                            SummarySection(data)
                        }

                        item {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = backgroundColor,
                                contentColor = PrimaryBlue
                            ) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("Compras") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("Calendario") }
                                )
                            }
                        }

                        if (selectedTab == 0) {
                            items(data.compras, key = { it.serie_id }) { purchase ->
                                PurchaseCard(purchase)
                            }
                        } else {
                            items(data.linha_do_tempo) { month ->
                                TimelineMonthCard(month)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCurrentMonthPaymentToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Mes atual pago",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue
            )
        )
    }
}

@Composable
private fun CurrentMonthPaymentToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Marcar mes atual como pago",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Quando ativo, a proxima parcela começa no mes seguinte.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryBlue
                )
            )
        }
    }
}

@Composable
private fun SummarySection(data: InstallmentCommitmentsResponse) {
    val summary = data.resumo

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Resumo",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total parcelado",
                value = formatCurrency(summary.total_original),
                subtitle = "${summary.total_compras} compras",
                icon = Icons.Default.CreditCard,
                iconColor = PrimaryBlue
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total pago",
                value = formatCurrency(summary.total_pago),
                subtitle = "${summary.parcelas_pagas} parcelas",
                icon = Icons.Default.Payments,
                iconColor = GreenPositive
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Falta pagar",
                value = formatCurrency(summary.total_restante),
                subtitle = "${summary.parcelas_restantes} parcelas",
                icon = Icons.Default.Schedule,
                iconColor = DangerRed
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Mes mais pesado",
                value = summary.mes_mais_pesado?.let { formatCurrency(it.total) } ?: "-",
                subtitle = summary.mes_mais_pesado?.let {
                    "${monthName(it.mes)}/${it.ano}"
                } ?: "Sem parcelas",
                icon = Icons.Default.TrendingUp,
                iconColor = Color(0xFFFF9800)
            )
        }

        Text(
            text = "Base: ${monthName(data.mes_base)}/${data.ano_base} - ${data.meses} meses",
            color = TextMuted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconColor.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PurchaseCard(purchase: InstallmentPurchase) {
    val progress = if (purchase.total_parcelas > 0) {
        purchase.parcelas_pagas.toFloat() / purchase.total_parcelas.toFloat()
    } else {
        0f
    }
    val currentInstallmentValue = purchase.proxima_parcela?.valor ?: purchase.valor_parcela

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = purchase.descricao,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${purchase.categoria_nome} - ${purchase.fonte_pagamento}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Parcela atual",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatCurrency(currentInstallmentValue),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = PrimaryBlue,
                trackColor = TextMuted.copy(alpha = 0.2f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetric(
                    modifier = Modifier.weight(1f),
                    label = "Restante",
                    value = formatCurrency(purchase.total_restante)
                )
                SmallMetric(
                    modifier = Modifier.weight(1f),
                    label = "Pago",
                    value = "${purchase.parcelas_pagas}/${purchase.total_parcelas}"
                )
            }

            purchase.proxima_parcela?.let { next ->
                Text(
                    text = "Proxima parcela: ${next.parcela_atual}/${next.total_parcelas} em ${monthName(next.mes)}/${next.ano} - ${formatCurrency(next.valor)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TimelineMonthCard(month: InstallmentTimelineMonth) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryBlue.copy(alpha = 0.13f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = "${monthName(month.mes)}/${month.ano}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "${month.parcelas.size} parcelas",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = formatCurrency(month.total),
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            if (month.parcelas.isEmpty()) {
                Text(
                    text = "Nenhuma parcela neste mes.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            } else {
                month.parcelas.forEach { parcel ->
                    ParcelRow(parcel)
                }
            }
        }
    }
}

@Composable
private fun ParcelRow(parcel: InstallmentParcel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = parcel.descricao,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${parcel.parcela_atual}/${parcel.total_parcelas} - ${parcel.categoria_nome} - ${parcel.fonte_pagamento}",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        Text(
            text = formatCurrency(parcel.valor),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SmallMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .background(TextMuted.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Voce ainda nao possui compras parceladas.",
            color = TextMuted,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Tentar novamente")
        }
    }
}

private fun monthName(month: Int): String {
    return when (month) {
        1 -> "Janeiro"
        2 -> "Fevereiro"
        3 -> "Marco"
        4 -> "Abril"
        5 -> "Maio"
        6 -> "Junho"
        7 -> "Julho"
        8 -> "Agosto"
        9 -> "Setembro"
        10 -> "Outubro"
        11 -> "Novembro"
        12 -> "Dezembro"
        else -> "-"
    }
}
