package com.example.appfinanceiro.feature.relatorios.export

import com.example.appfinanceiro.core.data.ApiRequestException
import com.example.appfinanceiro.core.data.NetworkRequestException
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import com.example.appfinanceiro.core.network.parseApiErrorMessage
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.ResponseBody

data class ReportDownload(
    val body: ResponseBody,
    val fileName: String,
    val mimeType: String
)

class ReportExportRepository {
    suspend fun download(token: String, request: ReportExportRequest): ReportDownload {
        try {
            val response = RetrofitClient.financeApi.exportReport(
                token = "Bearer $token",
                type = request.type.apiValue,
                month = request.month,
                year = request.year,
                format = request.format.apiValue,
                compareMonth = request.compareMonth,
                compareYear = request.compareYear,
                months = request.months,
                includeCurrentMonthAsPaid = request.includeCurrentMonthAsPaid
            )

            if (!response.isSuccessful) {
                val errorMessage = response.errorBody()?.use { errorBody ->
                    parseApiErrorMessage(errorBody.string())
                }

                if (response.code() == 401) throw SessionExpiredException()
                throw ApiRequestException(response.code(), errorMessage)
            }

            val body = response.body()
                ?: throw IOException("O servidor retornou um arquivo vazio.")
            val mimeType = response.headers()["Content-Type"]
                ?.substringBefore(';')
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: request.format.mimeType
            val fileName = extractSafeFileName(
                contentDisposition = response.headers()["Content-Disposition"],
                fallback = fallbackFileName(request)
            )

            return ReportDownload(body, fileName, mimeType)
        } catch (error: SessionExpiredException) {
            throw error
        } catch (error: ApiRequestException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw NetworkRequestException(
                userMessage = "A geração demorou mais que o esperado. Tente novamente.",
                cause = error
            )
        } catch (error: IOException) {
            throw NetworkRequestException(
                userMessage = "Não foi possível baixar o relatório. Verifique sua internet e o espaço disponível.",
                cause = error
            )
        }
    }
}

internal fun extractSafeFileName(contentDisposition: String?, fallback: String): String {
    val headerName = contentDisposition
        ?.substringAfter("filename=", missingDelimiterValue = "")
        ?.substringBefore(';')
        ?.trim()
        ?.trim('"', '\'')
        ?.takeIf { it.isNotBlank() }

    val candidate = headerName ?: fallback
    return candidate
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[\\x00-\\x1F<>:\"/\\\\|?*]"), "_")
        .trim()
        .trim('.')
        .take(120)
        .ifBlank { fallback }
}
