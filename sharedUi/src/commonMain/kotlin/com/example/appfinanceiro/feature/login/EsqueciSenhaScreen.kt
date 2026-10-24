package com.example.appfinanceiro.feature.login

import br.com.sobraai.ui.runtime.UiToast as Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.ApiResponseException as ApiRequestException
import com.example.appfinanceiro.core.data.AuthViewModel
import com.example.appfinanceiro.core.data.userMessageOr
import br.com.sobraai.ui.runtime.AuthProtectionException
import com.example.appfinanceiro.feature.login.components.AuthErrorMessage
import com.example.appfinanceiro.feature.login.components.AuthHeader
import com.example.appfinanceiro.feature.login.components.AuthPasswordField
import com.example.appfinanceiro.feature.login.components.AuthPrimaryButton
import com.example.appfinanceiro.feature.login.components.AuthTextField
import kotlinx.coroutines.launch

private enum class ForgotPasswordStep {
    Email,
    ResetPassword
}

@Composable
fun EsqueciSenhaScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = rememberPresentation("AuthViewModel") { services -> AuthViewModel(services.auth) }
) {
    var step by remember { mutableStateOf(ForgotPasswordStep.Email) }

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalUiServices.current
    val scrollState = rememberScrollState()
    val isResetStep = step == ForgotPasswordStep.ResetPassword
    val headerTopSpacing = if (isResetStep) 0.dp else 16.dp
    val sectionSpacing = if (isResetStep) 16.dp else 32.dp
    val fieldSpacing = if (isResetStep) 8.dp else 16.dp
    val buttonTopSpacing = if (isResetStep) 16.dp else 24.dp
    val bottomSpacing = if (isResetStep) 40.dp else 24.dp

    fun clearError() {
        errorMessage = ""
    }

    fun requestCode() {
        if (email.isBlank()) {
            errorMessage = "Informe o e-mail cadastrado."
            return
        }
        if (!context.codeDeliveryAvailable) {
            clearError()
            step = ForgotPasswordStep.ResetPassword
            return
        }

        coroutineScope.launch {
            isLoading = true
            clearError()

                try {
                val response = authViewModel.forgotPassword(email)

                Toast.success(context, response.message, Toast.LENGTH_LONG).show()
                step = ForgotPasswordStep.ResetPassword
            } catch (e: AuthProtectionException) {
                errorMessage = "Não foi possível validar a segurança do aplicativo. ${context.installationHint}"

            } catch (e: ApiRequestException) {
                errorMessage = when (e.statusCode) {
                    403 -> "Não foi possível validar a segurança do aplicativo. ${context.installationHint}"
                    429 -> "Muitas tentativas. Aguarde alguns minutos antes de solicitar outro código."
                    503 -> "A validação de segurança está temporariamente indisponível. Tente novamente."
                    else -> e.apiMessage ?: "Não foi possível solicitar o código. Tente novamente."
                }

            } catch (e: Exception) {

                if (e is kotlinx.coroutines.CancellationException) throw e
                errorMessage = e.userMessageOr("Não foi possível solicitar o código. Tente novamente.")

            } finally {
                isLoading = false
            }
        }
    }

    fun resetPassword() {
        when {
            email.isBlank() -> {
                errorMessage = "Informe o e-mail cadastrado."
            }

            code.length != 6 -> {
                errorMessage = "O código deve ter 6 dígitos."
            }

            newPassword.isBlank() -> {
                errorMessage = "Informe a nova senha."
            }

            confirmPassword != newPassword -> {
                errorMessage = "A confirmação de senha não confere."
            }

            else -> {
                coroutineScope.launch {
                    isLoading = true
                    clearError()

                    try {
                        val response = authViewModel.resetPassword(
                            email = email.trim(),
                            code = code,
                            newPassword = newPassword
                        )

                        Toast.success(context, response.message, Toast.LENGTH_LONG).show()
                        onNavigateBack()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        errorMessage = e.userMessageOr("Código inválido ou expirado.")

                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(headerTopSpacing))

            AuthHeader(
                icon = Icons.Default.LockReset,
                iconDescription = "Recuperar",
                title = if (!isResetStep) "Recuperar Senha" else "Nova Senha",
                subtitle = if (!isResetStep) {
                    "Informe seu e-mail cadastrado"
                } else {
                    if (context.codeDeliveryAvailable) "Digite o código enviado para seu e-mail" else "Digite um código válido já recebido"
                },
                compact = isResetStep
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            AuthTextField(
                label = "E-mail",
                value = email,
                onValueChange = {
                    email = it.replace(" ", "").replace("\n", "")
                    clearError()
                },
                placeholder = "seu@email.com",
                enabled = !isLoading,
                keyboardType = KeyboardType.Email
            )

            if (isResetStep) {
                Spacer(modifier = Modifier.height(fieldSpacing))

                AuthTextField(
                    label = "Código",
                    value = code,
                    onValueChange = {
                        code = it.filter { char -> char.isDigit() }.take(6)
                        clearError()
                    },
                    placeholder = "123456",
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(fieldSpacing))

                AuthPasswordField(
                    label = "Nova Senha",
                    value = newPassword,
                    onValueChange = {
                        newPassword = it.replace("\n", "")
                        clearError()
                    },
                    placeholder = "Crie a nova senha",
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(fieldSpacing))

                AuthPasswordField(
                    label = "Confirmar Nova Senha",
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it.replace("\n", "")
                        clearError()
                    },
                    placeholder = "Confirme a nova senha",
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(buttonTopSpacing))

            if (errorMessage.isNotBlank()) {
                AuthErrorMessage(errorMessage)
                Spacer(modifier = Modifier.height(8.dp))
            }

            AuthPrimaryButton(
                text = if (step == ForgotPasswordStep.Email) {
                    if (context.codeDeliveryAvailable) "Enviar Código" else "Usar código já recebido"
                } else "Atualizar Senha",
                isLoading = isLoading,
                enabled = true,
                onClick = {
                    if (step == ForgotPasswordStep.Email) {
                        requestCode()
                    } else {
                        resetPassword()
                    }
                }
            )

            Spacer(modifier = Modifier.height(bottomSpacing))
        }
    }
}
