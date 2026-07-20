package com.example.appfinanceiro.feature.login

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinanceiro.core.data.ApiRequestException
import com.example.appfinanceiro.core.data.AuthViewModel
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextSecondary
import com.example.appfinanceiro.feature.login.components.AuthErrorMessage
import com.example.appfinanceiro.feature.login.components.AuthPasswordField
import com.example.appfinanceiro.feature.login.components.AuthPrimaryButton
import com.example.appfinanceiro.feature.login.components.AuthTextField
import kotlinx.coroutines.launch

private enum class RegisterStep {
    Details,
    Confirmation
}

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var step by remember { mutableStateOf(RegisterStep.Details) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resendBlocked by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isConfirmation = step == RegisterStep.Confirmation

    fun clearError() {
        errorMessage = ""
    }

    fun requestCode() {
        when {
            name.isBlank() -> errorMessage = "Informe seu nome."
            email.isBlank() -> errorMessage = "Informe seu e-mail."
            password.isBlank() -> errorMessage = "Crie uma senha."
            else -> coroutineScope.launch {
                isLoading = true
                clearError()

                try {
                    val response = authViewModel.requestRegisterCode(email.trim())
                    code = ""
                    step = RegisterStep.Confirmation
                    Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                } catch (e: ApiRequestException) {
                    errorMessage = when (e.statusCode) {
                        400 -> e.apiMessage ?: "Informe um e-mail válido."
                        409 -> "Este e-mail já possui uma conta. Faça login para continuar."
                        429 -> {
                            resendBlocked = true
                            "Muitas tentativas. Aguarde alguns minutos antes de solicitar outro código."
                        }
                        500 -> "Não foi possível enviar o código. Tente novamente."
                        else -> e.apiMessage ?: "Não foi possível enviar o código. Tente novamente."
                    }
                    Log.e("API_ERRO", "Falha ao solicitar código de cadastro", e)
                } catch (e: Exception) {
                    errorMessage = e.userMessageOr("Não foi possível enviar o código. Verifique sua conexão e tente novamente.")
                    Log.e("API_ERRO", "Falha ao solicitar código de cadastro", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun register() {
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
                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                onRegisterSuccess()
            } catch (e: ApiRequestException) {
                errorMessage = when (e.statusCode) {
                    400 -> e.apiMessage ?: "Confira os dados informados."
                    401 -> "Código inválido ou expirado. Solicite um novo código e tente novamente."
                    409 -> "Este e-mail já possui uma conta. Faça login para continuar."
                    500 -> "Não foi possível criar sua conta. Tente novamente."
                    else -> e.apiMessage ?: "Não foi possível criar sua conta. Tente novamente."
                }
                Log.e("API_ERRO", "Falha ao criar conta", e)
            } catch (e: Exception) {
                errorMessage = e.userMessageOr("Não foi possível criar sua conta. Verifique sua conexão e tente novamente.")
                Log.e("API_ERRO", "Falha ao criar conta", e)
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            IconButton(
                onClick = {
                    if (isConfirmation) {
                        step = RegisterStep.Details
                        clearError()
                        resendBlocked = false
                    } else {
                        onNavigateBack()
                    }
                },
                enabled = !isLoading,
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
            Spacer(modifier = Modifier.height(if (isConfirmation) 0.dp else 16.dp))

            RegisterHeader(isConfirmation = isConfirmation)

            Spacer(modifier = Modifier.height(if (isConfirmation) 20.dp else 32.dp))

            if (!isConfirmation) {
                AuthTextField(
                    label = "Nome",
                    value = name,
                    onValueChange = { name = it; clearError() },
                    placeholder = "Seu nome completo",
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                AuthPasswordField(
                    label = "Senha",
                    value = password,
                    onValueChange = { password = it.replace("\n", ""); clearError() },
                    placeholder = "Crie uma senha forte",
                    enabled = !isLoading
                )
            } else {
                AuthTextField(
                    label = "Código de confirmação",
                    value = code,
                    onValueChange = {
                        code = it.filter(Char::isDigit).take(6)
                        clearError()
                    },
                    placeholder = "000000",
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enviamos um código de 6 dígitos para ${email.trim()}.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage.isNotBlank()) {
                AuthErrorMessage(errorMessage)
                Spacer(modifier = Modifier.height(8.dp))
            }

            AuthPrimaryButton(
                text = if (isConfirmation) "Criar Conta" else "Enviar Código",
                isLoading = isLoading,
                enabled = true,
                onClick = {
                    if (isConfirmation) register() else requestCode()
                }
            )

            if (isConfirmation) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (resendBlocked) {
                        "Reenvio indisponível temporariamente"
                    } else {
                        "Reenviar código"
                    },
                    color = if (resendBlocked) TextSecondary else PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !isLoading && !resendBlocked) {
                        requestCode()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Usar outro e-mail",
                    color = TextSecondary,
                    modifier = Modifier.clickable(enabled = !isLoading) {
                        step = RegisterStep.Details
                        resendBlocked = false
                        clearError()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isConfirmation) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Já tem uma conta? ", color = TextSecondary)
                    Text(
                        text = "Entrar",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !isLoading) { onNavigateBack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisterHeader(isConfirmation: Boolean) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val icon = if (isConfirmation) Icons.Default.MarkEmailRead else Icons.Default.AccountBalanceWallet

    Box(
        modifier = Modifier
            .size(if (isConfirmation) 56.dp else 64.dp)
            .background(surfaceColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(if (isConfirmation) 28.dp else 32.dp)
        )
    }

    Spacer(modifier = Modifier.height(if (isConfirmation) 18.dp else 32.dp))

    Text(
        text = if (isConfirmation) "Confirme seu e-mail" else "Criar Conta",
        fontSize = if (isConfirmation) 26.sp else 28.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = if (isConfirmation) {
            "Digite o código que enviamos para você"
        } else {
            "Preencha seus dados para começar"
        },
        fontSize = 14.sp,
        color = TextSecondary
    )
}
