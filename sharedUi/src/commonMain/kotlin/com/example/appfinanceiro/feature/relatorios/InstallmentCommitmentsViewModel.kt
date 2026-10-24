package com.example.appfinanceiro.feature.relatorios

import br.com.sobraai.ui.runtime.PresentationModel as ViewModel
import com.example.appfinanceiro.core.data.SharedFinanceRepository
import com.example.appfinanceiro.core.data.ReportsDataSource
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.network.InstallmentCommitmentsResponse
import com.example.appfinanceiro.core.network.InstallmentCommitmentsSummary
import com.example.appfinanceiro.core.network.InstallmentHeavyMonth
import com.example.appfinanceiro.core.network.InstallmentTimelineMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

data class InstallmentCommitmentsUiState(
    val data: InstallmentCommitmentsResponse? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSessionExpired: Boolean = false
)

class InstallmentCommitmentsViewModel(
    private val repository: ReportsDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(InstallmentCommitmentsUiState())
    val uiState: StateFlow<InstallmentCommitmentsUiState> = _uiState

    fun loadCommitments(
        token: String,
        month: Int,
        year: Int,
        months: Int = 12,
        includeCurrentMonthAsPaid: Boolean = false,
        refreshKey: Any? = null
    ) {
        launchLatest("commitments", listOf(token, month, year, months, includeCurrentMonthAsPaid, refreshKey)) {
            _uiState.update {
                it.copy(isLoading = true,
                    errorMessage = if (it.data != null) it.errorMessage else null, isSessionExpired = false)
            }

            try {
                val response = repository.getInstallmentCommitments(
                    token = token,
                    months = months,
                    month = month,
                    year = year,
                    includeCurrentMonthAsPaid = includeCurrentMonthAsPaid
                )
                val visibleResponse = response.onlyCommitmentsActiveFrom(
                    baseMonth = response.mes_base,
                    baseYear = response.ano_base
                )
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(data = visibleResponse, errorMessage = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SessionExpiredException) {
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        errorMessage = e.userMessageOr("Erro ao carregar compromissos parcelados")
                    )
                }
            } finally {
                if (currentCoroutineContext().isActive) {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                }
            }
        }
    }

    fun clearSessionExpired() {
        _uiState.update {
            it.copy(isSessionExpired = false)
        }
    }
}

fun InstallmentCommitmentsResponse.onlyCommitmentsActiveFrom(
    baseMonth: Int,
    baseYear: Int
): InstallmentCommitmentsResponse {
    val baseIndex = baseYear * 12 + (baseMonth - 1)
    val visiblePurchases = compras.filter { purchase ->
        purchase.ultimo_ano * 12 + (purchase.ultimo_mes - 1) >= baseIndex
    }
    val visibleSeries = visiblePurchases.mapTo(hashSetOf()) { it.serie_id }
    val visibleTimeline = linha_do_tempo.map { timelineMonth ->
        val parcels = timelineMonth.parcelas.filter { it.serie_id in visibleSeries }
        timelineMonth.copy(
            parcelas = parcels,
            total = parcels.sumOf { it.valor }
        )
    }
    val heaviestMonth = visibleTimeline
        .filter { it.total > 0.0 }
        .maxByOrNull(InstallmentTimelineMonth::total)
        ?.let { InstallmentHeavyMonth(mes = it.mes, ano = it.ano, total = it.total) }

    return copy(
        compras = visiblePurchases,
        linha_do_tempo = visibleTimeline,
        resumo = InstallmentCommitmentsSummary(
            total_original = visiblePurchases.sumOf { it.total_original },
            total_pago = visiblePurchases.sumOf { it.total_pago },
            total_restante = visiblePurchases.sumOf { it.total_restante },
            parcelas_pagas = visiblePurchases.sumOf { it.parcelas_pagas },
            parcelas_restantes = visiblePurchases.sumOf { it.parcelas_restantes },
            total_compras = visiblePurchases.size,
            mes_mais_pesado = heaviestMonth
        )
    )
}
