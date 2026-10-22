package com.example.appfinanceiro.platform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentActivity
import br.com.sobraai.ui.NativeFeatures
import br.com.sobraai.ui.runtime.ReportFiles
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.relatorios.export.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Android APIs only. Screen layout, navigation and presentation live in sharedUi/commonMain. */
class AndroidNativeFeatures(private val activity: FragmentActivity) : NativeFeatures, ReportFiles {
    private var photoResult: CancellableContinuation<Uri?>? = null
    private var documentResult: CancellableContinuation<Uri?>? = null
    private var permissionResult: CancellableContinuation<Boolean>? = null
    private var voiceResult: CancellableContinuation<String?>? = null
    private var speech: SpeechRecognizer? = null
    val externalInteraction: Boolean get() = photoResult != null || documentResult != null || permissionResult != null
    private val photos = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoResult?.takeIf { it.isActive }?.resume(uri); photoResult = null
    }
    private val documents = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        documentResult?.takeIf { it.isActive }?.resume(uri); documentResult = null
    }
    private val microphone = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        permissionResult?.takeIf { it.isActive }?.resume(allowed); permissionResult = null
    }

    override suspend fun pickPhoto(): ProfilePhoto? {
        val uri = suspendCancellableCoroutine<Uri?> { c ->
            check(photoResult == null); photoResult = c
            c.invokeOnCancellation { photoResult = null }; photos.launch("image/*")
        } ?: return null
        return withContext(Dispatchers.IO) {
            val resolver = activity.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Não foi possível ler a imagem selecionada." }
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (bounds.outWidth / inSampleSize > 2048 || bounds.outHeight / inSampleSize > 2048) inSampleSize *= 2
            }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Não foi possível decodificar a imagem selecionada.")
            val orientation = resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> preScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); preScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); preScale(-1f, 1f) }
                }
            }
            val normalized = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            try {
                val bytes = ByteArrayOutputStream().use { output -> normalized.compress(Bitmap.CompressFormat.JPEG, 90, output); output.toByteArray() }
                ProfilePhoto(bytes)
            } finally { if (normalized !== bitmap) normalized.recycle(); bitmap.recycle() }
        }
    }

    override suspend fun transcribe(onPartial: (String) -> Unit): String? {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val allowed = suspendCancellableCoroutine { c ->
                check(permissionResult == null); permissionResult = c
                c.invokeOnCancellation { permissionResult = null }; microphone.launch(Manifest.permission.RECORD_AUDIO)
            }
            check(allowed) { "Permissão de microfone negada." }
        }
        check(SpeechRecognizer.isRecognitionAvailable(activity)) { "Reconhecimento de voz indisponível neste aparelho." }
        return suspendCancellableCoroutine { c ->
            speech?.destroy()
            voiceResult = c
            val recognizer = SpeechRecognizer.createSpeechRecognizer(activity); speech = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
                }
                override fun onResults(results: Bundle?) {
                    if (c.isActive) c.resume(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim())
                    voiceResult = null; recognizer.destroy(); speech = null
                }
                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi. Tente falar de novo."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não ouvi nada. Toque no microfone e tente novamente."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone negada."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Falha de rede ao transcrever a voz."
                        else -> "Não foi possível transcrever a voz."
                    }
                    if (c.isActive) c.resumeWithException(IllegalStateException(message))
                    voiceResult = null; recognizer.destroy(); speech = null
                }
            })
            c.invokeOnCancellation { recognizer.destroy(); speech = null; voiceResult = null }
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR"); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        }
    }

    override suspend fun export(token: String, request: ReportExportRequest): ExportedReport? {
        val destination = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) suspendCancellableCoroutine<Uri?> { c ->
            check(documentResult == null); documentResult = c
            c.invokeOnCancellation { documentResult = null }; documents.launch(fallbackFileName(request))
        } ?: return null else null
        return ReportFileStore(activity).save(ReportExportRepository().download(token, request), destination)
    }
    override fun open(report: ExportedReport) {
        activity.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(report.uri), report.mimeType); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
    }
    override fun share(report: ExportedReport) {
        activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = report.mimeType; putExtra(Intent.EXTRA_STREAM, Uri.parse(report.uri)); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Compartilhar relatório"))
    }
    override suspend fun share(file: FinanceFile) {
        val uri = withContext(Dispatchers.IO) {
            val directory = java.io.File(activity.cacheDir, "reports/${java.util.UUID.randomUUID()}")
            check(directory.mkdirs()) { "Não foi possível preparar o relatório." }
            val name = file.fileName.substringAfterLast('/').substringAfterLast('\\')
                .replace(Regex("[\\x00-\\x1F<>:\"/\\\\|?*]"), "_").ifBlank { "relatorio" }
            val target = java.io.File(directory, name)
            target.writeBytes(file.bytes)
            FileProvider.getUriForFile(activity, "${activity.packageName}.files", target)
        }
        currentCoroutineContext().ensureActive()
        share(ExportedReport(uri.toString(), file.fileName, file.mime))
    }
    override fun openLink(url: String) { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    override fun cancelInteractions() {
        photoResult?.cancel(); documentResult?.cancel(); permissionResult?.cancel(); voiceResult?.cancel()
        photoResult = null; documentResult = null; permissionResult = null; voiceResult = null
        speech?.destroy(); speech = null
    }
}
