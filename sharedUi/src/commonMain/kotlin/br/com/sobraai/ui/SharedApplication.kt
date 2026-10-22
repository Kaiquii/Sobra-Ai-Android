package br.com.sobraai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.runtime.*
import com.example.appfinanceiro.core.designsystem.theme.AppFinanceiroTheme
import com.example.appfinanceiro.core.update.AppUpdateGate
import com.example.appfinanceiro.core.designsystem.components.FinancialPrivacyProvider
import com.example.appfinanceiro.feature.login.*
import com.example.appfinanceiro.feature.login.components.*
import com.example.appfinanceiro.feature.home.HomeScreen
import com.example.appfinanceiro.feature.despesas.DespesasScreen
import com.example.appfinanceiro.feature.despesas.components.*
import com.example.appfinanceiro.feature.perfil.PerfilScreen
import com.example.appfinanceiro.feature.perfil.components.*
import com.example.appfinanceiro.feature.relatorios.*
import com.example.appfinanceiro.feature.assistant.AssistantScreen

/** The only application UI entry point, called by both platform hosts. */
@Composable
fun SobraAiApp(services: UiServices) {
    val controller = services.controller
    val state by controller.state.collectAsState()
    val snackbars = remember { SnackbarHostState() }
    LaunchedEffect(services) { services.messages.collect { snackbars.showSnackbar(it) } }
    LaunchedEffect(services.session) {
        services.session.token.collect { token ->
            if (token.isNullOrBlank() && controller.state.value.route == AppRoute.Home) controller.logout()
        }
    }
    CompositionLocalProvider(LocalUiServices provides services) {
        AppFinanceiroTheme {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize()) {
                    when (state.route) {
                        AppRoute.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppLoadingIndicator() }
                        AppRoute.Login -> LoginScreen({ controller.accountRoute(AppRoute.Register) }, { controller.accountRoute(AppRoute.Recovery) })
                        AppRoute.Register -> RegisterScreen({ controller.accountRoute(AppRoute.Login) }, { controller.accountRoute(AppRoute.Login) })
                        AppRoute.Recovery -> EsqueciSenhaScreen(onNavigateBack = { controller.accountRoute(AppRoute.Login) })
                        AppRoute.Home -> FinancialPrivacyProvider { SignedInScreens(services) }
                        AppRoute.BiometricOffer -> GateScreen("Ativar ${controller.biometricKind.label}?",
                            "Use ${controller.biometricKind.label} para proteger o acesso à sua conta no SobraAi.", state,
                            "Ativar ${controller.biometricKind.label}", { controller.finishLogin(true) }, "Agora não", { controller.finishLogin(false) }, biometricIcon(controller.biometricKind))
                        AppRoute.Locked -> GateScreen("Desbloquear SobraAi", "Autentique-se para acessar sua conta.", state,
                            "Entrar com ${controller.biometricKind.label}", controller::unlock, "Entrar com senha", { controller.logout() }, biometricIcon(controller.biometricKind))
                        AppRoute.StorageError -> GateScreen("Sessão indisponível", "Seus dados ficam protegidos no aparelho.", state,
                            "Tentar novamente", controller::restore, "Encerrar sessão", { controller.logout() })
                    }
                    SnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().imePadding())
                    AppUpdateGate()
                }
            }
        }
    }
}

internal enum class Screen { Home, Expenses, Reports, Profile, Assistant, NewExpense, EditExpense, Incomes, Categories, EditProfile, Commitments, Help }

@Composable
private fun SignedInScreens(services: UiServices) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home.name) }
    var parent by rememberSaveable { mutableStateOf(Screen.Home.name) }
    var expenseId by rememberSaveable { mutableIntStateOf(0) }
    fun tab(index: Int) { screen = listOf(Screen.Home, Screen.Expenses, Screen.Reports, Screen.Profile)[index].name }
    fun open(destination: Screen) { parent = screen; screen = destination.name }
    fun back() { screen = parent }
    fun add() = open(Screen.NewExpense)
    fun expired() = services.controller.logout("Sua sessão expirou. Entre novamente.")
    val models = remember(services) { PresentationStore() }
    DisposableEffect(models) { onDispose { models.close() } }
    PlatformBackHandler(screen != Screen.Home.name) {
        if (Screen.valueOf(screen) in listOf(Screen.Expenses, Screen.Reports, Screen.Profile)) tab(0) else back()
    }
    val savedScreens = rememberSaveableStateHolder()
    savedScreens.SaveableStateProvider(screen) {
      CompositionLocalProvider(LocalPresentationStore provides models, LocalPresentationRoute provides screen) {
      key(screen, expenseId) {
        when (Screen.valueOf(screen)) {
            Screen.Home -> HomeScreen(::tab, ::add, { open(Screen.Assistant) }, ::expired)
            Screen.Expenses -> DespesasScreen(::tab, ::add, { id -> expenseId = id; open(Screen.EditExpense) }, ::expired)
            Screen.Reports -> RelatoriosScreen(::tab, ::add, { open(Screen.Commitments) }, ::expired)
            Screen.Profile -> PerfilScreen({ services.controller.logout() }, ::tab, ::add,
                { open(Screen.Incomes) }, { open(Screen.Categories) }, { open(Screen.EditProfile) }, { open(Screen.Help) })
            Screen.Assistant -> AssistantScreen(::back, ::expired)
            Screen.NewExpense -> NovaDespesaScreen(::back)
            Screen.EditExpense -> EditarDespesaScreen(expenseId, ::back)
            Screen.Incomes -> ConfiguracoesRendaScreen(::back)
            Screen.Categories -> CategoriasScreen(::back)
            Screen.EditProfile -> EditarPerfilScreen(::back)
            Screen.Commitments -> InstallmentCommitmentsScreen(::back, ::expired)
            Screen.Help -> AjudaScreen(::back)
        }
      }
      }
    }
}

@Composable
internal fun AppLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 18.dp, strokeWidth: Dp = 2.dp) {
    com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator(modifier, size, strokeWidth, color)
}
@Composable
private fun GateScreen(title: String, subtitle: String, state: AppState, primary: String,
    onPrimary: () -> Unit, secondary: String, onSecondary: () -> Unit,
    icon: ImageVector = Icons.Default.AccountBalanceWallet) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        AuthHeader(icon, "SobraAi", title, subtitle)
        state.message?.let { AuthErrorMessage(it) }
        AuthPrimaryButton(primary, state.busy, true, onPrimary)
        TextButton(onClick = onSecondary, enabled = !state.busy) { Text(secondary) }
    }
}
internal fun biometricIcon(kind: BiometricKind): ImageVector = if (kind == BiometricKind.FaceId) FaceIdIcon else Icons.Default.Fingerprint
@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
