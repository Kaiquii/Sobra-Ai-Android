package com.example.appfinanceiro.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.test.*

class PatrimonyAmountTest {
    @Test fun acceptsBrazilianAmountsAndZeroWithoutFloatingPointRounding() {
        val cases = mapOf("0" to 0L, "0,00" to 0L, "0,01" to 1L, "1234,56" to 123456L,
            "1.234,56" to 123456L, "R$ 1.234,56" to 123456L, "1.234" to 123400L,
            "0.01" to 1L, "9.999.999.999,99" to 999999999999L)
        cases.forEach { (input, cents) -> assertEquals(PatrimonyAmount(cents), parsePatrimonyAmount(input), input) }
    }
    @Test fun rejectsNegativeNonFiniteScientificAndExcessPrecisionInputs() {
        listOf("", " ", "-1", "-0,01", "NaN", "Infinity", "1e3", "+12", "1,001", "0.001",
            "1,2,3", "1.23,45", "1.000.00", "10.000.000.000,00").forEach {
            assertNull(parsePatrimonyAmount(it), it)
        }
    }
    @Test fun requestAmountsAreJsonNumbersWithExactDecimalsAndNeverExponents() {
        for (cents in listOf(0L, 1L, 100L, 100000000000L, 999999999999L)) {
            val amount = PatrimonyAmount(cents)
            val json = FinanceJson.encodeToString(FgtsWriteRequest(amount))
            val primitive = FinanceJson.parseToJsonElement(json).jsonObject.getValue("amount").jsonPrimitive
            assertFalse(primitive.isString)
            assertEquals(amount.decimal(), primitive.content)
            assertFalse('e' in primitive.content.lowercase())
            assertEquals(amount, FinanceJson.decodeFromString<FgtsWriteRequest>(json).amount)
        }
    }
    @Test fun responseTotalsCanExceedThePerEntryLimitAndStillKeepCents() {
        val amount = FinanceJson.decodeFromString<PatrimonyAmount>("19999999999.98")
        assertEquals(1999999999998L, amount.cents)
        assertEquals("R$ 19.999.999.999,98", amount.formatted())
        assertFailsWith<IllegalArgumentException> { amount.requireEntryLimit() }
        assertNull(PatrimonyAmount.fromDecimal("92233720368547758.08"))
    }
    @Test fun responseDoesNotCoerceMissingNegativeOrQuotedAmountsToZero() {
        listOf("null", "-1", "0.001", "1e3", "\"1.00\"").forEach {
            assertFailsWith<SerializationException>(it) { FinanceJson.decodeFromString<PatrimonyAmount>(it) }
        }
        assertEquals(PatrimonyAmount.Zero, FinanceJson.decodeFromString<PatrimonyAmount>("0"))
    }
    @Test fun investmentValidationTrimsFieldsAndKeepsCategoriesIndependent() {
        val request = investmentRequest("  Reserva  ", "  CDB  ", "0")
        assertEquals(InvestmentWriteRequest("Reserva", "CDB", PatrimonyAmount.Zero), request)
        assertEquals("CDB", request.category)
        assertFailsWith<IllegalArgumentException> { investmentRequest(" ", "CDB", "1") }
        assertFailsWith<IllegalArgumentException> { investmentRequest("x".repeat(121), "CDB", "1") }
        assertFailsWith<IllegalArgumentException> { investmentRequest("Reserva", "x".repeat(81), "1") }
        assertFailsWith<IllegalArgumentException> { investmentRequest("Reserva", "", "1") }
        assertFailsWith<IllegalArgumentException> { investmentRequest("Reserva", "CDB", "-1") }
        assertEquals(120, investmentRequest("x".repeat(120), "CDB", "1").name.length)
    }

    @Test fun investmentIdRequestsKeepExactlyOneCategoryFieldAndLegacyResponsesRemainReadable() {
        val byId = investmentRequest("  Reserva  ", 25, "12,34")
        assertEquals("Reserva", byId.name)
        assertNull(byId.category)
        assertEquals(25, byId.category_id)
        val idBody = FinanceJson.parseToJsonElement(FinanceJson.encodeToString(byId)).jsonObject
        assertEquals(setOf("name", "category_id", "amount"), idBody.keys)
        assertEquals(25, idBody.getValue("category_id").jsonPrimitive.int)
        assertEquals("12.34", idBody.getValue("amount").jsonPrimitive.content)
        val legacyBody = FinanceJson.parseToJsonElement(FinanceJson.encodeToString(investmentRequest("Reserva", "CDI", "0"))).jsonObject
        assertEquals(setOf("name", "category", "amount"), legacyBody.keys)
        assertEquals("CDI", legacyBody.getValue("category").jsonPrimitive.content)
        val legacy = FinanceJson.decodeFromString<Investment>("""{"id":1,"name":"Reserva","category":"CDI","amount":0,"created_at":"2026-10-26T12:00:00Z","updated_at":"2026-10-26T12:00:00Z"}""")
        assertNull(legacy.category_id)
        assertEquals("CDI", legacy.category)
        val current = FinanceJson.decodeFromString<Investment>("""{"id":1,"name":"Reserva","category":"CDB","category_id":25,"amount":0,"created_at":"2026-10-26T12:00:00Z","updated_at":"2026-10-26T12:00:00Z"}""")
        assertEquals(25, current.category_id)
        for (id in listOf(0, -1)) assertFailsWith<IllegalArgumentException> { investmentRequest("Reserva", id, "1") }
    }

    @Test fun investmentCategoryValidationCountsUnicodeCharactersAndTrimsOnlyTheEdges() {
        assertEquals("Reserva de emergência", investmentCategoryRequest("  Reserva de emergência  ").name)
        assertEquals("A  B", investmentCategoryRequest(" A  B ").name)
        val emoji = "\uD83C\uDFE0"
        assertEquals(emoji.repeat(80), investmentCategoryRequest(" " + emoji.repeat(80) + " ").name)
        assertEquals(emoji.repeat(120), investmentRequest(emoji.repeat(120), 25, "0").name)
        for (name in listOf("", "  ", "x".repeat(81), emoji.repeat(81))) {
            assertFailsWith<IllegalArgumentException> { investmentCategoryRequest(name) }
        }
        assertFailsWith<IllegalArgumentException> { investmentRequest(emoji.repeat(121), 25, "0") }
    }
}
