package br.com.sobraai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appfinanceiro.core.network.Category
import br.com.sobraai.ui.components.*

@Composable
internal fun ProfileScreen(workspace: FinanceWorkspace, presenter: ProfilePresenter, logout: () -> Unit) {
    val state by presenter.profile.state.collectAsState()
    val action by presenter.action.state.collectAsState()
    val avatar by presenter.avatar.collectAsState()
    val photoError by presenter.photoError.collectAsState()
    val biometric by presenter.biometricEnabled.collectAsState()
    val profile = state.data?.value
    var editing by remember { mutableStateOf(false) }; var removing by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }; var preview by remember { mutableStateOf(false) }
    LaunchedEffect(action.message) { if (action.message != null) editing = false }
    val photo = remember(avatar) { avatar?.let { runCatching { decodePhoto(it) }.getOrNull() } }
    LaunchedEffect(profile?.avatar_url) { profile?.let { presenter.loadPhoto(it.avatar_url.orEmpty()) } }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader("Meu perfil", { workspace.navigate(FinanceRoute.Home) }) }, bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Profile) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            QueryFeedback(state.loading, state.error, if (profile != null) "Perfil" else null, "Perfil", presenter.profile::refresh)
            FeatureActionFeedback(action)
            if (photo != null) Image(photo, "Foto de perfil", Modifier.size(120.dp).clickable { preview = true }, contentScale = ContentScale.Crop)
            else Text(profile?.name?.take(1)?.uppercase() ?: "SobraAi", style = MaterialTheme.typography.headlineLarge)
            photoError?.let { Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { presenter.loadPhoto(profile?.avatar_url.orEmpty()) }, enabled = !action.busy) { Text("Carregar foto novamente") } }
            Text(profile?.name ?: "Perfil não carregado", style = MaterialTheme.typography.titleLarge)
            profile?.let {
                Text(it.email)
                Text(if (it.role.equals("admin", true)) "Administrador" else "Usuário")
            }
            Button(onClick = { presenter.action.reset(); editing = true }, enabled = profile != null && !action.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Editar perfil") }
            OutlinedButton(onClick = presenter::photo, enabled = profile != null && !action.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Alterar foto") }
            if (!profile?.avatar_url.isNullOrBlank()) TextButton(onClick = { removing = true }, enabled = !action.busy) { Text("Remover foto", color = MaterialTheme.colorScheme.error) }
            FormCheck("Usar Face ID ou Touch ID para proteger esta conta", biometric, presenter::biometric, !action.busy && profile != null)
            ProfileLink("Configurações de Renda") { workspace.navigate(FinanceRoute.Incomes) }
            ProfileLink("Categorias") { workspace.navigate(FinanceRoute.Categories) }
            ProfileLink("Ajuda e Suporte") { workspace.navigate(FinanceRoute.Help) }
            TextButton(onClick = { exiting = true }) { Text("Sair da conta", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (editing && profile != null) {
        var name by remember(profile.name) { mutableStateOf(profile.name) }
        var email by remember(profile.email) { mutableStateOf(profile.email) }
        AlertDialog(onDismissRequest = { if (!action.busy) editing = false }, containerColor = MaterialTheme.colorScheme.background,
            title = { Text("Editar perfil") }, text = {
                Column(Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormInput("Nome", name, { name = it }, !action.busy)
                    FormInput("E-mail", email, { email = it }, !action.busy, KeyboardType.Email)
                    FeatureActionFeedback(action)
                }
            }, confirmButton = { TextButton(onClick = { presenter.save(name, email) }, enabled = !action.busy) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { editing = false }, enabled = !action.busy) { Text("Fechar") } })
    }
    if (removing) ConfirmDialog("Remover foto?", "Sua foto de perfil será removida.", "Remover", { removing = false; presenter.removePhoto() }, { removing = false })
    if (exiting) ConfirmDialog("Sair da conta?", "Será necessário entrar novamente para acessar seus dados.", "Sair", logout, { exiting = false })
    if (preview && photo != null) AlertDialog(onDismissRequest = { preview = false }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Foto de perfil") }, text = { Image(photo, "Foto de perfil ampliada", Modifier.fillMaxWidth().heightIn(max = 400.dp), contentScale = ContentScale.Fit) },
        confirmButton = { TextButton(onClick = { preview = false }) { Text("Fechar") } })
}

@Composable
internal fun CategoriesScreen(workspace: FinanceWorkspace, presenter: ProfilePresenter) {
    val state by presenter.categories.state.collectAsState(); val action by presenter.action.state.collectAsState()
    var edit by remember { mutableStateOf<Category?>(null) }; var open by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }; var deleting by remember { mutableStateOf<Category?>(null) }
    LaunchedEffect(action.message) { if (action.message != null) open = false }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader("Categorias", { workspace.navigate(FinanceRoute.Profile) }) }, bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Categories) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { QueryFeedback(state.loading, state.error, state.data?.let { "Categorias" }, "Categorias", presenter.categories::refresh) }
            item { FeatureActionFeedback(action) }
            item { Button(onClick = { presenter.action.reset(); edit = null; name = ""; open = true }, enabled = !action.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Criar categoria") } }
            if (state.data?.value?.isEmpty() == true) item { Text("Nenhuma categoria cadastrada.") }
            items(state.data?.value.orEmpty(), key = { it.id }) { category ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(category.name, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { presenter.action.reset(); edit = category; name = category.name; open = true }, enabled = !action.busy) { Text("Editar") }
                    TextButton(onClick = { deleting = category }, enabled = !action.busy) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                } }
            }
        }
    }
    if (open) AlertDialog(onDismissRequest = { if (!action.busy) open = false }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text(if (edit == null) "Criar categoria" else "Editar categoria") }, text = {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormInput("Nome", name, { name = it }, !action.busy); FeatureActionFeedback(action)
            }
        }, confirmButton = { TextButton(onClick = { presenter.category(name, edit) }, enabled = !action.busy) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { open = false }, enabled = !action.busy) { Text("Fechar") } })
    deleting?.let { category -> ConfirmDialog("Excluir categoria?", "Deseja excluir ${category.name}?", "Excluir", {
        deleting = null; presenter.deleteCategory(category)
    }, { deleting = null }) }
}

