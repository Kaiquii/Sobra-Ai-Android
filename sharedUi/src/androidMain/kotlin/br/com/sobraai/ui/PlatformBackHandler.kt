package br.com.sobraai.ui
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
