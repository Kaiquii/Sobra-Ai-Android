package com.example.appfinanceiro.feature.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sobraai.ui.AppRoute
import br.com.sobraai.ui.BiometricKind
import br.com.sobraai.ui.biometricIcon
import br.com.sobraai.ui.runtime.LocalUiServices
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.feature.login.components.*
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit
) {
    val controller = LocalUiServices.current.controller
    val state by controller.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    val errorMessage = state.message.orEmpty()
    val isLoading = state.busy
    var now by remember { mutableStateOf(kotlin.time.Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(state.blockedUntil) {
        do { now = kotlin.time.Clock.System.now().toEpochMilliseconds(); delay(1000) } while (now < state.blockedUntil)
    }
    val isLoginTemporarilyBlocked = email.trim().lowercase() == state.blockedEmail && now < state.blockedUntil
    val remainingBlockSeconds = ((state.blockedUntil - now).coerceAtLeast(0) + 999) / 1000
    val focusManager = LocalFocusManager.current
    fun clearError() = controller.clearLoginMessage()
    fun login() { focusManager.clearFocus(); controller.login(email, senha) }
    AuthPage(footer = {
        TextButton(onClick = onNavigateToRegister, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = authSupportingColor())) { append("Ainda não tem conta? ") }
                withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) { append("Criar conta") }
            }, textAlign = TextAlign.Center)
        }
    }) {
        AuthHeader(null, "", "Bem-vindo de volta",
            "Entre para continuar cuidando das suas finanças.", textAlign = TextAlign.Start)
        Spacer(Modifier.height(32.dp))
        AuthTextField(
            label = "E-mail", value = email,
            onValueChange = { email = it.replace(" ", "").replace("\n", ""); clearError() },
            placeholder = "seu@email.com", enabled = !isLoading, keyboardType = KeyboardType.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            leadingIcon = Icons.Default.Email)
        Spacer(Modifier.height(20.dp))
        AuthPasswordField(
            label = "Senha", value = senha,
            onValueChange = { senha = it.replace(" ", "").replace("\n", ""); clearError() },
            placeholder = "Digite a sua senha", enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (!isLoading && !isLoginTemporarilyBlocked) login() }),
            leadingIcon = Icons.Default.Lock)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onNavigateToForgot, enabled = !isLoading) {
                Text("Esqueci minha senha", color = PrimaryBlue)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (errorMessage.isNotBlank()) {
            AuthErrorMessage(errorMessage)
            Spacer(Modifier.height(12.dp))
        }
        if (isLoginTemporarilyBlocked) {
            Text("Bloqueio temporário para este e-mail. Você pode corrigir os dados, mas aguarde para tentar novamente.",
                color = authSupportingColor(), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
        }
        AuthPrimaryButton(
            text = if (isLoginTemporarilyBlocked) {
                "Tente novamente em ${remainingBlockSeconds / 60}:${(remainingBlockSeconds % 60).toString().padStart(2, '0')}"
            } else "Entrar",
            isLoading = isLoading, enabled = !isLoginTemporarilyBlocked, onClick = ::login)
        if (state.route == AppRoute.Locked) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = { focusManager.clearFocus(); controller.unlock() }, enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
                Icon(biometricIcon(controller.biometricKind), null, tint = PrimaryBlue)
                Spacer(Modifier.width(10.dp))
                Text(if (controller.biometricKind == BiometricKind.Generic) "Entrar com biometria"
                    else "Entrar com ${controller.biometricKind.label}",
                    color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            }
        }
    }
}
