package br.com.sobraai.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodePhoto(bytes: ByteArray): ImageBitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).asImageBitmap()
