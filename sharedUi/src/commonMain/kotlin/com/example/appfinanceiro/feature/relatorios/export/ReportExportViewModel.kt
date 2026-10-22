package com.example.appfinanceiro.feature.relatorios.export

import com.example.appfinanceiro.core.data.ApiResponseException as ApiRequestException
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportExportViewModel(private val services: br.com.sobraai.ui.runtime.UiServices) : br.com.sobraai.ui.runtime.PresentationModel() {
    private var exportJob: Job? = null

    private val _uiState = MutableStateFlow<ReportExportUiState>(ReportExportUiState.Idle)
    val uiState: StateFlow<ReportExportUiState> = _uiState.asStateFlow()

    fun export(
        token: String,
        request: ReportExportRequest
    ) {
        if (exportJob?.isActive == true) return

        exportJob = viewModelScope.launch {
            _uiState.value = ReportExportUiState.Exporting
            try {
                val savedReport = services.reports.export(token, request)
                if (savedReport == null) { _uiState.value = ReportExportUiState.Idle; return@launch }
                _uiState.value = ReportExportUiState.Success(savedReport)
            } catch (error: CancellationException) {
                _uiState.value = ReportExportUiState.Idle
                throw error
            } catch (_: SessionExpiredException) {
                _uiState.value = ReportExportUiState.SessionExpired
            } catch (error: Throwable) {
                _uiState.value = ReportExportUiState.Error(exportErrorMessage(error))
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        _uiState.value = ReportExportUiState.Idle
    }

    fun clearResult() {
        if (_uiState.value !is ReportExportUiState.Exporting) {
            _uiState.value = ReportExportUiState.Idle
        }
    }

    private fun exportErrorMessage(error: Throwable): String {
        if (error.message?.contains("espaço", ignoreCase = true) == true) {
            return error.message ?: "Não há espaço suficiente para salvar o relatório."
        }
        if (error is ApiRequestException) {
            return when (error.statusCode) {
                400 -> error.apiMessage ?: "Confira as opções escolhidas para o relatório."
                429 -> error.apiMessage ?: "Muitas exportações em pouco tempo. Aguarde e tente novamente."
                500 -> error.apiMessage ?: "O servidor não conseguiu gerar o relatório. Tente novamente."
                else -> error.userMessageOr("Não foi possível gerar o relatório.")
            }
        }
        return error.userMessageOr(
            "Não foi possível salvar o relatório. Verifique o espaço disponível e tente novamente."
        )
    }
}
