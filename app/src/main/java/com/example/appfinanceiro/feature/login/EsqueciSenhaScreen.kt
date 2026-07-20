package com.example.appfinanceiro.feature.login

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinanceiro.core.data.ApiRequestException
import com.example.appfinanceiro.core.data.AuthViewModel
import com.example.appfinanceiro.core.data.userMessageOr
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
    authViewModel: AuthViewModel = viewModel()
) {
    var step by remember { mutableStateOf(ForgotPasswordStep.Email) }

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
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

        coroutineScope.launch {
            isLoading = true
            clearError()

            try {
                val response = authViewModel.forgotPassword(email.trim())

                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                step = ForgotPasswordStep.ResetPassword
            } catch (e: ApiRequestException) {
                errorMessage = if (e.statusCode == 429) {
                    "Muitas tentativas. Aguarde alguns minutos antes de solicitar outro código."
                } else {
                    e.apiMessage ?: "Não foi possível solicitar o código. Tente novamente."
                }
                android.util.Log.e("API_ERRO", "Falha ao solicitar codigo", e)
            } catch (e: Exception) {
                errorMessage = e.userMessageOr("Não foi possível solicitar o código. Tente novamente.")
                android.util.Log.e("API_ERRO", "Falha ao solicitar codigo", e)
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

                        Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                        onNavigateBack()
                    } catch (e: Exception) {
                        errorMessage = e.userMessageOr("Código inválido ou expirado.")
                        android.util.Log.e("API_ERRO", "Falha ao redefinir senha", e)
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
                    "Digite o código enviado para seu e-mail"
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
                text = if (step == ForgotPasswordStep.Email) "Enviar Código" else "Atualizar Senha",
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
