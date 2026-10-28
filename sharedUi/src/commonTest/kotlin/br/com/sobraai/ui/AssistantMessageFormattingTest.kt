package br.com.sobraai.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import com.example.appfinanceiro.core.network.AssistantStoredMessage
import com.example.appfinanceiro.feature.assistant.AssistantMessage
import com.example.appfinanceiro.feature.assistant.assistantMessageText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTextApi::class)
class AssistantMessageFormattingTest {
    private fun assistant(content: String) = AssistantMessage("assistant", content)

    private fun boldText(text: AnnotatedString): List<String> = text.spanStyles
        .filter { it.item.fontWeight == FontWeight.Bold }
        .map { text.text.substring(it.start, it.end) }

    @Test
    fun balancedEmphasisFormatsOnlyTheMarkedTextAndKeepsOriginalContent() {
        val source = "Seu **saldo disponível** é **R$ 1.234,56**."
        val message = assistant(source)

        val rendered = assistantMessageText(message)

        assertEquals("Seu saldo disponível é R$ 1.234,56.", rendered.text)
        assertEquals(listOf("saldo disponível", "R$ 1.234,56"), boldText(rendered))
        assertEquals(source, message.content)
    }

    @Test
    fun unmatchedMarkersAndLiteralAsterisksRemainReadable() {
        for (source in listOf("**Sem fechamento", "Sem abertura**", "2 * 3 = 6", "Asterisco * literal",
            "****", "***Não suportado***", "** espaço **", "\\**marcador literal**")) {
            val rendered = assistantMessageText(assistant(source))
            assertEquals(source, rendered.text, source)
            assertTrue(boldText(rendered).isEmpty(), source)
        }
    }

    @Test
    fun unicodeAndEmojisDoNotShiftEmphasisOrChangePortuguese() {
        val source = "💡 **Atenção às parcelas**: você tem **R$ -25,40** em ajustes. ✅"
        val rendered = assistantMessageText(assistant(source))

        assertEquals("💡 Atenção às parcelas: você tem R$ -25,40 em ajustes. ✅", rendered.text)
        assertEquals(listOf("Atenção às parcelas", "R$ -25,40"), boldText(rendered))
    }

    @Test
    fun existingParagraphsAndExplicitLineBreaksArePreserved() {
        val source = "Resumo do mês\n\nReceitas recebidas.\nConfira as despesas.\n\nPosso ajudar a organizar."
        assertEquals(source, assistantMessageText(assistant(source)).text)
    }

    @Test
    fun numberedAdviceKeepsReadingOrderAndIndentsWrappedItems() {
        val items = listOf("Usar transporte público", "Caminhar", "Compartilhar viagens",
            "Evitar horários de pico", "Planejar o trajeto", "Combinar compromissos")
        val source = "Algumas opções:\n\n" + items.mapIndexed { index, item ->
            "${index + 1}. **$item**: considere o que funciona na sua rotina."
        }.joinToString("\n") + "\n\nVocê decide o que faz sentido."
        val rendered = assistantMessageText(assistant(source))

        assertEquals(items, boldText(rendered))
        var previous = -1
        for ((index, item) in items.withIndex()) {
            val position = rendered.text.indexOf("${index + 1}. $item")
            assertTrue(position > previous, "A lista deve manter a ordem dos conselhos")
            assertTrue(rendered.paragraphStyles.any { range ->
                val indent = range.item.textIndent
                range.start <= position && range.end > position && indent != null &&
                    indent.restLine.value > indent.firstLine.value
            }, "As linhas de continuação de cada item devem ter recuo")
            previous = position
        }
        assertTrue(rendered.text.startsWith("Algumas opções:"))
        assertTrue(rendered.text.endsWith("Você decide o que faz sentido."))
    }

    @Test
    fun simpleBulletsKeepTheirMarkersAndIndentContinuationLines() {
        val source = "- **Guardar recibos** para consultar depois.\n* **Conferir parcelas** antes de outra compra."
        val rendered = assistantMessageText(assistant(source))

        assertEquals("- Guardar recibos para consultar depois.\n* Conferir parcelas antes de outra compra.", rendered.text)
        assertEquals(listOf("Guardar recibos", "Conferir parcelas"), boldText(rendered))
        for (marker in listOf("- Guardar recibos", "* Conferir parcelas")) {
            val position = rendered.text.indexOf(marker)
            assertTrue(rendered.paragraphStyles.any { range ->
                val indent = range.item.textIndent
                range.start <= position && range.end > position && indent != null &&
                    indent.restLine.value > indent.firstLine.value
            })
        }
    }

    @Test
    fun lineEndingNormalizationDoesNotRewritePersistedContent() {
        val source = "**Resumo**\r\n\r\nReceitas.\rDespesas."
        val message = assistant(source)

        assertEquals("Resumo\n\nReceitas.\nDespesas.", assistantMessageText(message).text)
        assertEquals(source, message.content)
    }

    @Test
    fun formattingCannotRemoveNegativeSignsOrInventFinancialValues() {
        val source = "Saldo de 2026, registro 154:\n\n- R$ 1.234,56\n-1.234,56\n-R$ 20,00\n**- R$ 42,50**"
        val message = assistant(source)
        val rendered = assistantMessageText(message)

        for (value in listOf("2026", "154", "- R$ 1.234,56", "-1.234,56", "-R$ 20,00", "- R$ 42,50")) {
            assertTrue(rendered.text.contains(value), "O valor/sinal original deve permanecer: $value")
        }
        assertEquals(listOf("- R$ 42,50"), boldText(rendered))
        assertEquals(source, message.content)
    }

    @Test
    fun userMessagesAndAssistantWarningsStayLiteral() {
        val source = "**Texto digitado**\n- item\n1. mensagem"
        val messages = listOf(AssistantMessage("user", source), AssistantMessage("system", source)) +
            listOf("assistant_quota_exceeded", "assistant_user_limit", "assistant_unavailable", "gemini_quota_exceeded")
                .map { AssistantMessage("assistant", source, errorCode = it, retryAfterSeconds = 30) }

        for (message in messages) {
            assertEquals(AnnotatedString(source), assistantMessageText(message))
            assertEquals(source, message.content)
        }
    }

    @Test
    fun htmlImagesAndLinksRemainInertText() {
        val source = "<b>Texto</b> <img src=\"https://example.invalid/a.png\"> " +
            "[Abrir site](https://example.invalid/) javascript:alert(1)"
        val rendered = assistantMessageText(assistant(source))

        assertEquals(source, rendered.text)
        assertTrue(rendered.getLinkAnnotations(0, rendered.length).isEmpty())
        assertTrue(rendered.getUrlAnnotations(0, rendered.length).isEmpty())
        assertTrue(rendered.getStringAnnotations(0, rendered.length).isEmpty())
    }

    @Test
    fun storedAndNewAssistantMessagesUseTheSameFormattingWithoutRewritingHistory() {
        val source = "**Resumo**\n\nSeu saldo é R$ 25,00. 💡"
        val stored = AssistantStoredMessage(1, 42, "assistant", source, "Agora")
        val historyMessage = AssistantMessage(stored.role, stored.content, displayTime = stored.display_time)
        val freshMessage = assistant(source)

        assertEquals(assistantMessageText(freshMessage), assistantMessageText(historyMessage))
        assertEquals(source, stored.content)
        assertEquals(source, historyMessage.content)
        assertEquals(source, freshMessage.content)
    }

    @Test
    fun emptyReplyRemainsEmpty() {
        assertEquals(AnnotatedString(""), assistantMessageText(assistant("")))
    }
}
