package com.example.appfinanceiro.feature.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import br.com.sobraai.ui.runtime.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    onNavigateBack: () -> Unit,
    onSessionExpired: () -> Unit = {},
    viewModel: AssistantViewModel = rememberPresentation("AssistantViewModel") { services -> AssistantViewModel(services.finance) }
) {
    val context = LocalUiServices.current
    val sessionManager = remember { context.session }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    var showConversations by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = colorScheme.background
    val inputBgColor = colorScheme.surface
    val textColor = colorScheme.onBackground
    val canSend = input.isNotBlank() &&
            !uiState.isSending &&
            userToken != null &&
            (uiState.quotaRetrySeconds ?: 0) <= 0

    val voiceScope = androidx.compose.runtime.rememberCoroutineScope()
    var voiceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    fun toggleVoiceInput() {
        if (isListening) { voiceJob?.cancel(); context.native.cancelInteractions(); isListening = false; return }
        voiceError = null
        voiceJob = voiceScope.launch {
            isListening = true
            try { context.native.transcribe { input = it }?.let { input = it } }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) { voiceError = error.message ?: "Não foi possível transcrever a voz." }
            finally { isListening = false }
        }
    }
    DisposableEffect(context) { onDispose { voiceJob?.cancel(); context.native.cancelInteractions() } }

    LaunchedEffect(userToken) {
        userToken?.let { token ->
            viewModel.loadConversations(token)
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.isSending) {
        val itemCount = uiState.messages.size + if (uiState.isSending) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
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
                        text = "Assistente IA",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            tint = textColor,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startNewConversation() }) {
                        Icon(
                            Icons.Default.Add,
                            tint = textColor,
                            contentDescription = "Nova conversa"
                        )
                    }
                    IconButton(onClick = { showConversations = true }) {
                        Icon(
                            Icons.Default.History,
                            tint = textColor,
                            contentDescription = "Conversas"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        bottomBar = {
            AssistantInputBar(
                value = input,
                enabled = !uiState.isSending,
                canSend = canSend,
                quotaRetrySeconds = uiState.quotaRetrySeconds,
                isListening = isListening,
                voiceError = voiceError,
                onValueChange = {
                    input = it
                    voiceError = null
                    if (uiState.errorMessage != null) {
                        viewModel.clearError()
                    }
                },
                onVoiceClick = { toggleVoiceInput() },
                onSend = {
                    userToken?.let { token ->
                        val message = input
                        input = ""
                        viewModel.sendMessage(token, message)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoadingMessages) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            } else if (uiState.messages.isEmpty()) {
                item {
                    EmptyAssistantState()
                }
            }

            items(uiState.messages) { message ->
                ChatBubble(message)
            }

            if (uiState.isSending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            color = colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryBlue
                                )
                                Text("Pensando...", color = colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }

    if (showConversations) {
        ModalBottomSheet(
            onDismissRequest = { showConversations = false },
            containerColor = backgroundColor
        ) {
            ConversationsSheet(
                uiState = uiState,
                onNewConversation = {
                    viewModel.startNewConversation()
                    showConversations = false
                },
                onOpenConversation = { conversationId ->
                    userToken?.let { token ->
                        viewModel.openConversation(token, conversationId)
                    }
                    showConversations = false
                },
                onDeleteConversation = { conversationId ->
                    userToken?.let { token ->
                        viewModel.deleteConversation(token, conversationId)
                    }
                }
            )
        }
    }
}

@Composable
private fun ConversationsSheet(
    uiState: AssistantUiState,
    onNewConversation: () -> Unit,
    onOpenConversation: (Int) -> Unit,
    onDeleteConversation: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Conversas",
                color = colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNewConversation) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Nova conversa",
                    tint = PrimaryBlue
                )
            }
        }

        if (uiState.isLoadingConversations) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (uiState.conversations.isEmpty()) {
            Text(
                text = "Nenhuma conversa salva ainda.",
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            uiState.conversations.forEach { conversation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenConversation(conversation.conversation_id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conversation.title,
                            color = colorScheme.onSurface,
                            fontWeight = if (conversation.conversation_id == uiState.currentConversationId) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                        Text(
                            text = conversation.display_label
                                ?: listOfNotNull(
                                    conversation.display_date,
                                    conversation.display_time
                                ).joinToString(" "),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteConversation(conversation.conversation_id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Apagar conversa",
                            tint = colorScheme.error
                        )
                    }
                }
                HorizontalDivider(color = colorScheme.outlineVariant)
            }
        }
    }
}
