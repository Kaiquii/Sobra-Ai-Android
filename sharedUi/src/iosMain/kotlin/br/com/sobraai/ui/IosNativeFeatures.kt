@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package br.com.sobraai.ui

import com.example.appfinanceiro.core.network.FinanceFile
import com.example.appfinanceiro.core.network.ProfilePhoto
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AVFAudio.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.Speech.*
import platform.UIKit.*
import platform.darwin.*
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

/** All delegates and native resources live as long as their cancellable operation. */
internal class IosNativeFeatures : NativeFeatures {
    var host: UIViewController? = null
    private var cancelCurrent: (() -> Unit)? = null
    private var revision = 0
    private fun presenter(): UIViewController {
        var current = host ?: error("Não foi possível abrir esta ação.")
        while (current.presentedViewController != null) current = current.presentedViewController!!
        return current
    }
    override fun cancelInteractions() { revision++; cancelCurrent?.invoke() }
    fun retainInteraction(onCancel: () -> Unit) { cancelCurrent = onCancel }

    fun shareSavedFile(path: String) {
        cancelInteractions()
        val sheet = UIActivityViewController(listOf(NSURL.fileURLWithPath(path)), null)
        val owner = presenter()
        sheet.popoverPresentationController?.sourceView = owner.view
        sheet.popoverPresentationController?.sourceRect = owner.view.bounds
        cancelCurrent = { sheet.dismissViewControllerAnimated(false, null); cancelCurrent = null }
        sheet.completionWithItemsHandler = { _, _, _, _ -> cancelCurrent = null }
        owner.presentViewController(sheet, true, null)
    }

