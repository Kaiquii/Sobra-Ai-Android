package com.example.appfinanceiro.feature.perfil.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjudaScreen(
    onNavigateBack: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            SettingsTopBar("Ajuda e Suporte", onNavigateBack)
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
            SettingsHeading(
                title = "Como podemos ajudar?",
                subtitle = "Encontre orientações para aproveitar o SobraAi.",
                icon = Icons.AutoMirrored.Filled.HelpOutline
            )
            SettingsCard {
                    SettingsHeading("Fale com a gente", "Canais de atendimento")
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

            Text("Explore o aplicativo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HelpSection(
                title = "Como usar o app",
                text = "O SobraAi ajuda você a organizar suas rendas, despesas e acompanhar o seu saldo mensal. Na tela inicial, você visualiza um resumo do mês selecionado, incluindo salário, adiantamento, renda extra, total gasto e valores restantes."
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
@Composable
private fun HelpSection(
    title: String,
    text: String
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column {
            Surface(
                onClick = { expanded = !expanded },
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().semantics {
                    stateDescription = if (expanded) "Expandido" else "Recolhido"
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Recolher tópico" else "Expandir tópico",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Text(text, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
