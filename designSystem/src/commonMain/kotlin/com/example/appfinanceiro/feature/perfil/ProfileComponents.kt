package com.example.appfinanceiro.feature.perfil

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.appfinanceiro.core.designsystem.theme.*
@Composable
fun ProfileHeader(
    userName: String,
    userEmail: String,
    roleLabel: String,
    isAdmin: Boolean,
    isPremium: Boolean,
    adminRoleColor: Color,
    premiumRoleColor: Color,
    avatarImageModel: Any?,
    isPhotoLoading: Boolean,
    onAvatarClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    avatarContent: @Composable (Modifier) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val roleColor = when {
        isAdmin -> adminRoleColor
        isPremium -> premiumRoleColor
        else -> PrimaryBlue
    }
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
                        avatarImageModel != null -> avatarContent(Modifier.fillMaxSize())
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
                    if (isAdmin || isPremium) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.Star else Icons.Default.Diamond,
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
fun SectionTitle(title: String) {
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
fun SettingsItem(
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

