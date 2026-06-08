package com.example.appfinanceiro.feature.perfil.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjudaScreen(
    onNavigateBack: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Central de Ajuda",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = surfaceColor
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Contato",
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    ContactItem(
                        icon = Icons.Default.Phone,
                        label = "WhatsApp",
                        value = "11 93367-3435"
                    )

                    ContactItem(
                        icon = Icons.Default.Email,
                        label = "E-mail",
                        value = "kaiqui.lucaskaiquiluc@gmail.com"
                    )
                }
            }

            HelpSection(
                title = "Como usar o app",
                text = "O SobraAí ajuda você a organizar suas rendas, despesas e acompanhar o seu saldo mensal. Na tela inicial, você visualiza um resumo do mês selecionado, incluindo salário, adiantamento, renda extra, total gasto e valores restantes."
            )

            HelpSection(
                title = "Tela inicial",
                text = "Use as setas do seletor de mês para navegar entre os meses. O resumo financeiro mostra quanto entrou, quanto saiu e quanto ainda está disponível. A seção de despesas exibe os lançamentos cadastrados no mês."
            )

            HelpSection(
                title = "Cadastrar despesas",
                text = "Toque no botão central de adicionar para criar uma nova despesa. Informe o valor, descrição, categoria, data, origem do pagamento e tipo da despesa. Despesas únicas aparecem apenas no mês escolhido. Despesas fixas e parceladas podem continuar nos meses seguintes conforme a configuração escolhida."
            )

            HelpSection(
                title = "Gerenciar despesas",
                text = "Na aba Despesas, você pode buscar lançamentos, filtrar por tipo, editar uma despesa existente ou excluir uma despesa. Ao excluir despesas parceladas ou fixas, o app pode perguntar se você deseja remover também os próximos meses."
            )

            HelpSection(
                title = "Rendas",
                text = "Na área de Perfil, acesse Configurações de Renda para cadastrar salário, adiantamento e renda extra. Salário e adiantamento são usados para calcular o orçamento mensal. A renda extra pode ser cadastrada apenas no mês atual ou repetida nos próximos meses, caso você marque essa opção."
            )

            HelpSection(
                title = "Categorias",
                text = "Em Categorias, você pode criar, editar e remover categorias usadas para organizar suas despesas. Elas ajudam a visualizar melhor para onde o seu dinheiro está indo."
            )

            HelpSection(
                title = "Relatórios",
                text = "A aba Relatórios mostra uma visão mais detalhada dos seus gastos, comparando receitas e despesas e destacando os valores por categoria. Use essa área para entender seus hábitos financeiros ao longo dos meses."
            )

            HelpSection(
                title = "Biometria",
                text = "Após fazer login, o app pode perguntar se você deseja ativar a biometria. Se ativada, você poderá entrar com sua digital ou reconhecimento disponível no aparelho, deixando o acesso mais rápido e seguro."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val textColor = MaterialTheme.colorScheme.onBackground

    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue
        )

        Column(
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    text: String
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = text,
                color = TextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
