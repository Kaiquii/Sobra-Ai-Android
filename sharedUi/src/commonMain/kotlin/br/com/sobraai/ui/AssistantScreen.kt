package br.com.sobraai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable
internal fun AssistantScreen(workspace: FinanceWorkspace, presenter: AssistantPresenter, hidden: Boolean, toggle: () -> Unit) {
    val state by presenter.state.collectAsState()
    val deletion by presenter.deletion.state.collectAsState()
    var history by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Int?>(null) }
    var time by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    val list = rememberLazyListState()
    val seconds = ((state.retryAt - time).coerceAtLeast(0) + 999) / 1000
    LaunchedEffect(state.retryAt) { do { time = Clock.System.now().toEpochMilliseconds(); delay(1000) } while (time < state.retryAt) }
    LaunchedEffect(state.messages.size) { if (!hidden && state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.lastIndex) }
    DisposableEffect(presenter) { onDispose { presenter.stopVoice() } }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader("Assistente IA", { workspace.navigate(FinanceRoute.Home) }) {
            PrivacyIcon(hidden, toggle)
            IconButton(onClick = { history = true; presenter.history() }, enabled = !state.sending && !hidden) { Icon(Icons.Default.History, "Histórico de conversas") }
            IconButton(onClick = { presenter.open(null) }, enabled = !state.sending && !hidden) { Icon(Icons.Default.Add, "Nova conversa") }
        } }, bottomBar = { FinanceBottomBar(workspace, FinanceRoute.Assistant) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp)) {
            if (hidden) Text("Conversa oculta. Mostre os valores para consultar ou enviar mensagens.", Modifier.padding(24.dp))
            else {
                if (state.loading) AppLoadingIndicator()
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error)
                    if (!state.loading && state.conversation != null) TextButton(onClick = { presenter.open(state.conversation) }, enabled = !state.sending) { Text("Recarregar conversa") }
                }
                if (state.messages.isEmpty() && !state.loading) Text("Pergunte sobre suas finanças ou descreva uma despesa. Confira os dados antes de confirmar uma criação.", Modifier.padding(vertical = 16.dp))
                LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    itemsIndexed(state.messages) { _, message ->
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (message.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (message.role == "user") "Você" else "SobraAi", fontWeight = FontWeight.Bold)
                                Text(message.content)
                                message.displayTime?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                    if (state.sending) item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AppLoadingIndicator(); Text("O assistente está respondendo...") } }
                }
                if (seconds > 0) Text("Limite temporário. Você pode enviar novamente em ${seconds}s.")
                if (state.listening) Text("Ouvindo... Confira a transcrição e toque em Enviar quando terminar.")
                OutlinedTextField(state.input, presenter::input, modifier = Modifier.fillMaxWidth(), enabled = !state.sending && !state.loading,
                    label = { Text("Mensagem") }, minLines = 1, maxLines = 5)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (state.listening) presenter.stopVoice() else presenter.voice() }, enabled = !state.sending && !state.loading) {
                        Icon(if (state.listening) Icons.Default.Stop else Icons.Default.Mic, if (state.listening) "Parar gravação" else "Preencher mensagem por voz")
                    }
                    TextButton(onClick = presenter::send, enabled = !state.sending && !state.loading && state.input.isNotBlank() && seconds == 0L) { Text("Enviar") }
                }
            }
        }
    }
    if (history) AlertDialog(onDismissRequest = { history = false }, containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Conversas") }, text = {
            Column(Modifier.heightIn(max = 460.dp)) {
                if (state.historyLoading) AppLoadingIndicator()
                state.historyError?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = presenter::history) { Text("Tentar novamente") } }
                FeatureActionFeedback(deletion)
                if (state.conversations.isEmpty() && !state.historyLoading) Text("Nenhuma conversa salva.")
                LazyColumn { items(state.conversations, key = { it.conversation_id }) { conversation ->
                    Column {
                        TextButton(onClick = { presenter.open(conversation.conversation_id); history = false }, enabled = !state.sending) {
                            Text(conversation.title.ifBlank { "Conversa" })
                        }
                        conversation.display_label?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        TextButton(onClick = { deleting = conversation.conversation_id }, enabled = !deletion.busy && !state.sending) { Text("Excluir conversa", color = MaterialTheme.colorScheme.error) }
                    }
                } }
            }
        }, confirmButton = { TextButton(onClick = { history = false }) { Text("Fechar") } })
    deleting?.let { id -> ConfirmDialog("Excluir conversa?", "O histórico desta conversa será removido.", "Excluir", { deleting = null; presenter.delete(id) }, { deleting = null }) }
}
