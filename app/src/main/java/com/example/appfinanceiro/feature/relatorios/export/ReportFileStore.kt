package com.example.appfinanceiro.feature.relatorios.export

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ReportFileStore(private val context: Context) {
    suspend fun save(
        download: ReportDownload,
        legacyDestination: Uri? = null
    ): ExportedReport = withContext(Dispatchers.IO) {
        download.body.use { responseBody ->
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ensureAvailableSpace(responseBody.contentLength())
            }
            val destination = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                createMediaStoreDestination(
                    resolver = resolver,
                    requestedName = download.fileName,
                    mimeType = download.mimeType
                )
            } else {
                val uri = legacyDestination
                    ?: throw IllegalArgumentException("Escolha onde salvar o relatório.")
                FileDestination(uri = uri, fileName = download.fileName, pending = false)
            }

            try {
                resolver.openOutputStream(destination.uri, "w")?.use { output ->
                    responseBody.byteStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                } ?: throw IOException("Não foi possível criar o arquivo no aparelho.")

                if (destination.pending) {
                    resolver.update(
                        destination.uri,
                        ContentValues().apply {
                            put(MediaStore.Downloads.IS_PENDING, 0)
                        },
                        null,
                        null
                    )
                }

                ExportedReport(
                    uri = destination.uri,
                    fileName = destination.fileName,
                    mimeType = download.mimeType
                )
            } catch (error: Throwable) {
                runCatching { resolver.delete(destination.uri, null, null) }
                throw error
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createMediaStoreDestination(
        resolver: ContentResolver,
        requestedName: String,
        mimeType: String
    ): FileDestination {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/SobraAi"
        val uniqueName = uniqueDownloadName(resolver, requestedName, relativePath)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, uniqueName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Não foi possível reservar espaço para o relatório.")

        return FileDestination(uri = uri, fileName = uniqueName, pending = true)
    }

    private fun ensureAvailableSpace(requiredBytes: Long) {
        if (requiredBytes <= 0L) return
        val availableBytes = StatFs(Environment.getExternalStorageDirectory().path).availableBytes
        val safetyMargin = 1_048_576L
        if (availableBytes < requiredBytes + safetyMargin) {
            throw IOException("Não há espaço suficiente para salvar o relatório.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun uniqueDownloadName(
        resolver: ContentResolver,
        requestedName: String,
        relativePath: String
    ): String {
        val base = requestedName.substringBeforeLast('.', requestedName)
        val extension = requestedName.substringAfterLast('.', "")
        var candidate = requestedName
        var suffix = 1

        while (downloadExists(resolver, candidate, relativePath)) {
            candidate = if (extension.isBlank()) {
                "$base ($suffix)"
            } else {
                "$base ($suffix).$extension"
            }
            suffix++
        }
        return candidate
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadExists(
        resolver: ContentResolver,
        fileName: String,
        relativePath: String
    ): Boolean {
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection =
            "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val args = arrayOf(fileName, "$relativePath/")

        return resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { it.moveToFirst() } == true
    }

    private data class FileDestination(
        val uri: Uri,
        val fileName: String,
        val pending: Boolean
    )
}
