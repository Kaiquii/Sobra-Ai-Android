package com.example.appfinanceiro.feature.perfil.components

import br.com.sobraai.ui.runtime.UiToast as Toast
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import br.com.sobraai.ui.runtime.LocalUiServices
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.runtime.rememberPresentation
import com.example.appfinanceiro.core.data.FinanceActionsViewModel
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.network.UpdateProfileRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    onNavigateBack: () -> Unit,
    actionsViewModel: FinanceActionsViewModel = rememberPresentation("FinanceActionsViewModel") { services -> FinanceActionsViewModel(services.finance, services.files) }
) {
    val context = LocalUiServices.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { context.session }

    val userToken by sessionManager.token.collectAsState(initial = null)
    val currentName by sessionManager.userName.collectAsState(initial = "")
    val currentEmail by sessionManager.userEmail.collectAsState(initial = "")
    val currentAvatarUrl by sessionManager.userAvatarUrl.collectAsState(initial = "")

    val backgroundColor = MaterialTheme.colorScheme.background
    val currentRole by sessionManager.userRole.collectAsState(initial = "")

    var name by remember(currentName) { mutableStateOf(currentName) }
    var email by remember(currentEmail) { mutableStateOf(currentEmail) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            SettingsTopBar("Editar Perfil", onNavigateBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsHeading(
                title = "Do seu jeito",
                subtitle = "Mantenha seus dados pessoais atualizados.",
                icon = Icons.Default.Person
            )
            SettingsCard {
                SettingsHeading("Informações pessoais", "Edite seu nome e e-mail de acesso.")
                SettingsTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nome",
                    icon = Icons.Default.Person,
                    enabled = !isLoading
                )
                SettingsTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "E-mail",
                    icon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isLoading
                )
            }
            SettingsPrimaryButton(
                label = "Salvar alterações",
                loading = isLoading,
                onClick = {
                    if (name.isBlank() || email.isBlank()) {
                        Toast.makeText(context, "Preencha nome e e-mail", Toast.LENGTH_SHORT).show()
                        return@SettingsPrimaryButton
                    }

                    val token = userToken
                    if (token.isNullOrBlank()) {
                        Toast.makeText(context, "Sessão inválida. Faça login novamente.", Toast.LENGTH_SHORT).show()
                        return@SettingsPrimaryButton
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            actionsViewModel.updateProfile(
                                token,
                                UpdateProfileRequest(
                                    name = name.trim(),
                                    email = email.trim()
                                )
                            )

                            context.persistProfileIdentity(
                                token = token,
                                name = name.trim(),
                                email = email.trim(),
                                role = currentRole,
                                avatarUrl = currentAvatarUrl
                            )

                            Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            Toast.makeText(
                                context,
                                e.userMessageOr("Erro ao atualizar perfil"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            )
        }
    }
}
