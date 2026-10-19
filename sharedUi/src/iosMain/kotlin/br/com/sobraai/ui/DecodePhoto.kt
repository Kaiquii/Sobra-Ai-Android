package br.com.sobraai.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun decodePhoto(bytes: ByteArray): ImageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
