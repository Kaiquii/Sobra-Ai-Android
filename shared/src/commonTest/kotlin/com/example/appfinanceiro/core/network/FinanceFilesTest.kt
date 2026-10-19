package com.example.appfinanceiro.core.network

import com.example.appfinanceiro.core.data.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.get
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class FinanceFilesTest {
    @Test fun exportPreservesAllQueryParametersAndSharesSafeFileName() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method); assertEquals("/api/reports/export", request.url.encodedPath)
            assertEquals("Bearer synthetic", request.headers[HttpHeaders.Authorization])
            assertEquals(mapOf("type" to "full_report", "month" to "10", "year" to "2026", "format" to "pdf", "compare_month" to "9", "compare_year" to "2026", "months" to "24", "include_current_month_as_paid" to "true"), request.url.parameters.entries().associate { it.key to it.value.single() })
            respond("%PDF-synthetic", headers = headersOf(HttpHeaders.ContentType to listOf("application/pdf"), HttpHeaders.ContentDisposition to listOf("attachment; filename=\"../../relatorio.pdf\"")))
        }) { configureFinanceHttpClient() }
        try {
            val file = KtorFinanceFiles(client, ApiConfiguration("https://example.invalid/")).export("synthetic", ExportRequest(ExportKind.Full, 10, 2026, ExportFormat.Pdf, 9, 2026, 24, true))
            assertEquals("relatorio.pdf", file.fileName); assertEquals("%PDF-synthetic", file.bytes.decodeToString())
        } finally { client.close() }
    }
    @Test fun binaryErrorBodyCannotMasqueradeAsSuccessfulPdf() = runTest {
        val client = HttpClient(MockEngine { respond("{\"message\":\"erro\"}", headers = headersOf(HttpHeaders.ContentType, "application/json")) })
        try { assertFailsWith<IllegalArgumentException> { KtorFinanceFiles(client, ApiConfiguration("https://example.invalid/")).export("synthetic", ExportRequest(ExportKind.Full, 10, 2026, ExportFormat.Pdf)) } }
        finally { client.close() }
    }
    @Test fun fileReadLimitRejectsAdvertisedAndUnadvertisedOversize() = runTest {
        for (declared in listOf(false, true)) {
            val client = HttpClient(MockEngine { respond(ByteReadChannel(ByteArray(5)), headers = if (declared) headersOf(HttpHeaders.ContentLength, "5") else Headers.Empty) })
            try { client.prepareGetForTest { response -> assertFailsWith<IllegalArgumentException> { readLimited(response, 4) } } }
            finally { client.close() }
        }
    }
    @Test fun sessionExpiryIsPreservedEvenWithOversizedErrorBody() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentLength, "99999999")) })
        try { assertFailsWith<SessionExpiredException> { KtorFinanceFiles(client, ApiConfiguration("https://example.invalid/")).export("synthetic", ExportRequest(ExportKind.Full, 10, 2026, ExportFormat.Csv)) } }
        finally { client.close() }
    }
    @Test fun profilePhotoKeepsMultipartFieldAndPatchContract() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Patch, request.method); assertEquals("/api/users/profile/photo", request.url.encodedPath)
            assertEquals("Bearer synthetic", request.headers[HttpHeaders.Authorization]); assertIs<MultiPartFormDataContent>(request.body)
            respond("""{"message":"ok","avatar_url":"/uploads/avatar.jpg"}""")
        })
        try {
            val source = KtorFinanceFiles(client, ApiConfiguration("https://example.invalid/"))
            assertEquals("/uploads/avatar.jpg", source.uploadPhoto("synthetic", ProfilePhoto(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0))).avatar_url)
            assertFailsWith<IllegalArgumentException> { source.uploadPhoto("synthetic", ProfilePhoto(byteArrayOf(1, 2, 3))) }
        } finally { client.close() }
    }
    @Test fun avatarNeverLeaksBearerToDifferentOriginAndRejectsHttp() = runTest {
        val hosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            hosts += request.url.host
            if (request.url.host == "example.invalid") assertEquals("Bearer synthetic", request.headers[HttpHeaders.Authorization])
            else assertNull(request.headers[HttpHeaders.Authorization])
            respond(byteArrayOf(1, 2, 3), headers = headersOf(HttpHeaders.ContentType, "image/jpeg"))
        })
        try {
            val source = KtorFinanceFiles(client, ApiConfiguration("https://example.invalid/"))
            source.avatar("synthetic", "/uploads/a.jpg"); source.avatar("synthetic", "https://cdn.example.invalid/a.jpg")
            assertFailsWith<IllegalArgumentException> { source.avatar("synthetic", "http://example.invalid/a.jpg") }
            assertFailsWith<IllegalArgumentException> { source.avatar("synthetic", "//example.invalid/a.jpg") }
            assertEquals(listOf("example.invalid", "cdn.example.invalid"), hosts)
        } finally { client.close() }
    }
    @Test fun publicCdnUnauthorizedDoesNotExpireAccount() = runTest {
        val client = HttpClient(MockEngine { respond("""{"message":"Foto indisponível"}""", HttpStatusCode.Unauthorized) })
        try { val error = assertFailsWith<ApiResponseException> { KtorFinanceFiles(client, ApiConfiguration("https://example.invalid/")).avatar("synthetic", "https://cdn.example.invalid/a.jpg") }; assertEquals(401, error.statusCode) }
        finally { client.close() }
    }
}

private suspend fun HttpClient.prepareGetForTest(block: suspend (io.ktor.client.statement.HttpResponse) -> Unit) {
    get("https://example.invalid/").let { block(it) }
}
