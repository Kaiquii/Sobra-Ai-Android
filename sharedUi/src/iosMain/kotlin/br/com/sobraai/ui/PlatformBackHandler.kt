package br.com.sobraai.ui
import androidx.compose.runtime.Composable
// iOS uses the shared visible back actions and shared horizontal tab gesture.
@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
