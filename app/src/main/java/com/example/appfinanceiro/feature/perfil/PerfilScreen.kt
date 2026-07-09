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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import br.com.sobraai.app.BuildConfig
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.components.ExitConfirmationDialog
import com.example.appfinanceiro.core.designsystem.components.StandardBottomBar
import com.example.appfinanceiro.core.designsystem.components.swipeNavigation
import com.example.appfinanceiro.core.designsystem.theme.DangerRed
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.auth.RetrofitClient
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
    onIncomeSettingsClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onHelpClick: () -> Unit = {}
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
            val profile = RetrofitClient.financeApi.getProfile("Bearer $token").user
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
                val response = RetrofitClient.financeApi.updateProfilePhoto(
                    token = "Bearer $token",
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
                Toast.makeText(context, "Não foi possível atualizar a foto.", Toast.LENGTH_SHORT).show()
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
                RetrofitClient.financeApi.deleteProfilePhoto("Bearer $token")
                localAvatarPreviewUri = null
                sessionManager.saveAvatarUrl(null)
                Toast.makeText(context, "Foto removida com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("PROFILE_ERRO", "Falha ao remover foto", e)
                Toast.makeText(context, "Não foi possível remover a foto.", Toast.LENGTH_SHORT).show()
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
        AlertDialog(
            onDismissRequest = { showPhotoPreviewDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp),
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimaryBlue.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatarImage(
                        model = avatarImageModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoPreviewDialog = false }) {
                    Text("Fechar", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.swipeNavigation(3, onNavigate),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Perfil", color = textColor, fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        bottomBar = {
            StandardBottomBar(
                itemSelecionado = 3,
                onItemClick = onNavigate,
                onAddClick = { /* Abre Nova Despesa */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.size(width = 128.dp, height = 122.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(PrimaryBlue.copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !isPhotoLoading) {
                            if (avatarImageModel != null) {
                                showPhotoPreviewDialog = true
                            } else {
                                photoPickerLauncher.launch("image/*")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarImageModel != null) {
                        ProfileAvatarImage(
                            model = avatarImageModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (userName.isNotEmpty()) {
                        Text(
                            text = userName.first().uppercase(),
                            color = PrimaryBlue,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(60.dp))
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
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarActionButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Alterar foto",
                            backgroundColor = PrimaryBlue,
                            onClick = { photoPickerLauncher.launch("image/*") }
                        )

                        if (avatarImageModel != null) {
                            AvatarActionButton(
                                icon = Icons.Default.Delete,
                                contentDescription = "Remover foto",
                                backgroundColor = DangerRed,
                                onClick = { showRemovePhotoDialog = true }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userName.ifEmpty { "Carregando..." },
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = userEmail.ifEmpty { "E-mail não identificado" },
                color = TextMuted,
                fontSize = 14.sp
            )

            if (isAdmin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = adminRoleColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = roleLabel,
                        color = adminRoleColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = roleLabel,
                    color = PrimaryBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onEditProfileClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
            ) {
                Text("Editar Perfil")
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionTitle("Configurações")
            SettingsItem(
                icon = Icons.Default.AccountBalance,
                iconColor = PrimaryBlue,
                title = "Configurações de Renda",
                subtitle = "Salário, Adiantamento e Renda Extra",
                onClick = onIncomeSettingsClick
            )

            SettingsItem(
                icon = Icons.Default.Category,
                iconColor = PrimaryBlue,
                title = "Categorias",
                subtitle = "Crie e edite suas categorias",
                onClick = onCategoriesClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("Ajuda")
            SettingsItem(
                icon = Icons.Default.HelpOutline,
                iconColor = PrimaryBlue,
                title = "Central de Ajuda",
                subtitle = "Dúvidas frequentes e Suporte",
                onClick = onHelpClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showExitDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = DangerRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da Conta", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
        }

        Spacer(modifier = Modifier.width(16.dp))

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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
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
        contentScale = ContentScale.Crop,
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