@Composable
private fun ProfileLink(text: String, click: () -> Unit) { OutlinedButton(onClick = click, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(text) } }
@Composable
internal fun HelpScreen(workspace: FinanceWorkspace, native: NativeFeatures) {
    val topics = listOf(
        "Como usar o app" to "Organize rendas e despesas e acompanhe o saldo de cada mês. Use as setas para mudar o período.",
        "Despesas" to "Crie despesas únicas, fixas ou parceladas. Confira a confirmação antes de alterar ou excluir ocorrências futuras. Os rateios precisam distribuir todo o valor entre as origens.",
        "Adiantamentos" to "A despesa continua visível no mês previsto e entra no cálculo da data escolhida. Adiantar não muda o status Paga.",
        "Rendas e categorias" to "No Perfil, gerencie rendas e categorias. A renda extra pode ser repetida nos próximos meses quando essa opção for selecionada.",
        "Relatórios" to "Compare meses, acompanhe categorias e compromissos parcelados. Exporte PDF, Excel ou CSV e escolha onde salvar ou compartilhar.",
        "Face ID e Touch ID" to "A Apple escolhe a biometria disponível no seu aparelho. A ativação exige uma autenticação. Quando a proteção está ativa, o app bloqueia ao entrar em segundo plano. Você pode encerrar a sessão e entrar com senha.",
        "Assistente e voz" to "A voz preenche o campo de mensagem. Confira a transcrição antes de enviar. Uma criação de despesa só é considerada concluída após a confirmação do servidor.",
        "Cadastro e recuperação no iOS" to "O envio de códigos depende da habilitação da proteção Apple no servidor. Essa integração ainda está pendente; login com conta existente continua disponível.")
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader("Ajuda e Suporte", { workspace.navigate(FinanceRoute.Profile) }) }, bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Help) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Como podemos ajudar?", style = MaterialTheme.typography.titleLarge) }
            item { ProfileLink("WhatsApp: 11 93367-3435") { native.openLink("https://wa.me/5511933673435") } }
            item { ProfileLink("E-mail de suporte") { native.openLink("mailto:kaiqui.lucaskaiquiluc@gmail.com") } }
            items(topics) { (title, body) ->
                var expanded by remember(title) { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(title) }
                    if (expanded) Text(body)
                } }
            }
        }
    }
}
