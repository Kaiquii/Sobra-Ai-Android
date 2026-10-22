package com.example.appfinanceiro.feature.perfil

import br.com.sobraai.ui.runtime.UiServices as Context
import br.com.sobraai.ui.runtime.UiToast as Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.FinanceActionsViewModel
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.designsystem.components.AppDataErrorBanner
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.components.ExitConfirmationDialog
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.dataRequestErrorMessage
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onLogoutClick: () -> Unit = {},
    onNavigate: (Int) -> Unit = {},
    onAddClick: () -> Unit = {},
    onIncomeSettingsClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    actionsViewModel: FinanceActionsViewModel = rememberPresentation("FinanceActionsViewModel") { services -> FinanceActionsViewModel(services.finance, services.files) }
) {
    val context = LocalUiServices.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    val sessionManager = remember { context.session }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val userName by sessionManager.userName.collectAsState(initial = "")
    val userEmail by sessionManager.userEmail.collectAsState(initial = "")
    val userRole by sessionManager.userRole.collectAsState(initial = "")
    val userAvatarUrl by sessionManager.userAvatarUrl.collectAsState(initial = "")
    val userAvatarCacheVersion by sessionManager.userAvatarCacheVersion.collectAsState(initial = null)

    val isAdmin = userRole.equals("admin", ignoreCase = true)
    val isPremium = userRole.equals("premium", ignoreCase = true)
    val roleLabel = when (userRole.lowercase()) {
        "admin" -> "Administrador"
        "premium" -> "Premium"
        "user" -> "Usuário"
        else -> userRole.ifBlank { "Usuário" }
    }
    val adminRoleColor = Color(0xFFFF9800)
    val premiumRoleColor = Color(0xFF8B5CF6)

    var showExitDialog by remember { mutableStateOf(false) }
    var showRemovePhotoDialog by remember { mutableStateOf(false) }
    var showPhotoPreviewDialog by remember { mutableStateOf(false) }
    var isPhotoLoading by remember { mutableStateOf(false) }
    var isProfileLoading by remember { mutableStateOf(false) }
    var hasLoadedProfile by remember { mutableStateOf(false) }
    var profileLoadError by remember { mutableStateOf<String?>(null) }
    var profileRefreshTrigger by remember { mutableStateOf(0) }
    var localAvatarPreviewUri by remember { mutableStateOf<ByteArray?>(null) }
    val fullAvatarUrl = remember(userAvatarUrl, userAvatarCacheVersion) {
        userAvatarUrl.takeIf { it.isNotBlank() }?.let { reference ->
            if (userAvatarCacheVersion == null) reference else
                "$reference${if ('?' in reference) "&" else "?"}v=$userAvatarCacheVersion"
        }
    }
    val avatarImageModel = localAvatarPreviewUri ?: fullAvatarUrl

    LaunchedEffect(userToken, profileRefreshTrigger) {
        val token = userToken ?: return@LaunchedEffect

        isProfileLoading = true
        try {
            val profile = actionsViewModel.getProfile(token).user
            sessionManager.saveToken(
                token = token,
                name = profile.name,
                email = profile.email,
                role = profile.role,
                avatarUrl = profile.avatar_url
            )
            profileLoadError = null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e

            profileLoadError = e.userMessageOr("Não foi possível carregar o perfil.")
        } finally {
            isProfileLoading = false
            hasLoadedProfile = true
        }
    }

    fun uploadProfilePhoto(photo: com.example.appfinanceiro.core.network.ProfilePhoto) {
        val token = userToken
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Sessão inválida. Faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        isPhotoLoading = true
        coroutineScope.launch {
            try {
                localAvatarPreviewUri = photo.bytes
                val response = actionsViewModel.updateProfilePhoto(
                    token = token,
                    photo = photo
                )
                sessionManager.saveAvatarUrl(
                    avatarUrl = response.avatar_url,
                    cacheVersion = kotlin.time.Clock.System.now().toEpochMilliseconds()
                )
                Toast.makeText(context, "Foto atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                localAvatarPreviewUri = null

                Toast.makeText(
                    context,
                    e.userMessageOr("Não foi possível atualizar a foto."),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isPhotoLoading = false
            }
        }
    }

    fun removeProfilePhoto() {
        val token = userToken
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Sessão inválida. Faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        isPhotoLoading = true
        coroutineScope.launch {
            try {
                actionsViewModel.deleteProfilePhoto(token)
                localAvatarPreviewUri = null
                sessionManager.saveAvatarUrl(null)
                Toast.makeText(context, "Foto removida com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                Toast.makeText(
                    context,
                    e.userMessageOr("Não foi possível remover a foto."),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isPhotoLoading = false
            }
        }
    }

    fun pickPhoto() {
        if (isPhotoLoading) return
        coroutineScope.launch {
            try { context.native.pickPhoto()?.let { uploadProfilePhoto(it) } }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) { context.notify(error.userMessageOr("Não foi possível selecionar a foto.")) }
        }
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                showExitDialog = false
                onLogoutClick()
            },
            onDismiss = {
                showExitDialog = false
            }
        )
    }

    if (showRemovePhotoDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isPhotoLoading) {
                    showRemovePhotoDialog = false
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Remover foto",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tem certeza que deseja remover sua foto de perfil?",
                    color = textColor
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isPhotoLoading,
                    onClick = {
                        showRemovePhotoDialog = false
                        removeProfilePhoto()
                    }
                ) {
                    Text("Remover", color = DangerRed, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isPhotoLoading,
                    onClick = { showRemovePhotoDialog = false }
                ) {
                    Text("Cancelar", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    if (showPhotoPreviewDialog && avatarImageModel != null) {
        Dialog(
            onDismissRequest = { showPhotoPreviewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 480.dp),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Foto de perfil",
                                color = textColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userName.ifBlank { "SobraAi" },
                                color = TextMuted,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        IconButton(onClick = { showPhotoPreviewDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = textColor
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatarImage(
                            model = avatarImageModel,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.swipeNavigation(3, onNavigate),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Meu perfil", color = textColor, fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                actions = {
                    if (isProfileLoading && hasLoadedProfile) {
                        AppLoadingIndicator(modifier = Modifier.padding(end = 4.dp))
                    }
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sair da conta",
                            tint = DangerRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        bottomBar = {
            StandardBottomBar(
                itemSelecionado = 3,
                onItemClick = onNavigate,
                onAddClick = onAddClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            profileLoadError?.let { error ->
                AppDataErrorBanner(
                    message = dataRequestErrorMessage(
                        errorMessage = error,
                        showingPreviousData = userName.isNotBlank() || userEmail.isNotBlank(),
                        dataLabel = "o perfil"
                    ),
                    isRetrying = isProfileLoading,
                    onRetry = { profileRefreshTrigger++ }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            ProfileHeader(
                userName = userName,
                userEmail = userEmail,
                roleLabel = roleLabel,
                isAdmin = isAdmin,
                isPremium = isPremium,
                adminRoleColor = adminRoleColor,
                premiumRoleColor = premiumRoleColor,
                avatarImageModel = avatarImageModel,
                isPhotoLoading = isPhotoLoading,
                onAvatarClick = {
                    if (avatarImageModel != null) showPhotoPreviewDialog = true
                    else pickPhoto()
                },
                onChangePhotoClick = { pickPhoto() },
                onRemovePhotoClick = { showRemovePhotoDialog = true },
                onEditProfileClick = onEditProfileClick,
                avatarContent = { modifier -> if (avatarImageModel != null) ProfileAvatarImage(avatarImageModel, modifier = modifier) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("Finanças")
            SettingsItem(
                icon = Icons.Default.AccountBalance,
                iconColor = Color(0xFF2E9D57),
                title = "Rendas",
                subtitle = "Salário, adiantamento e renda extra",
                onClick = onIncomeSettingsClick
            )

            SettingsItem(
                icon = Icons.Default.Category,
                iconColor = Color(0xFF7E57C2),
                title = "Categorias",
                subtitle = "Organize os tipos de despesa",
                onClick = onCategoriesClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Suporte")
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                iconColor = Color(0xFFE29732),
                title = "Ajuda e suporte",
                subtitle = "Dúvidas frequentes e contato",
                onClick = onHelpClick
            )

            Spacer(
                modifier = Modifier.height(
                    paddingValues.calculateBottomPadding() + 12.dp
                )
            )
        }
    }
}

@Composable
private fun ProfileAvatarImage(model: Any, contentScale: ContentScale = ContentScale.Crop, modifier: Modifier = Modifier) {
    val services = LocalUiServices.current
    val token by services.session.token.collectAsState(initial = null)
    var bitmap by remember(model) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(model, token) {
        try {
            val bytes = if (model is ByteArray) model else services.files.avatar(token ?: return@LaunchedEffect, model.toString())
            bitmap = br.com.sobraai.ui.decodePhoto(bytes)
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { bitmap = null }
    }
    bitmap?.let { androidx.compose.foundation.Image(it, "Foto de perfil", modifier, contentScale = contentScale) }
}
