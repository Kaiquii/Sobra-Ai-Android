package br.com.sobraai.ui

import com.example.appfinanceiro.core.data.HomeDataSource
import com.example.appfinanceiro.core.network.Income
import com.example.appfinanceiro.core.network.IncomesResponse
import com.example.appfinanceiro.core.network.SummaryResponse
import com.example.appfinanceiro.feature.home.HomeViewModel
import com.example.appfinanceiro.feature.home.components.availableBalanceSubtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AvailableBalancePresentationTest {
    @Test fun subtitleIdentifiesEachExclusionWithoutChangingTheBalance() {
        val labels = listOf(
            Triple(false, false, "Saldo livre no mês"),
            Triple(true, false, "Saldo livre no mês · sem VA"),
            Triple(false, true, "Saldo livre no mês · sem VR"),
            Triple(true, true, "Saldo livre no mês · sem VA e VR")
        )
        labels.forEach { (va, vr, label) ->
            val data = summary(11, 2026, va, vr, 2000.0)
            assertEquals(label, availableBalanceSubtitle(data))
            assertEquals(2000.0, data.total_geral_disponivel)
        }
        assertEquals("Saldo livre no mês", availableBalanceSubtitle(null))
    }

    @Test fun negativeWarningAndZeroArePreservedWithExclusions() {
        assertEquals("Atenção: saldo negativo no mês · sem VA e VR",
            availableBalanceSubtitle(summary(11, 2026, true, true, -50.0)))
        assertEquals("Atenção: saldo negativo no mês",
            availableBalanceSubtitle(summary(11, 2026, false, false, -50.0)))
        assertEquals("Saldo livre no mês · sem VR",
            availableBalanceSubtitle(summary(11, 2026, false, true, 0.0)))
    }

    @Test fun homeUsesServerBalanceForEveryCombinationAfterIncomeAndMonthReloads() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val base = EditingFake()
        var response = summary(12, 2026, false, false, 2625.0)
        val source = object : HomeDataSource by base {
            override suspend fun getSummary(token: String, month: Int, year: Int) = response.copy(month = month, year = year)
            override suspend fun getIncomes(token: String, month: Int?, year: Int?) = IncomesResponse(listOf(
                Income(1, "Salario", 2000.0, month!!, year!!),
                Income(2, "VA", 725.0, month, year),
                Income(3, "VR", 600.0, month, year)
            ), 3)
        }
        val model = HomeViewModel(source)
        try {
            val combinations = listOf(
                Triple(false, false, 2625.0), Triple(true, false, 2500.0),
                Triple(false, true, 2125.0), Triple(true, true, 2000.0)
            )
            combinations.forEachIndexed { revision, (va, vr, balance) ->
                response = summary(12, 2026, va, vr, balance)
                model.loadAll("synthetic-token", 12, 2026, refreshKey = revision, reuseRecent = true)
                runCurrent()
                val actual = assertNotNull(model.uiState.value.summaryData)
                assertEquals(balance, actual.total_geral_disponivel)
                assertEquals(va, actual.exclude_va_from_available)
                assertEquals(vr, actual.exclude_vr_from_available)
                assertEquals(3325.0, actual.total_income)
                assertEquals(700.0, actual.total_expense)
                assertEquals(725.0, actual.va)
                assertEquals(600.0, actual.vr)
                assertEquals(125.0, actual.restante_va)
                assertEquals(500.0, actual.restante_vr)
            }
            // A spent benefit and an overdrawn benefit must not be subtracted locally again.
            response = summary(1, 2027, true, true, 2000.0).copy(
                restante_va = 0.0, restante_vr = -50.0, total_expense = 1375.0)
            model.loadAll("synthetic-token", 1, 2027, refreshKey = 5, reuseRecent = true)
            runCurrent()
            val january = assertNotNull(model.uiState.value.summaryData)
            assertEquals(1, january.month)
            assertEquals(2027, january.year)
            assertEquals(2000.0, january.total_geral_disponivel)
            assertEquals(0.0, january.restante_va)
            assertEquals(-50.0, january.restante_vr)
            assertEquals(true, january.exclude_va_from_available)
            assertEquals(true, january.exclude_vr_from_available)
        } finally {
            model.close()
            Dispatchers.resetMain()
        }
    }

    private fun summary(month: Int, year: Int, va: Boolean, vr: Boolean, balance: Double) = SummaryResponse(
        month = month, year = year, salario = 2000.0, adiantamento = 0.0, renda_extra_amt = 0.0,
        restante_salario = 2000.0, restante_adiantamento = 0.0, restante_renda_extra = 0.0,
        total_gasto_salario = 0.0, total_gasto_adiantamento = 0.0, total_gasto_renda_extra = 0.0,
        total_expense = 700.0, total_geral_disponivel = balance, total_income = 3325.0,
        va = 725.0, vr = 600.0, total_gasto_va = 600.0, total_gasto_vr = 100.0,
        restante_va = 125.0, restante_vr = 500.0,
        exclude_va_from_available = va, exclude_vr_from_available = vr
    )
}
