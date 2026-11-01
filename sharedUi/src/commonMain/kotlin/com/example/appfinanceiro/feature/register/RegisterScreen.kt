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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue

private enum class RegisterStep {
    Details,
    Confirmation
}

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    authViewModel: AuthViewModel = rememberPresentation("AuthViewModel") { services -> AuthViewModel(services.auth) }
) {
    var step by remember { mutableStateOf(RegisterStep.Details) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resendBlocked by remember { mutableStateOf(false) }
    var codeSent by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalUiServices.current
    val isConfirmation = step == RegisterStep.Confirmation

    fun clearError() {
        errorMessage = ""
    }

    fun requestCode() {
        if (isLoading) return
        when {
            name.isBlank() -> errorMessage = "Informe seu nome."
            email.isBlank() -> errorMessage = "Informe seu e-mail."
            password.isBlank() -> errorMessage = "Crie uma senha."
            !context.codeDeliveryAvailable -> {
                clearError()
                step = RegisterStep.Confirmation
            }
            else -> coroutineScope.launch {
                isLoading = true
                clearError()

                try {
                    val response = authViewModel.requestRegisterCode(email)
                    code = ""
                    codeSent = true
                    step = RegisterStep.Confirmation
                    Toast.success(context, response.message, Toast.LENGTH_LONG).show()
                } catch (e: AuthProtectionException) {
                    errorMessage = "Não foi possível validar a segurança do aplicativo. ${context.installationHint}"

                } catch (e: ApiRequestException) {
                    errorMessage = when (e.statusCode) {
                        400 -> e.apiMessage ?: "Informe um e-mail válido."
                        403 -> "Não foi possível validar a segurança do aplicativo. ${context.installationHint}"
                        409 -> "Este e-mail já possui uma conta. Faça login para continuar."
                        429 -> {
                            resendBlocked = true
                            "Muitas tentativas. Aguarde alguns minutos antes de solicitar outro código."
                        }
                        500 -> "Não foi possível enviar o código. Tente novamente."
                        503 -> "A validação de segurança está temporariamente indisponível. Tente novamente."
                        else -> e.apiMessage ?: "Não foi possível enviar o código. Tente novamente."
                    }

                } catch (e: Exception) {

                    if (e is kotlinx.coroutines.CancellationException) throw e
                    errorMessage = e.userMessageOr("Não foi possível enviar o código. Verifique sua conexão e tente novamente.")

                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun register() {
        if (isLoading) return
        if (code.length != 6) {
            errorMessage = "O código deve ter 6 dígitos."
            return
        }

        coroutineScope.launch {
            isLoading = true
            clearError()

            try {
                val response = authViewModel.register(
                    name = name.trim(),
                    email = email.trim(),
                    password = password,
                    code = code
                )
                Toast.success(context, response.message, Toast.LENGTH_LONG).show()
                onRegisterSuccess()
            } catch (e: ApiRequestException) {
                errorMessage = when (e.statusCode) {
                    400 -> e.apiMessage ?: "Confira os dados informados."
                    401 -> "Código inválido ou expirado. Solicite um novo código e tente novamente."
                    409 -> "Este e-mail já possui uma conta. Faça login para continuar."
                    500 -> "Não foi possível criar sua conta. Tente novamente."
                    else -> e.apiMessage ?: "Não foi possível criar sua conta. Tente novamente."
                }

            } catch (e: Exception) {

                if (e is kotlinx.coroutines.CancellationException) throw e
                errorMessage = e.userMessageOr("Não foi possível criar sua conta. Verifique sua conexão e tente novamente.")

            } finally {
                isLoading = false
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val goBack = {
        if (isConfirmation) {
            step = RegisterStep.Details
            clearError()
            resendBlocked = false
        } else onNavigateBack()
    }
    AuthPage(onBack = goBack, enabled = !isLoading, footer = if (!isConfirmation) ({
        TextButton(onClick = onNavigateBack, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = authSupportingColor())) { append("Já tem uma conta? ") }
                withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) { append("Entrar") }
            }, textAlign = TextAlign.Center)
        }
    }) else null) {
        AuthHeader(
            icon = null,
            iconDescription = "",
            title = if (isConfirmation) "Confirme seu e-mail" else "Crie sua conta",
            subtitle = if (isConfirmation) {
                if (codeSent) "Digite o código de 6 dígitos que enviamos para ${email.trim()}."
                else "Informe o código de 6 dígitos já recebido para ${email.trim()}."
            } else "Comece a organizar seu dinheiro de forma simples.",
            compact = isConfirmation, textAlign = TextAlign.Start)
        Spacer(Modifier.height(28.dp))
        if (!isConfirmation) {
            AuthTextField("Nome", name, { name = it; clearError() }, "Seu nome completo", !isLoading,
                KeyboardType.Text,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
            Spacer(Modifier.height(20.dp))
            AuthTextField("E-mail", email,
                { email = it.replace(" ", "").replace("\n", ""); clearError() }, "seu@email.com", !isLoading,
                KeyboardType.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
            Spacer(Modifier.height(20.dp))
            AuthPasswordField("Senha", password, { password = it.replace("\n", ""); clearError() },
                "Crie uma senha forte", !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); requestCode() }))
        } else {
            AuthCodeField("Código de confirmação", code,
                { code = it.filter(Char::isDigit).take(6); clearError() }, !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); register() }))
        }
        Spacer(Modifier.height(24.dp))
        if (errorMessage.isNotBlank()) {
            AuthErrorMessage(errorMessage)
            Spacer(Modifier.height(12.dp))
        }
        AuthPrimaryButton(
            text = if (isConfirmation) "Criar conta" else if (context.codeDeliveryAvailable) "Enviar código" else "Usar código já recebido",
            isLoading = isLoading, enabled = true,
            onClick = { focusManager.clearFocus(); if (isConfirmation) register() else requestCode() })
        if (isConfirmation) {
            Spacer(Modifier.height(12.dp))
            TextButton(enabled = !isLoading && !resendBlocked && context.codeDeliveryAvailable,
                onClick = { focusManager.clearFocus(); requestCode() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (!context.codeDeliveryAvailable) "Envio de códigos indisponível no momento"
                    else if (resendBlocked) "Reenvio indisponível temporariamente" else "Reenviar código",
                    textAlign = TextAlign.Center)
            }
            TextButton(enabled = !isLoading, onClick = goBack, modifier = Modifier.fillMaxWidth()) {
                Text("Usar outro e-mail", color = authSupportingColor())
            }
        }
    }
}
