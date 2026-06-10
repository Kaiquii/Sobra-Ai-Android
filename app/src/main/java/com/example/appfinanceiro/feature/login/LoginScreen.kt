package com.example.appfinanceiro.feature.login

import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.appfinanceiro.core.biometric.BiometricAuth
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextSecondary
import com.example.appfinanceiro.core.network.auth.LoginRequest
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import com.example.appfinanceiro.core.network.parseApiErrorMessage
import com.example.appfinanceiro.feature.login.components.AuthErrorMessage
import com.example.appfinanceiro.feature.login.components.AuthHeader
import com.example.appfinanceiro.feature.login.components.AuthPasswordField
import com.example.appfinanceiro.feature.login.components.AuthPrimaryButton
import com.example.appfinanceiro.feature.login.components.AuthTextField
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val sessionManager = remember { SessionManager(context) }

    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var showBiometricOffer by remember { mutableStateOf(false) }
    var pendingToken by remember { mutableStateOf("") }
    var pendingUserName by remember { mutableStateOf("") }
    var pendingUserEmail by remember { mutableStateOf("") }
    var pendingUserRole by remember { mutableStateOf("") }
    var pendingUserAvatarUrl by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val savedToken by sessionManager.token.collectAsState(initial = null)
    val savedUserEmail by sessionManager.userEmail.collectAsState(initial = "")
    val biometricEnabledEmails by sessionManager.biometricEnabledEmails.collectAsState(initial = emptySet())

    val biometricEnabledForSavedUser =
        savedUserEmail.isNotBlank() &&
                biometricEnabledEmails.contains(savedUserEmail.trim().lowercase())

    val canUseBiometric =
        activity != null &&
                savedToken != null &&
                biometricEnabledForSavedUser &&
                BiometricAuth.isAvailable(activity)
    val biometricActivity = activity?.takeIf { canUseBiometric }

    fun clearError() {
        errorMessage = ""
    }

    fun finishLogin(
        token: String,
        name: String,
        userEmail: String,
        userRole: String,
        userAvatarUrl: String? = null,
        biometricEnabled: Boolean? = null
    ) {
        coroutineScope.launch {
            sessionManager.saveToken(
                token = token,
                name = name,
                email = userEmail,
                role = userRole,
                avatarUrl = userAvatarUrl
            )

            if (biometricEnabled != null) {
                sessionManager.setBiometricEnabledForUser(
                    email = userEmail,
                    enabled = biometricEnabled
                )
            }

            showBiometricOffer = false
            onLoginSuccess()
        }
    }

    fun login() {
        if (email.isBlank() || senha.isBlank()) {
            errorMessage = "Preencha e-mail e senha."
            return
        }

        coroutineScope.launch {
            isLoading = true
            clearError()

            try {
                val response = RetrofitClient.authApi.login(
                    LoginRequest(email.trim(), senha)
                )

                pendingToken = response.token
                pendingUserName = response.user.name
                pendingUserEmail = response.user.email
                pendingUserRole = response.user.role
                pendingUserAvatarUrl = response.user.avatar_url

                if (activity != null && BiometricAuth.isAvailable(activity)) {
                    showBiometricOffer = true
                } else {
                    finishLogin(
                        token = pendingToken,
                        name = pendingUserName,
                        userEmail = pendingUserEmail,
                        userRole = pendingUserRole,
                        userAvatarUrl = pendingUserAvatarUrl
                    )
                }
            } catch (e: HttpException) {
                val apiMessage = parseApiErrorMessage(e.response()?.errorBody()?.string())

                if (e.code() == 403 && !apiMessage.isNullOrBlank()) {
                    sessionManager.clearSession()
                    errorMessage = apiMessage
                } else {
                    errorMessage = apiMessage ?: "Credenciais invalidas ou erro no servidor."
                }

                android.util.Log.e("API_ERRO", "Erro no login: ${e.message}", e)
            } catch (e: Exception) {
                errorMessage = "Credenciais invalidas ou erro no servidor."
                android.util.Log.e("API_ERRO", "Erro no login: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        AuthHeader(
            icon = Icons.Default.AccountBalanceWallet,
            iconDescription = "Logo",
            title = "Bem-vindo de volta",
            subtitle = "Faca login na sua conta para continuar"
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

        AuthPrimaryButton(
            text = "Entrar",
            isLoading = isLoading,
            enabled = true,
            onClick = { login() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (biometricActivity != null) {
            OutlinedButton(
                onClick = {
                    BiometricAuth.showBiometricPrompt(
                        activity = biometricActivity,
                        onSuccess = onLoginSuccess,
                        onError = { errorMessage = "Nao foi possivel autenticar com biometria." }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Entrar com biometria")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nao tem uma conta? ", color = TextSecondary)

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

    if (showBiometricOffer) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = {
                Text(
                    text = "Ativar biometria?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Deseja usar biometria para deixar seu aplicativo mais seguro?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        finishLogin(
                            token = pendingToken,
                            name = pendingUserName,
                            userEmail = pendingUserEmail,
                            userRole = pendingUserRole,
                            userAvatarUrl = pendingUserAvatarUrl,
                            biometricEnabled = true
                        )
                    }
                ) {
                    Text(
                        text = "Ativar",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        finishLogin(
                            token = pendingToken,
                            name = pendingUserName,
                            userEmail = pendingUserEmail,
                            userRole = pendingUserRole,
                            userAvatarUrl = pendingUserAvatarUrl,
                            biometricEnabled = false
                        )
                    }
                ) {
                    Text(
                        text = "Agora nao",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
