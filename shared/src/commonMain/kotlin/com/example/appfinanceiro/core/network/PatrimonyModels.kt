package com.example.appfinanceiro.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

/** Exact cents, independent of the monthly finance DTOs and their Double amounts. */
@Serializable(with = PatrimonyAmountSerializer::class)
data class PatrimonyAmount(val cents: Long) {
    init { require(cents >= 0) }
    fun decimal(): String = (cents / 100).toString() + "." + (cents % 100).toString().padStart(2, '0')
    fun input(): String = decimal().replace('.', ',')
    fun formatted(): String {
        val units = (cents / 100).toString().reversed().chunked(3).joinToString(".").reversed()
        return "R$ " + units + "," + (cents % 100).toString().padStart(2, '0')
    }
    fun requireEntryLimit(): PatrimonyAmount {
        require(cents <= MAX_ENTRY_CENTS) { "O saldo deve ser de até R$ 9.999.999.999,99." }
        return this
    }
    companion object {
        const val MAX_ENTRY_CENTS = 999_999_999_999L
        val Zero = PatrimonyAmount(0)
        fun fromDecimal(text: String): PatrimonyAmount? {
            if (!Regex("[0-9]+(\\.[0-9]{1,2})?").matches(text)) return null
            val units = text.substringBefore('.').toLongOrNull() ?: return null
            val fraction = if ('.' in text) text.substringAfter('.').padEnd(2, '0').toLong() else 0L
            if (units > (Long.MAX_VALUE - fraction) / 100) return null
            return PatrimonyAmount(units * 100 + fraction)
        }
    }
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object PatrimonyAmountSerializer : KSerializer<PatrimonyAmount> {
    override val descriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: PatrimonyAmount) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("Saldo exige JSON.")
        // The literal is generated only from validated Long cents, with no user-supplied JSON.
        json.encodeJsonElement(JsonUnquotedLiteral(value.decimal()))
    }
    override fun deserialize(decoder: Decoder): PatrimonyAmount {
        val element = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive
        if (element.isString) throw SerializationException("Saldo deve ser um número JSON.")
        return PatrimonyAmount.fromDecimal(element.content) ?: throw SerializationException("Saldo inválido.")
    }
}

fun parsePatrimonyAmount(input: String): PatrimonyAmount? {
    val value = input.trim().removePrefix("R$").trim()
    val normalized = when {
        ',' in value -> {
            if (value.count { it == ',' } != 1) return null
            val units = value.substringBefore(',')
            if (!Regex("[0-9]+|[1-9][0-9]{0,2}(\\.[0-9]{3})+").matches(units)) return null
            units.replace(".", "") + "." + value.substringAfter(',')
        }
        Regex("[1-9][0-9]{0,2}(\\.[0-9]{3})+").matches(value) -> value.replace(".", "")
        else -> value
    }
    return PatrimonyAmount.fromDecimal(normalized)?.takeIf { it.cents <= PatrimonyAmount.MAX_ENTRY_CENTS }
}

@Serializable
data class FgtsBalance(val amount: PatrimonyAmount, val updated_at: String)
@Serializable
data class Investment(val id: Int, val name: String, val category: String, val amount: PatrimonyAmount,
    val created_at: String, val updated_at: String, val category_id: Int? = null)
@Serializable
data class PatrimonyResponse(val fgts: FgtsBalance?, val investments: List<Investment>,
    val total_fgts: PatrimonyAmount, val total_investments: PatrimonyAmount, val total_patrimony: PatrimonyAmount)
@Serializable
data class FgtsWriteRequest(val amount: PatrimonyAmount)
@Serializable
data class InvestmentWriteRequest(val name: String, val category: String? = null, val amount: PatrimonyAmount,
    val category_id: Int? = null)
@Serializable
data class FgtsWriteResponse(val data: FgtsBalance)
@Serializable
data class InvestmentWriteResponse(val data: Investment)

@Serializable
data class InvestmentCategory(val id: Int, val name: String, val is_default: Boolean)
@Serializable
data class InvestmentCategoriesResponse(val total: Int, val categories: List<InvestmentCategory>)
@Serializable
data class InvestmentCategoryWriteRequest(val name: String)
@Serializable
data class InvestmentCategoryWriteResponse(val data: InvestmentCategory)

private fun validatedPatrimonyText(value: String, limit: Int, message: String): String {
    val clean = value.trim()
    val characters = clean.indices.count { i ->
        !clean[i].isLowSurrogate() || i == 0 || !clean[i - 1].isHighSurrogate()
    }
    require(characters in 1..limit) { message }
    return clean
}

fun investmentCategoryRequest(name: String): InvestmentCategoryWriteRequest = InvestmentCategoryWriteRequest(
    validatedPatrimonyText(name, 80, "Informe uma categoria com até 80 caracteres."))

private fun validatedInvestmentName(name: String): String =
    validatedPatrimonyText(name, 120, "Informe um nome com até 120 caracteres.")

private fun validatedInvestmentAmount(amount: String): PatrimonyAmount = requireNotNull(parsePatrimonyAmount(amount)) {
    "Informe um saldo válido, de zero a R$ 9.999.999.999,99, com até duas casas decimais."
}

fun investmentRequest(name: String, category: String, amount: String): InvestmentWriteRequest {
    val cleanName = validatedInvestmentName(name)
    val cleanCategory = investmentCategoryRequest(category).name
    return InvestmentWriteRequest(cleanName, cleanCategory, validatedInvestmentAmount(amount))
}

fun investmentRequest(name: String, categoryId: Int, amount: String): InvestmentWriteRequest {
    val cleanName = validatedInvestmentName(name)
    require(categoryId > 0) { "Selecione uma categoria válida." }
    return InvestmentWriteRequest(cleanName, amount = validatedInvestmentAmount(amount), category_id = categoryId)
}
