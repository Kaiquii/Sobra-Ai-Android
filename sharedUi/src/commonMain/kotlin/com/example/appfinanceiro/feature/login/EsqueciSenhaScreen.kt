package com.example.appfinanceiro.feature.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.runtime.AuthProtectionException
import br.com.sobraai.ui.runtime.LocalUiServices
import br.com.sobraai.ui.runtime.UiToast as Toast
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.ApiResponseException as ApiRequestException
import com.example.appfinanceiro.core.data.AuthViewModel
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.feature.login.components.*
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
    val isResetStep = step == ForgotPasswordStep.ResetPassword

    fun clearError() {
        errorMessage = ""
    }

    fun requestCode() {
        if (isLoading) return
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
        if (isLoading) return
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

    val focusManager = LocalFocusManager.current
    AuthPage(onBack = onNavigateBack, enabled = !isLoading) {
        AuthHeader(
            icon = null,
            iconDescription = "",
            title = if (isResetStep) "Nova senha" else "Recuperar senha",
            subtitle = if (!isResetStep) "Informe seu e-mail cadastrado"
                else if (context.codeDeliveryAvailable) "Digite o código enviado para seu e-mail"
                else "Digite um código válido já recebido",
            compact = true, textAlign = TextAlign.Start)
        Spacer(Modifier.height(if (isResetStep) 24.dp else 32.dp))
        AuthTextField("E-mail", email,
            { email = it.replace(" ", "").replace("\n", ""); clearError() },
            "seu@email.com", !isLoading, KeyboardType.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                imeAction = if (isResetStep) ImeAction.Next else ImeAction.Done),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus(); if (!isResetStep) requestCode() }))
        if (isResetStep) {
            Spacer(Modifier.height(16.dp))
            AuthTextField("Código", code,
                { code = it.filter(Char::isDigit).take(6); clearError() },
                "123456", !isLoading, KeyboardType.Number,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
            Spacer(Modifier.height(16.dp))
            AuthPasswordField("Nova senha", newPassword,
                { newPassword = it.replace("\n", ""); clearError() }, "Crie a nova senha", !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
            Spacer(Modifier.height(16.dp))
            AuthPasswordField("Confirmar nova senha", confirmPassword,
                { confirmPassword = it.replace("\n", ""); clearError() }, "Confirme a nova senha", !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); resetPassword() }))
        }
        Spacer(Modifier.height(24.dp))
        if (errorMessage.isNotBlank()) {
            AuthErrorMessage(errorMessage)
            Spacer(Modifier.height(12.dp))
        }
        AuthPrimaryButton(
            text = if (isResetStep) "Atualizar senha" else if (context.codeDeliveryAvailable) "Enviar código"
                else "Usar código já recebido",
            isLoading = isLoading, enabled = true,
            onClick = { focusManager.clearFocus(); if (isResetStep) resetPassword() else requestCode() })
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onNavigateBack, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar para entrar", color = authSupportingColor())
        }
    }
}
