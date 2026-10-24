package com.example.appfinanceiro.core.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDataErrorBannerTest {

    @Test
    fun `informa quando dados anteriores continuam visiveis sem conexao`() {
        val message = dataRequestErrorMessage(
            errorMessage = "Não foi possível conectar ao servidor. Verifique sua internet.",
            showingPreviousData = true
        )

        assertEquals(
            "Falha de conexão com o servidor. Exibindo dados salvos.",
            message
        )
    }

    @Test
    fun `na primeira carga informa que os dados nao foram carregados`() {
        val message = dataRequestErrorMessage(
            errorMessage = "Não foi possível conectar ao servidor. Verifique sua internet.",
            showingPreviousData = false,
            dataLabel = "as despesas"
        )

        assertEquals(
            "Falha de conexão com o servidor. Não foi possível carregar as despesas.",
            message
        )
    }

    @Test
    fun `preserva erro da api quando nao ha dados anteriores`() {
        val message = dataRequestErrorMessage(
            errorMessage = "Serviço temporariamente indisponível.",
            showingPreviousData = false
        )

        assertEquals("Serviço temporariamente indisponível.", message)
    }
}
