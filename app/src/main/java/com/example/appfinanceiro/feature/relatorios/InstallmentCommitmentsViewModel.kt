package com.example.appfinanceiro.feature.relatorios

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfinanceiro.core.data.FinanceRepository
import com.example.appfinanceiro.core.data.SessionExpiredException
import com.example.appfinanceiro.core.network.InstallmentCommitmentsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.util.Calendar

data class InstallmentCommitmentsUiState(
    val data: InstallmentCommitmentsResponse? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSessionExpired: Boolean = false
)

class InstallmentCommitmentsViewModel(
    private val repository: FinanceRepository = FinanceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstallmentCommitmentsUiState())
    val uiState: StateFlow<InstallmentCommitmentsUiState> = _uiState

    fun loadCommitments(
        token: String,
        months: Int = 12,
        includeCurrentMonthAsPaid: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, isSessionExpired = false)
            }

            try {
                val currentDate = Calendar.getInstance()
                val currentMonth = currentDate.get(Calendar.MONTH) + 1
                val currentYear = currentDate.get(Calendar.YEAR)

                val response = repository.getInstallmentCommitments(
                    token = token,
                    months = months,
                    month = currentMonth,
                    year = currentYear,
                    includeCurrentMonthAsPaid = includeCurrentMonthAsPaid
                )
                _uiState.update {
                    it.copy(data = response)
                }
            } catch (e: SessionExpiredException) {
                _uiState.update {
                    it.copy(isSessionExpired = true)
                }
            } catch (e: SocketTimeoutException) {
                Log.e("API_ERRO", "Timeout ao carregar compromissos parcelados", e)
                _uiState.update {
                    it.copy(
                        data = null,
                        errorMessage = "O servidor demorou para responder. Tente novamente em alguns instantes."
                    )
                }
            } catch (e: Exception) {
                Log.e("API_ERRO", "Falha ao carregar compromissos parcelados", e)
                _uiState.update {
                    it.copy(
                        data = null,
                        errorMessage = "Erro ao carregar compromissos parcelados"
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(isLoading = false)
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
