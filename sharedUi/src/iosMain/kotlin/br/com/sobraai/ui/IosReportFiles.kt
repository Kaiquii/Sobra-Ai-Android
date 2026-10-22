@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package br.com.sobraai.ui

import br.com.sobraai.ui.runtime.ReportFiles
import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.relatorios.export.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

internal class IosReportFiles(private val files: FinanceFilesDataSource, private val native: IosNativeFeatures) : ReportFiles {
    private var preview: UIDocumentInteractionController? = null
    private var previewDelegate: UIDocumentInteractionControllerDelegateProtocol? = null
    override suspend fun export(token: String, request: ReportExportRequest): ExportedReport {
        val file = files.export(token, ExportRequest(ExportKind.entries.first { it.api == request.type.apiValue }, request.month,
            request.year, ExportFormat.entries.first { it.api == request.format.apiValue }, request.compareMonth, request.compareYear,
            request.months, request.includeCurrentMonthAsPaid))
        currentCoroutineContext().ensureActive()
        val directory = NSHomeDirectory() + "/Documents/SobraAi"
        val manager = NSFileManager.defaultManager
        check(manager.createDirectoryAtPath(directory, true, mapOf(NSFileProtectionKey to NSFileProtectionComplete), null)) { "Não foi possível criar a pasta de relatórios." }
        var name = file.fileName
        var suffix = 1
        while (manager.fileExistsAtPath("$directory/$name")) {
            val base = file.fileName.substringBeforeLast('.', file.fileName)
            val extension = file.fileName.substringAfterLast('.', "")
            name = "$base (${suffix++})${if (extension.isBlank()) "" else ".$extension"}"
        }
        val path = "$directory/$name"
        val data = file.bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = file.bytes.size.toULong()) }
        try {
            check(data.writeToFile(path, true)) { "Não foi possível salvar o relatório. Verifique o espaço disponível." }
            check(manager.setAttributes(mapOf(NSFileProtectionKey to NSFileProtectionComplete), path, null)) { "Não foi possível proteger o relatório salvo." }
            currentCoroutineContext().ensureActive()
        } catch (error: Throwable) { manager.removeItemAtPath(path, null); throw error }
        return ExportedReport(path, name, file.mime)
    }
    override fun open(report: ExportedReport) {
        native.cancelInteractions()
        val host = native.host ?: return
        val delegate = object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
            override fun documentInteractionControllerViewControllerForPreview(controller: UIDocumentInteractionController): UIViewController = host
        }
        previewDelegate = delegate
        preview = UIDocumentInteractionController.interactionControllerWithURL(NSURL.fileURLWithPath(report.uri)).apply {
            this.delegate = delegate
            presentPreviewAnimated(true)
        }
        native.retainInteraction {
            preview?.dismissPreviewAnimated(false)
            preview = null
            previewDelegate = null
        }
    }
    override fun share(report: ExportedReport) = native.shareSavedFile(report.uri)
}
