package br.com.sobraai.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.example.appfinanceiro.core.network.FinanceFile
import com.example.appfinanceiro.core.network.ProfilePhoto

interface NativeFeatures {
    suspend fun pickPhoto(): ProfilePhoto?
    suspend fun share(file: FinanceFile)
    suspend fun transcribe(onPartial: (String) -> Unit): String?
    fun openLink(url: String)
    fun cancelInteractions()
}
expect fun decodePhoto(bytes: ByteArray): ImageBitmap
