package com.example.appfinanceiro.feature.login

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusDirection
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sobraai.ui.AppRoute
import br.com.sobraai.ui.BiometricKind
import br.com.sobraai.ui.biometricIcon
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.ApiResponseException as ApiRequestException
import com.example.appfinanceiro.core.data.AuthViewModel
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextSecondary
import com.example.appfinanceiro.feature.login.components.AuthErrorMessage
import com.example.appfinanceiro.feature.login.components.AuthHeader
import com.example.appfinanceiro.feature.login.components.AuthPasswordField
import com.example.appfinanceiro.feature.login.components.AuthPrimaryButton
import com.example.appfinanceiro.feature.login.components.AuthTextField
import kotlinx.coroutines.launch
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        AuthHeader(
            icon = Icons.Default.AccountBalanceWallet,
            iconDescription = "Logo",
            title = "Bem-vindo de volta",
            subtitle = "Faça login na sua conta para continuar"
        )

        Spacer(modifier = Modifier.height(32.dp))

        AuthTextField(
            label = "E-mail",
            value = email,
            onValueChange = {
                email = it.replace(" ", "").replace("\n", "")
                clearError()
            },
            placeholder = "Digite o seu e-mail",
            enabled = !isLoading,
            keyboardType = KeyboardType.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthPasswordField(
            label = "Senha",
            value = senha,
            onValueChange = {
                senha = it.replace(" ", "").replace("\n", "")
                clearError()
            },
            placeholder = "Digite a sua senha",
            enabled = !isLoading
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            TextButton(
                onClick = onNavigateToForgot,
                enabled = !isLoading
            ) {
                Text("Esqueci minha senha", color = PrimaryBlue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotBlank()) {
            AuthErrorMessage(errorMessage)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoginTemporarilyBlocked) {
            Text(
                text = "Bloqueio temporário para este e-mail. Você pode corrigir os dados, mas aguarde para tentar novamente.",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        AuthPrimaryButton(
            text = if (isLoginTemporarilyBlocked) {
                "Tente novamente em ${"${remainingBlockSeconds / 60}:${(remainingBlockSeconds % 60).toString().padStart(2, '0')}"}"
            } else {
                "Entrar"
            },
            isLoading = isLoading,
            enabled = !isLoginTemporarilyBlocked,
            onClick = { login() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.route == AppRoute.Locked) {
            OutlinedButton(
                onClick = { focusManager.clearFocus(); controller.unlock() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(biometricIcon(controller.biometricKind), contentDescription = null, tint = PrimaryBlue)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (controller.biometricKind == BiometricKind.Generic) "Entrar com biometria"
                    else "Entrar com ${controller.biometricKind.label}",
                    color = PrimaryBlue
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Não tem uma conta? ", color = TextSecondary)

            Text(
                text = "Cadastre-se",
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !isLoading) {
                    onNavigateToRegister()
                }
            )
        }
    }

}
