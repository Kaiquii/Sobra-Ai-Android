package com.example.appfinanceiro.feature.perfil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import br.com.sobraai.app.BuildConfig
import com.example.appfinanceiro.core.data.FinanceActionsViewModel
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.designsystem.components.ExitConfirmationDialog
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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
    actionsViewModel: FinanceActionsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    val sessionManager = remember { SessionManager(context) }
    val userToken by sessionManager.token.collectAsState(initial = null)
    val userName by sessionManager.userName.collectAsState(initial = "")
    val userEmail by sessionManager.userEmail.collectAsState(initial = "")
    val userRole by sessionManager.userRole.collectAsState(initial = "")
    val userAvatarUrl by sessionManager.userAvatarUrl.collectAsState(initial = "")
    val userAvatarCacheVersion by sessionManager.userAvatarCacheVersion.collectAsState(initial = null)

    val isAdmin = userRole.equals("admin", ignoreCase = true)
    val roleLabel = when (userRole.lowercase()) {
        "admin" -> "Administrador"
        "user" -> "Usuário"
        else -> userRole.ifBlank { "Usuário" }
    }
    val adminRoleColor = Color(0xFFFF9800)

    var showExitDialog by remember { mutableStateOf(false) }
    var showRemovePhotoDialog by remember { mutableStateOf(false) }
    var showPhotoPreviewDialog by remember { mutableStateOf(false) }
    var isPhotoLoading by remember { mutableStateOf(false) }
    var localAvatarPreviewUri by remember { mutableStateOf<Uri?>(null) }
    val fullAvatarUrl = remember(userAvatarUrl, userAvatarCacheVersion) {
        buildFullAvatarUrl(userAvatarUrl, userAvatarCacheVersion)
    }
    val avatarImageModel = localAvatarPreviewUri ?: fullAvatarUrl

    LaunchedEffect(userToken) {
        val token = userToken ?: return@LaunchedEffect

        try {
            val profile = actionsViewModel.getProfile(token).user
            sessionManager.saveToken(
                token = token,
                name = profile.name,
                email = profile.email,
                role = profile.role,
                avatarUrl = profile.avatar_url
            )
        } catch (e: Exception) {
            Log.e("PROFILE_ERRO", "Falha ao carregar perfil", e)
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        val token = userToken
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Sessão inválida. Faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        isPhotoLoading = true
        coroutineScope.launch {
            try {
                val preparedPhoto = withContext(Dispatchers.IO) {
                    prepareProfilePhoto(context, uri)
                }
                localAvatarPreviewUri = preparedPhoto.previewUri
                val photoPart = createPhotoPart(preparedPhoto)
                val response = actionsViewModel.updateProfilePhoto(
                    token = token,
                    photo = photoPart
                )
                sessionManager.saveAvatarUrl(
                    avatarUrl = response.avatar_url,
                    cacheVersion = System.currentTimeMillis()
                )
                Toast.makeText(context, "Foto atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                localAvatarPreviewUri = null
                Log.e("PROFILE_ERRO", "Falha ao atualizar foto", e)
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
                Log.e("PROFILE_ERRO", "Falha ao remover foto", e)
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadProfilePhoto(it) }
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
                                text = userName.ifBlank { "SobraAí" },
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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ProfileHeader(
                userName = userName,
                userEmail = userEmail,
                roleLabel = roleLabel,
                isAdmin = isAdmin,
                adminRoleColor = adminRoleColor,
                avatarImageModel = avatarImageModel,
                isPhotoLoading = isPhotoLoading,
                onAvatarClick = {
                    if (avatarImageModel != null) showPhotoPreviewDialog = true
                    else photoPickerLauncher.launch("image/*")
                },
                onChangePhotoClick = { photoPickerLauncher.launch("image/*") },
                onRemovePhotoClick = { showRemovePhotoDialog = true },
                onEditProfileClick = onEditProfileClick
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

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    userEmail: String,
    roleLabel: String,
    isAdmin: Boolean,
    adminRoleColor: Color,
    avatarImageModel: Any?,
    isPhotoLoading: Boolean,
    onAvatarClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val roleColor = if (isAdmin) adminRoleColor else PrimaryBlue
    val editProfileContentColor = if (isSystemInDarkTheme()) {
        Color(0xFF0D2B4D)
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(width = 92.dp, height = 96.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(PrimaryBlue.copy(alpha = 0.16f), CircleShape)
                        .clip(CircleShape)
                        .clickable(
                            enabled = !isPhotoLoading && avatarImageModel != null,
                            onClick = onAvatarClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        avatarImageModel != null -> ProfileAvatarImage(
                            model = avatarImageModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        userName.isNotEmpty() -> Text(
                            text = userName.first().uppercase(),
                            color = PrimaryBlue,
                            fontSize = 31.sp,
                            fontWeight = FontWeight.Bold
                        )
                        else -> Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    if (isPhotoLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        }
                    }
                }

                if (!isPhotoLoading) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarActionButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Alterar foto",
                            backgroundColor = PrimaryBlue,
                            onClick = onChangePhotoClick
                        )

                        if (avatarImageModel != null) {
                            AvatarActionButton(
                                icon = Icons.Default.Delete,
                                contentDescription = "Remover foto",
                                backgroundColor = DangerRed,
                                onClick = onRemovePhotoClick
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName.ifEmpty { "Carregando..." },
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = userEmail.ifEmpty { "E-mail não identificado" },
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .background(roleColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAdmin) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = roleLabel,
                        color = roleColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onEditProfileClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                contentColor = editProfileContentColor
            )
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Editar perfil", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    val cardBg = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(cardBg, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = "Acessar", tint = TextMuted)
    }
}

@Composable
private fun AvatarActionButton(
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .padding(3.dp)
            .background(backgroundColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ProfileAvatarImage(
    model: Any,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = "Foto de perfil",
        contentScale = contentScale,
        modifier = modifier
    )
}

private fun buildFullAvatarUrl(avatarUrl: String, cacheVersion: Long? = null): String? {
    val cleanAvatarUrl = avatarUrl.trim()
    if (cleanAvatarUrl.isBlank()) return null

    val fullUrl = if (cleanAvatarUrl.startsWith("http://") || cleanAvatarUrl.startsWith("https://")) {
        cleanAvatarUrl
    } else {
        "${BuildConfig.API_BASE_URL.removeSuffix("/")}/${cleanAvatarUrl.removePrefix("/")}"
    }

    return cacheVersion?.let { version ->
        val separator = if (fullUrl.contains("?")) "&" else "?"
        "$fullUrl${separator}v=$version"
    } ?: fullUrl
}

private class PreparedProfilePhoto(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val previewUri: Uri
)

private fun prepareProfilePhoto(context: Context, uri: Uri): PreparedProfilePhoto {
    val originalFileName = getFileName(context, uri)
    val jpegFileName = originalFileName
        .substringBeforeLast('.', missingDelimiterValue = "avatar")
        .ifBlank { "avatar" }
        .plus(".jpg")

    val originalBytes = context.contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes()
    } ?: throw IllegalArgumentException("Nao foi possivel ler a imagem selecionada.")

    val originalBitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
        ?: throw IllegalArgumentException("Nao foi possivel decodificar a imagem selecionada.")

    val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
        ExifInterface(input).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL

    val rotatedBitmap = rotateBitmapByExifOrientation(originalBitmap, orientation)
    val normalizedBytes = ByteArrayOutputStream().use { output ->
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        output.toByteArray()
    }

    if (rotatedBitmap !== originalBitmap) {
        originalBitmap.recycle()
    }
    rotatedBitmap.recycle()

    val previewFile = File(context.cacheDir, "profile-avatar-preview-${System.currentTimeMillis()}.jpg")
    previewFile.writeBytes(normalizedBytes)

    return PreparedProfilePhoto(
        bytes = normalizedBytes,
        fileName = jpegFileName,
        mimeType = "image/jpeg",
        previewUri = Uri.fromFile(previewFile)
    )
}

private fun rotateBitmapByExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()

    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.preScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.preScale(-1f, 1f)
        }
        else -> return bitmap
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun createPhotoPart(photo: PreparedProfilePhoto): MultipartBody.Part {
    val requestBody = photo.bytes.toRequestBody(photo.mimeType.toMediaTypeOrNull())

    return MultipartBody.Part.createFormData("photo", photo.fileName, requestBody)
}

private fun getFileName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)

    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) {
            return it.getString(nameIndex)
        }
    }

    return "avatar.jpg"
}
