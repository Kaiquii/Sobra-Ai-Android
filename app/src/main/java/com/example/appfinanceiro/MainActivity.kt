package com.example.appfinanceiro

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appfinanceiro.core.biometric.BiometricAuth
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.theme.AppFinanceiroTheme
import com.example.appfinanceiro.core.network.SessionAccessEvents
import com.example.appfinanceiro.feature.assistant.AssistantScreen
import com.example.appfinanceiro.feature.despesas.DespesasScreen
import com.example.appfinanceiro.feature.despesas.components.EditarDespesaScreen
import com.example.appfinanceiro.feature.despesas.components.NovaDespesaScreen
import com.example.appfinanceiro.feature.home.HomeScreen
import com.example.appfinanceiro.feature.login.EsqueciSenhaScreen
import com.example.appfinanceiro.feature.login.LoginScreen
import com.example.appfinanceiro.feature.login.RegisterScreen
import com.example.appfinanceiro.feature.perfil.PerfilScreen
import com.example.appfinanceiro.feature.perfil.components.CategoriasScreen
import com.example.appfinanceiro.feature.perfil.components.ConfiguracoesRendaScreen
import com.example.appfinanceiro.feature.perfil.components.EditarPerfilScreen
import com.example.appfinanceiro.feature.relatorios.InstallmentCommitmentsScreen
import com.example.appfinanceiro.feature.relatorios.RelatoriosScreen
import com.example.appfinanceiro.feature.perfil.components.AjudaScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)

        setContent {
            val userToken by sessionManager.token.collectAsState(initial = null)
            val savedUserEmail by sessionManager.userEmail.collectAsState(initial = "")
            val biometricEnabledEmails by sessionManager.biometricEnabledEmails.collectAsState(initial = emptySet())

            val biometricEnabledForSavedUser =
                savedUserEmail.isNotBlank() &&
                        biometricEnabledEmails.contains(savedUserEmail.trim().lowercase())

            val activity = LocalActivity.current as? FragmentActivity

            var unlockedByBiometric by remember { mutableStateOf(false) }
            var biometricChecked by remember { mutableStateOf(false) }

            LaunchedEffect(userToken, biometricEnabledForSavedUser) {
                if (userToken == null) {
                    unlockedByBiometric = false
                    biometricChecked = true
                    return@LaunchedEffect
                }

                if (!biometricEnabledForSavedUser) {
                    unlockedByBiometric = true
                    biometricChecked = true
                    return@LaunchedEffect
                }

                if (activity != null && BiometricAuth.isAvailable(activity)) {
                    BiometricAuth.showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            unlockedByBiometric = true
                            biometricChecked = true
                        },
                        onError = {
                            unlockedByBiometric = false
                            biometricChecked = true
                        }
                    )
                } else {
                    unlockedByBiometric = false
                    biometricChecked = true
                }
            }

            AppFinanceiroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    fun navigateToLogin() {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }

                    LaunchedEffect(Unit) {
                        SessionAccessEvents.accessRevoked.collect { message ->
                            sessionManager.clearSession()
                            Toast.makeText(
                                this@MainActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                            navigateToLogin()
                        }
                    }

                    val destination = when {
                        userToken == null -> "login"
                        !biometricEnabledForSavedUser -> "home"
                        biometricChecked && unlockedByBiometric -> "home"
                        else -> "login"
                    }

                    NavHost(navController = navController, startDestination = destination) {

                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                },
                                onNavigateToForgot = {
                                    navController.navigate("esqueci_senha")
                                }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onRegisterSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("esqueci_senha") {
                            EsqueciSenhaScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                onNavigate = { tabIndex ->
                                    when (tabIndex) {
                                        1 -> navController.navigate("despesas") { launchSingleTop = true }
                                        2 -> navController.navigate("relatorios") { launchSingleTop = true }
                                        3 -> navController.navigate("perfil") { launchSingleTop = true }
                                    }
                                },
                                onAddClick = {
                                    navController.navigate("nova_despesa")
                                },
                                onAssistantClick = {
                                    navController.navigate("assistant") { launchSingleTop = true }
                                },
                                onSessionExpired = { navigateToLogin() }
                            )
                        }

                        composable("assistant") {
                            AssistantScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSessionExpired = { navigateToLogin() }
                            )
                        }

                        composable("perfil") {
                            PerfilScreen(
                                onLogoutClick = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigate = { tabIndex ->
                                    when (tabIndex) {
                                        0 -> navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        1 -> navController.navigate("despesas") { launchSingleTop = true }
                                        2 -> navController.navigate("relatorios") { launchSingleTop = true }
                                    }
                                },
                                onIncomeSettingsClick = {
                                    navController.navigate("configuracoes_renda")
                                },
                                onCategoriesClick = {
                                    navController.navigate("categorias")
                                },
                                onEditProfileClick = {
                                    navController.navigate("editar_perfil")
                                },
                                onHelpClick = {
                                    navController.navigate("ajuda")
                                }
                            )
                        }


                        composable("nova_despesa") {
                            NovaDespesaScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("despesas") {
                            DespesasScreen(
                                onNavigate = { tabIndex ->
                                    when (tabIndex) {
                                        0 -> navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        2 -> navController.navigate("relatorios") { launchSingleTop = true }
                                        3 -> navController.navigate("perfil") { launchSingleTop = true }
                                    }
                                },
                                onAddClick = {
                                    navController.navigate("nova_despesa")
                                },
                                onEditClick = { expenseId ->
                                    navController.navigate("editar_despesa/$expenseId")
                                },
                                onSessionExpired = { navigateToLogin() }
                            )
                        }

                        composable("editar_despesa/{expenseId}") { backStackEntry ->
                            val expenseId = backStackEntry.arguments?.getString("expenseId")?.toIntOrNull()
                            if (expenseId != null) {
                                EditarDespesaScreen(
                                    expenseId = expenseId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }

                        composable("configuracoes_renda") {
                            ConfiguracoesRendaScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("categorias") {
                            CategoriasScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("editar_perfil") {
                            EditarPerfilScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("relatorios") {
                            RelatoriosScreen(
                                onNavigate = { tabIndex ->
                                    when (tabIndex) {
                                        0 -> navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        1 -> navController.navigate("despesas") { launchSingleTop = true }
                                        3 -> navController.navigate("perfil") { launchSingleTop = true }
                                    }
                                },
                                onAddClick = {
                                    navController.navigate("nova_despesa")
                                },
                                onInstallmentsClick = {
                                    navController.navigate("compromissos_parcelados") {
                                        launchSingleTop = true
                                    }
                                },
                                onSessionExpired = { navigateToLogin() }
                            )
                        }

                        composable("compromissos_parcelados") {
                            InstallmentCommitmentsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSessionExpired = { navigateToLogin() }
                            )
                        }

                        composable("ajuda") {
                            AjudaScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                    }
                }
            }
        }
    }
}
