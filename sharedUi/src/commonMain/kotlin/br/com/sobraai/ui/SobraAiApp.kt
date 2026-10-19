package br.com.sobraai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import br.com.sobraai.ui.components.*
import br.com.sobraai.ui.theme.AppFinanceiroTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock

@Composable
fun SobraAiApp(controller: AppController, homeFactory: (String) -> HomePresenter,
    workspaceFactory: (String, HomePresenter) -> FinanceWorkspace,
    accountFactory: (AccountMode, CoroutineScope) -> AccountAuthPresenter) {
    val state by controller.state.collectAsState()
    var valuesHidden by rememberSaveable { mutableStateOf(false) }
    AppFinanceiroTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (state.route) {
                AppRoute.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppLoadingIndicator() }
                AppRoute.Login -> LoginScreen(state, controller::login,
                    { controller.accountRoute(AppRoute.Register) }, { controller.accountRoute(AppRoute.Recovery) })
                AppRoute.Register, AppRoute.Recovery -> {
                    val accountScope = rememberCoroutineScope()
                    val mode = if (state.route == AppRoute.Register) AccountMode.Register else AccountMode.Recovery
                    val presenter = remember(mode) { accountFactory(mode, accountScope) }
                    AccountAuthScreen(presenter) { controller.accountRoute(AppRoute.Login) }
                }
                AppRoute.Home -> {
                    val home = remember(state.route) { controller.attachHome(homeFactory) }
                    val workspace = remember(home) { controller.attachWorkspace(workspaceFactory) }
                    if (workspace != null) FinanceWorkspaceScreen(workspace, valuesHidden, { valuesHidden = !valuesHidden }) { controller.logout() }
                }
                AppRoute.BiometricOffer -> GateScreen("Ativar biometria?",
                    "Deseja usar Face ID ou Touch ID para deixar seu aplicativo mais seguro?", state,
                    "Ativar", { controller.finishLogin(true) }, "Agora não", { controller.finishLogin(false) })
                AppRoute.Locked -> GateScreen("Desbloquear SobraAi", "Autentique-se para acessar sua conta.", state,
                    "Entrar com biometria", controller::unlock, "Entrar com senha", { controller.logout() })
                AppRoute.StorageError -> GateScreen("Sessão indisponível", "Seus dados ficam protegidos no aparelho.", state,
                    "Tentar novamente", controller::restore, "Encerrar sessão", { controller.logout() })
            }
        }
    }
}

@Composable
internal fun AppLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp, strokeWidth: Dp = 2.dp) {
    CircularProgressIndicator(modifier.size(size), color = color, strokeWidth = strokeWidth)
}

@Composable
private fun GateScreen(title: String, subtitle: String, state: AppState, primary: String,
    onPrimary: () -> Unit, secondary: String, onSecondary: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        AuthHeader(Icons.Default.AccountBalanceWallet, "SobraAi", title, subtitle)
        state.message?.let { AuthErrorMessage(it) }
        AuthPrimaryButton(primary, state.busy, true, onPrimary)
        TextButton(onClick = onSecondary, enabled = !state.busy) { Text(secondary) }
    }
}

@Composable
private fun LoginScreen(state: AppState, onLogin: (String, String) -> Unit, register: () -> Unit, recovery: () -> Unit) {
    // Deliberately not saveable: passwords must not be serialized into restored UI state.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var time by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    val focus = LocalFocusManager.current
    LaunchedEffect(state.blockedUntil) {
        do { time = Clock.System.now().toEpochMilliseconds(); delay(1000) } while (time < state.blockedUntil)
    }
    val blocked = email.trim().lowercase() == state.blockedEmail && time < state.blockedUntil
    val remaining = ((state.blockedUntil - time).coerceAtLeast(0) + 999) / 1000
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
        .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        AuthHeader(Icons.Default.AccountBalanceWallet, "SobraAi", "Bem-vindo de volta", "Faça login na sua conta para continuar")
        Spacer(Modifier.height(32.dp))
        AuthTextField("E-mail", email, { email = it.replace(" ", "").replace("\n", "") },
            "Digite o seu e-mail", !state.busy, KeyboardType.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
        Spacer(Modifier.height(16.dp))
        AuthPasswordField("Senha", password, { password = it.replace("\n", "") }, "Digite a sua senha", !state.busy)
        TextButton(onClick = recovery, enabled = !state.busy) { Text("Esqueci minha senha") }
        Spacer(Modifier.height(24.dp))
        state.message?.let { AuthErrorMessage(it); Spacer(Modifier.height(12.dp)) }
        AuthPrimaryButton(if (blocked) "Tente novamente em ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}" else "Entrar",
            state.busy, !blocked, { focus.clearFocus(); onLogin(email, password) })
        TextButton(onClick = register, enabled = !state.busy) { Text("Não tem uma conta? Cadastre-se") }
    }
}
