package br.com.sobraai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.sobraai.ui.components.AuthPasswordField
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable
internal fun AccountAuthScreen(presenter: AccountAuthPresenter, back: () -> Unit) {
    val state by presenter.state.collectAsState()
    var time by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(state.retryAt) { do { time = Clock.System.now().toEpochMilliseconds(); delay(1000) } while (time < state.retryAt) }
    val remaining = ((state.retryAt - time).coerceAtLeast(0) + 999) / 1000
    val register = presenter.mode == AccountMode.Register
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { FinanceHeader(if (register) "Criar conta" else "Recuperar senha", back, !state.busy) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!state.confirmation) {
                if (register) FormInput("Nome", state.draft.name, { v -> presenter.update { it.copy(name = v) } }, !state.busy)
                FormInput("E-mail", state.draft.email, { v -> presenter.update { it.copy(email = v.replace(" ", "").replace("\n", "")) } }, !state.busy, KeyboardType.Email)
                if (register) AuthPasswordField("Senha", state.draft.password, { v -> presenter.update { it.copy(password = v) } }, "Crie sua senha", !state.busy)
                state.deliveryReason?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(onClick = presenter::requestCode, enabled = !state.busy && state.deliveryEnabled && remaining == 0L,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (remaining > 0) "Aguarde ${remaining}s" else "Enviar código") }
                TextButton(onClick = presenter::useExistingCode, enabled = !state.busy) { Text("Já tenho um código válido") }
            } else {
                Text("Informe o código de 6 dígitos recebido em ${state.draft.email}.")
                FormInput("Código de 6 dígitos", state.draft.code, { v -> presenter.update { it.copy(code = v.filter(Char::isDigit).take(6)) } }, !state.busy, KeyboardType.Number)
                if (!register) {
                    AuthPasswordField("Nova senha", state.draft.password, { v -> presenter.update { it.copy(password = v) } }, "Crie sua nova senha", !state.busy)
                    AuthPasswordField("Confirmar senha", state.draft.confirm, { v -> presenter.update { it.copy(confirm = v) } }, "Digite a senha novamente", !state.busy)
                }
                Button(onClick = presenter::submit, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (register) "Criar conta" else "Redefinir senha") }
                TextButton(onClick = presenter::requestCode, enabled = !state.busy && state.deliveryEnabled && remaining == 0L) { Text(if (remaining > 0) "Reenviar em ${remaining}s" else "Reenviar código") }
                TextButton(onClick = presenter::details, enabled = !state.busy) { Text("Corrigir dados") }
            }
            state.message?.let { Text(it) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.busy) AppLoadingIndicator()
        }
    }
}
