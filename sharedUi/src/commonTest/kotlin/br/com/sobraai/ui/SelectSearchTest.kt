package br.com.sobraai.ui

import com.example.appfinanceiro.core.designsystem.components.AppSelectOption
import com.example.appfinanceiro.core.designsystem.components.filterSelectOptions
import kotlin.test.*

class SelectSearchTest {
    private val options = listOf(
        AppSelectOption<Int?>(null, "Todas"),
        AppSelectOption<Int?>(10, "Saúde"),
        AppSelectOption<Int?>(20, "Vale-refeição (VR)"),
        AppSelectOption<Int?>(30, "Vale-alimentação (VA)")
    )

    @Test fun searchIgnoresAccentsCaseAndExtraWhitespace() {
        assertEquals(listOf(10), filterSelectOptions(options, " SAUDE ").map { it.value })
        assertEquals(listOf(20), filterSelectOptions(options, "REFEICAO vr").map { it.value })
        assertEquals(listOf(30), filterSelectOptions(options, "  VALE   alimentacao  ").map { it.value })
    }

    @Test fun clearingSearchRestoresAllOptionsIncludingAllFilter() {
        assertTrue(filterSelectOptions(options, "inexistente").isEmpty())
        assertEquals(options, filterSelectOptions(options, "   "))
        assertEquals(listOf(null), filterSelectOptions(options, "todas").map { it.value })
    }

    @Test fun filteringPreservesIdsOrderAndDuplicateLabels() {
        val duplicates = listOf(AppSelectOption(9, "Mercado"), AppSelectOption(3, "Mercado"), AppSelectOption(5, "Casa"))
        val result = filterSelectOptions(duplicates, "mercado")
        assertEquals(listOf(9, 3), result.map { it.value })
        assertSame(duplicates.first(), result.first())
        assertEquals(3, duplicates.size)
    }
}
