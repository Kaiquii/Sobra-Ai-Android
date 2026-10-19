package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.readAvailable
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.decodeFromString

enum class ExportKind(val api: String, val label: String) {
    Expenses("expenses", "Despesas"), Incomes("incomes", "Receitas"), Categories("categories", "Categorias"),
    Summary("summary", "Resumo mensal"), Comparison("month_comparison", "Comparativo mensal"),
    Commitments("installment_commitments", "Compromissos parcelados"), Full("full_report", "Relatório completo")
}
enum class ExportFormat(val api: String, val label: String, val mime: String) {
    Pdf("pdf", "PDF", "application/pdf"), Xlsx("xlsx", "Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), Csv("csv", "CSV", "text/csv")
}
data class ExportRequest(val kind: ExportKind, val month: Int, val year: Int, val format: ExportFormat,
    val compareMonth: Int? = null, val compareYear: Int? = null, val months: Int? = null, val includeCurrentAsPaid: Boolean? = null)
data class FinanceFile(val bytes: ByteArray, val fileName: String, val mime: String)
data class ProfilePhoto(val bytes: ByteArray, val fileName: String = "avatar.jpg", val mime: String = "image/jpeg")
interface FinanceFilesDataSource {
    suspend fun export(token: String, request: ExportRequest): FinanceFile
    suspend fun uploadPhoto(token: String, photo: ProfilePhoto): ProfilePhotoResponse
    suspend fun avatar(token: String, reference: String): ByteArray
}

/** File boundary for Apple/KMP. Existing Android streaming/Retrofit entry points stay intact. */
class KtorFinanceFiles(private val client: HttpClient, private val configuration: ApiConfiguration) : FinanceFilesDataSource {
    override suspend fun export(token: String, request: ExportRequest): FinanceFile {
        require(request.month in 1..12 && request.year in 1..9999)
        require(request.months == null || request.months in 1..120)
        require(request.compareMonth == null || request.compareMonth in 1..12)
        val fallback = "${request.kind.api}-${request.year}-${request.month.toString().padStart(2, '0')}.${request.format.api}"
        return safely {
            client.prepareGet(configuration.baseUrl + "api/reports/export") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("type", request.kind.api); parameter("month", request.month); parameter("year", request.year); parameter("format", request.format.api)
                request.compareMonth?.let { parameter("compare_month", it) }; request.compareYear?.let { parameter("compare_year", it) }
                request.months?.let { parameter("months", it) }; request.includeCurrentAsPaid?.let { parameter("include_current_month_as_paid", it) }
            }.execute { response ->
                checkResponse(response)
                val mime = response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim() ?: request.format.mime
                require(mime == request.format.mime || mime == "application/octet-stream" || request.format == ExportFormat.Csv && mime == "application/csv") { "O servidor não retornou o formato solicitado." }
                val bytes = readLimited(response, 50 * 1024 * 1024)
                require(bytes.isNotEmpty()) { "O servidor retornou um arquivo vazio." }
                if (request.format == ExportFormat.Pdf) require(bytes.take(4).toByteArray().decodeToString() == "%PDF") { "O arquivo PDF recebido é inválido." }
                if (request.format == ExportFormat.Xlsx) require(bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte()) { "A planilha recebida é inválida." }
                FinanceFile(bytes, safeFileName(response.headers[HttpHeaders.ContentDisposition], fallback), request.format.mime)
            }
        }
    }
    override suspend fun uploadPhoto(token: String, photo: ProfilePhoto): ProfilePhotoResponse {
        require(photo.bytes.isNotEmpty() && photo.bytes.size <= 15 * 1024 * 1024) { "Selecione uma foto de até 15 MB." }
        require(photo.mime == "image/jpeg" && photo.bytes.size > 2 && photo.bytes[0] == 0xff.toByte() && photo.bytes[1] == 0xd8.toByte()) { "A foto precisa estar em JPEG." }
        return safely {
            client.preparePatch(configuration.baseUrl + "api/users/profile/photo") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(MultiPartFormDataContent(formData { append("photo", photo.bytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg"); append(HttpHeaders.ContentDisposition, "filename=\"avatar.jpg\"")
                }) }))
            }.execute { response -> checkResponse(response); FinanceJson.decodeFromString(readLimited(response, 1024 * 1024).decodeToString()) }
        }
    }
    override suspend fun avatar(token: String, reference: String): ByteArray {
        val base = Url(configuration.baseUrl)
        val url = Url(if (reference.startsWith("https://", true)) reference else {
            require(!reference.contains("://") && !reference.startsWith("//") && !reference.contains("..")) { "Endereço de foto inválido." }
            configuration.baseUrl + reference.trimStart('/')
        })
        require(url.protocol == URLProtocol.HTTPS && url.user == null && url.password == null) { "A foto deve usar HTTPS." }
        return safely {
            client.prepareGet(url) {
                if (url.host == base.host && url.port == base.port) header(HttpHeaders.Authorization, "Bearer $token")
            }.execute { response ->
                // A public CDN failure must not invalidate the user's account.
                checkResponse(response, authenticated = url.host == base.host && url.port == base.port)
                require(response.headers[HttpHeaders.ContentType]?.startsWith("image/") == true) { "O servidor não retornou uma imagem." }
                readLimited(response, 15 * 1024 * 1024)
            }
        }
    }
}

internal suspend fun readLimited(response: HttpResponse, maximum: Int): ByteArray {
    require((response.contentLength() ?: 0) <= maximum) { "O arquivo excede o tamanho permitido." }
    val channel = response.bodyAsChannel()
    val chunks = mutableListOf<ByteArray>(); var total = 0
    while (true) {
        val buffer = ByteArray(16 * 1024)
        val count = channel.readAvailable(buffer, 0, buffer.size)
        if (count < 0) break
        if (count == 0) continue
        total += count
        require(total <= maximum) { "O arquivo excede o tamanho permitido." }
        chunks += buffer.copyOf(count)
    }
    return ByteArray(total).also { result -> var position = 0; chunks.forEach { it.copyInto(result, position); position += it.size } }
}
private suspend fun checkResponse(response: HttpResponse, authenticated: Boolean = true) {
    if (response.status.value in 200..299) return
    if (authenticated && response.status.value == 401) throw SessionExpiredException()
    val message = parseApiErrorMessage(readLimited(response, 1024 * 1024).decodeToString())
    if (authenticated && response.status.value == 403 && message?.contains("Acesso revogado", true) == true) SessionAccessEvents.notifyAccessRevoked(message)
    throw ApiResponseException(response.status.value, message)
}
private suspend fun <T> safely(block: suspend () -> T): T = try { block() }
catch (cancel: CancellationException) { throw cancel }
catch (error: HttpRequestTimeoutException) { throw NetworkRequestException("A operação demorou mais que o esperado. Tente novamente.", error) }
catch (error: IOException) { throw NetworkRequestException("Não foi possível conectar ao servidor. Verifique sua internet.", error) }

internal fun safeFileName(disposition: String?, fallback: String): String {
    val candidate = disposition?.substringAfter("filename=", "")?.substringBefore(';')?.trim()?.trim('"', '\'')?.takeIf { it.isNotBlank() } ?: fallback
    return candidate.substringAfterLast('/').substringAfterLast('\\').replace(Regex("[\\x00-\\x1F<>:\"/\\\\|?*]"), "_").trim().trim('.').take(120).ifBlank { fallback }
}