    override suspend fun pickPhoto(): ProfilePhoto? = withContext(Dispatchers.Main) {
        cancelInteractions()
        suspendCancellableCoroutine { continuation ->
            val picker = UIImagePickerController()
            picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            var finished = false
            fun finish(photo: ProfilePhoto? = null, error: Throwable? = null) {
                if (finished) return
                finished = true; cancelCurrent = null
                picker.dismissViewControllerAnimated(true, null)
                if (continuation.isActive) {
                    if (error == null) continuation.resume(photo) else continuation.resumeWithException(error)
                }
            }
            val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
                override fun imagePickerControllerDidCancel(picker: UIImagePickerController) { finish() }
                override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                    try {
                        val original = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                            ?: error("Não foi possível ler a imagem selecionada.")
                        val size = original.size.useContents { width to height }
                        require(size.first > 0 && size.second > 0) { "Imagem inválida." }
                        val scale = min(1.0, 2048.0 / maxOf(size.first, size.second))
                        val target = CGSizeMake(size.first * scale, size.second * scale)
                        // Drawing applies EXIF orientation before uploading a bounded JPEG.
                        UIGraphicsBeginImageContextWithOptions(target, true, 1.0)
                        val normalized = try {
                            original.drawInRect(CGRectMake(0.0, 0.0, size.first * scale, size.second * scale))
                            UIGraphicsGetImageFromCurrentImageContext() ?: error("Não foi possível preparar a foto.")
                        } finally { UIGraphicsEndImageContext() }
                        val data = UIImageJPEGRepresentation(normalized, 0.9) ?: error("Não foi possível preparar a foto.")
                        finish(ProfilePhoto(data.bytesCopy()))
                    } catch (error: Exception) { finish(error = error) }
                }
            }
            picker.delegate = delegate
            // Capture the delegate strongly; UIKit keeps only a weak reference.
            cancelCurrent = { delegate.hash; finish() }
            continuation.invokeOnCancellation { dispatch_async(dispatch_get_main_queue()) { delegate.hash; finish() } }
            presenter().presentViewController(picker, true, null)
        }
    }

    override suspend fun share(file: FinanceFile) = withContext(Dispatchers.Main) {
        cancelInteractions()
        suspendCancellableCoroutine<Unit> { continuation ->
            val manager = NSFileManager.defaultManager
            val directory = NSTemporaryDirectory() + "sobraai-" + NSUUID().UUIDString
            val path = directory + "/" + file.fileName
            manager.createDirectoryAtPath(directory, true, mapOf(NSFileProtectionKey to NSFileProtectionComplete), null)
            if (!file.bytes.asData().writeToFile(path, true)) {
                manager.removeItemAtPath(directory, null)
                continuation.resumeWithException(IllegalStateException("Não foi possível preparar o arquivo para compartilhar."))
                return@suspendCancellableCoroutine
            }
            val sheet = UIActivityViewController(listOf(NSURL.fileURLWithPath(path)), null)
            val owner = presenter()
            sheet.popoverPresentationController?.sourceView = owner.view
            sheet.popoverPresentationController?.sourceRect = owner.view.bounds
            var finished = false
            fun finish() {
                if (finished) return
                finished = true; cancelCurrent = null
                manager.removeItemAtPath(directory, null)
                if (continuation.isActive) continuation.resume(Unit)
            }
            sheet.completionWithItemsHandler = { _, _, _, _ -> finish() }
            cancelCurrent = { sheet.dismissViewControllerAnimated(false, null); finish() }
            continuation.invokeOnCancellation { dispatch_async(dispatch_get_main_queue()) { sheet.dismissViewControllerAnimated(false, null); finish() } }
            owner.presentViewController(sheet, true, null)
        }
    }

    override suspend fun transcribe(onPartial: (String) -> Unit): String? = withContext(Dispatchers.Main) {
        cancelInteractions()
        val version = revision
        val authorized = suspendCancellableCoroutine { continuation ->
            SFSpeechRecognizer.requestAuthorization { status ->
                if (continuation.isActive) continuation.resume(status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized)
            }
        }
        require(authorized) { "Permita o reconhecimento de fala nos Ajustes para usar a voz." }
        if (version != revision) return@withContext null
        val audio = AVAudioSession.sharedInstance()
        val microphone = suspendCancellableCoroutine { continuation ->
            audio.requestRecordPermission { allowed -> if (continuation.isActive) continuation.resume(allowed) }
        }
        require(microphone) { "Permita o microfone nos Ajustes para usar a voz." }
        if (version != revision) return@withContext null
        val recognizer = SFSpeechRecognizer(NSLocale("pt-BR"))
        check(recognizer.available) { "Reconhecimento de voz indisponível no momento. Digite sua mensagem." }
        withTimeoutOrNull(45_000) {
            suspendCancellableCoroutine<String?> { continuation ->
                val engine = AVAudioEngine()
                val request = SFSpeechAudioBufferRecognitionRequest()
                request.shouldReportPartialResults = true
                var task: SFSpeechRecognitionTask? = null
                var tapInstalled = false
                var finished = false
                var latest = ""
                fun finish(error: Throwable? = null) {
                    if (finished) return
                    finished = true; cancelCurrent = null
                    engine.stop()
                    if (tapInstalled) engine.inputNode.removeTapOnBus(0u)
                    request.endAudio(); task?.cancel()
                    audio.setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
                    if (continuation.isActive) {
                        if (error != null && latest.isBlank()) continuation.resumeWithException(error)
                        else continuation.resume(latest.takeIf { it.isNotBlank() })
                    }
                }
                cancelCurrent = { finish() }
                continuation.invokeOnCancellation { dispatch_async(dispatch_get_main_queue()) { finish() } }
                try {
                    check(audio.setCategory(AVAudioSessionCategoryRecord, AVAudioSessionModeMeasurement, 0u, null)) { "Não foi possível iniciar o microfone." }
                    check(audio.setActive(true, null)) { "Não foi possível ativar o microfone." }
                    val input = engine.inputNode
                    val format = input.outputFormatForBus(0u)
                    check(format.sampleRate > 0 && format.channelCount > 0u) { "Microfone indisponível." }
                    input.installTapOnBus(0u, 1024u, format) { buffer, _ -> if (buffer != null) request.appendAudioPCMBuffer(buffer) }
                    tapInstalled = true
                    task = recognizer.recognitionTaskWithRequest(request) { result, error ->
                        dispatch_async(dispatch_get_main_queue()) {
                            if (!finished && continuation.isActive) {
                                result?.bestTranscription?.formattedString?.let { latest = it; onPartial(it) }
                                if (result?.final == true) finish()
                                else if (error != null) finish(IllegalStateException("Não foi possível reconhecer a fala. Digite sua mensagem ou tente novamente."))
                            }
                        }
                    }
                    engine.prepare()
                    check(engine.startAndReturnError(null)) { "Não foi possível iniciar a gravação." }
                } catch (error: Exception) { finish(error) }
            }
        }
    }

    override fun openLink(url: String) {
        val parsed = NSURL.URLWithString(url) ?: return
        if (parsed.scheme !in setOf("https", "mailto")) return
        UIApplication.sharedApplication.openURL(parsed, emptyMap<Any?, Any>(), null)
    }
}

private fun NSData.bytesCopy(): ByteArray = ByteArray(length.toInt()).also { result ->
    if (result.isNotEmpty()) result.usePinned { memcpy(it.addressOf(0), bytes, length) }
}
private fun ByteArray.asData(): NSData = if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
