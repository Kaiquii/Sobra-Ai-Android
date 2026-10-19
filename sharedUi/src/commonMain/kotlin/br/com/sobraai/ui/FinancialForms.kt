package br.com.sobraai.ui

import com.example.appfinanceiro.core.network.*
import com.example.appfinanceiro.feature.despesas.validateExpenseForm
import kotlin.math.abs

val PAYMENT_SOURCES = listOf("Salário", "Adiantamento", "Renda Extra")
val EXPENSE_TYPES = listOf("Única", "Fixa", "Parcelada")
fun sourceKey(value: String): String = value.lowercase().replace("á", "a")
fun sourceLabel(value: String): String = if (sourceKey(value) == "salario") "Salário" else value
fun amountInput(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val raw = abs(value).toString().lowercase()
    if ('e' !in raw) return sign + raw.replace('.', ',')
    val (mantissa, exponent) = raw.split('e')
    val digits = mantissa.replace(".", "")
    val point = mantissa.substringBefore('.').length + exponent.toInt()
    return sign + when {
        point <= 0 -> "0," + "0".repeat(-point) + digits
        point >= digits.length -> digits + "0".repeat(point - digits.length)
        else -> digits.take(point) + "," + digits.drop(point)
    }
}

/** Decimal input only; a negative sign is an explicit opt-in for legacy expense edits. */
fun parseAmount(value: String, allowZero: Boolean = false, allowNegative: Boolean = false): Double? {
    val sign = if (allowNegative) "-?" else ""
    if (!Regex("$sign([0-9]+([,.][0-9]*)?|[,.][0-9]+)").matches(value.trim())) return null
    return value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && (it > 0 || allowZero && it == 0.0 || allowNegative && it < 0) }
}

data class SplitDraft(val source: String, val amount: String)
data class ExpenseDraft(
    val amount: String = "", val description: String = "", val notes: String = "",
    val categoryId: Int? = null, val source: String = "Salário",
    val split: Boolean = false, val splits: List<SplitDraft> = emptyList(),
    val date: String = today().display, val type: String = "Única", val installments: String = "1",
    val updateFuture: Boolean = false
) {
    fun validate(editing: Boolean = false): String? {
        validateExpenseForm(amount, description.trim(), categoryId, notes, "Preencha valor, descrição e categoria.")?.let { return it }
        val total = parseAmount(amount, allowZero = true, allowNegative = editing) ?: return "Informe um valor válido${if (editing) "." else ", maior ou igual a zero."}"
        if (FinancialDate.parseDisplay(date) == null) return "Informe uma data válida em DD/MM/AAAA."
        if (!editing && type !in EXPENSE_TYPES) return "Selecione um tipo de despesa válido."
        if (!editing && type == "Parcelada" && (installments.toIntOrNull() ?: 0) < 1) return "Informe pelo menos uma parcela."
        if (!split && sourceKey(source) !in PAYMENT_SOURCES.map(::sourceKey)) return "Selecione uma origem de pagamento."
        if (split) {
            if (splits.isEmpty()) return "Adicione pelo menos uma origem de pagamento."
            if (splits.map { sourceKey(it.source) }.distinct().size != splits.size) return "Não repita uma origem de pagamento."
            if (splits.any { sourceKey(it.source) !in PAYMENT_SOURCES.map(::sourceKey) }) return "Selecione uma origem de pagamento válida."
            if (splits.any { parseAmount(it.amount) == null }) return "Cada valor da divisão deve ser maior que zero."
            if (abs(splits.sumOf { parseAmount(it.amount)!! } - total) > .009) return "Distribua todo o valor da despesa entre as origens."
        }
        return null
    }

    fun createRequest(): ExpenseRequest {
        require(validate() == null)
        return ExpenseRequest(parseAmount(amount, allowZero = true)!!, description, categoryId!!,
            if (split) null else source, if (split) splits.map { PaymentSplitRequest(it.source, parseAmount(it.amount)!!) } else null,
            apiDate(FinancialDate.parseDisplay(date)!!), type, if (type == "Parcelada") installments.toInt() else 1, notes.trim())
    }

    fun updateRequest(original: Expense): ExpenseUpdateRequest {
        require(validate(editing = true) == null)
        return ExpenseUpdateRequest(amount = parseAmount(amount, allowZero = true, allowNegative = true)!!, description = description,
            category_id = categoryId, payment_source = if (split) null else source,
            payment_splits = if (split) splits.map { PaymentSplitRequest(it.source, parseAmount(it.amount)!!) } else null,
            date = apiDate(FinancialDate.parseDisplay(date)!!), notes = notes.trim(),
            update_future = if (original.isRecurring) updateFuture else null)
    }

    companion object {
        fun from(expense: Expense): ExpenseDraft = ExpenseDraft(amount = amountInput(expense.amount), description = expense.description,
            notes = expense.notes.orEmpty(), categoryId = expense.category_id,
            source = expense.payment_splits.singleOrNull()?.payment_source ?: expense.payment_source.orEmpty(),
            split = expense.payment_splits.size > 1, splits = expense.payment_splits.map { SplitDraft(it.payment_source, amountInput(it.amount)) },
            // Never silently replace an unreadable legacy date with today.
            date = FinancialDate.parseApi(expense.date)?.display.orEmpty(), type = if (expense.type.equals("Unica", true)) "Única" else expense.type,
            installments = (expense.installments ?: 1).toString())
    }
}

val Expense.isRecurring: Boolean get() = type.equals("Fixa", true) || type.equals("Parcelada", true)

data class IncomeDraft(val source: String, val period: Period, val existing: Income? = null,
    val amount: String = existing?.amount?.let(::amountInput).orEmpty(),
    val updateFuture: Boolean = true, val repeatFuture: Boolean = false, val deleteFuture: Boolean = false) {
    fun createRequest(): IncomeRequest = IncomeRequest(source, requireNotNull(parseAmount(amount)), period.month, period.year,
        "Fixa", if (sourceKey(source) == "renda extra") repeatFuture else null)
    fun updateRequest() = IncomeUpdateRequest(requireNotNull(parseAmount(amount)), updateFuture)
}

data class FinancialDate(val year: Int, val month: Int, val day: Int) : Comparable<FinancialDate> {
    init { require(year in 1..9999 && month in 1..12 && day in 1..daysInMonth(year, month)) }
    val iso get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    val display get() = "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/${year.toString().padStart(4, '0')}"
    override fun compareTo(other: FinancialDate): Int = iso.compareTo(other.iso)
    val utcMillis: Long get() {
        val adjustedYear = year - if (month <= 2) 1 else 0
        val era = adjustedYear.floorDiv(400)
        val yoe = adjustedYear - era * 400
        val doy = (153 * (month + if (month > 2) -3 else 9) + 2) / 5 + day - 1
        return (era * 146097L + yoe * 365 + yoe / 4 - yoe / 100 + doy - 719468) * 86_400_000L
    }
    companion object {
        fun parseApi(value: String): FinancialDate? = parse(value.take(10).split('-'), false)
        fun parseDisplay(value: String): FinancialDate? = parse(value.split('/'), true)
        private fun parse(parts: List<String>, reversed: Boolean): FinancialDate? = runCatching {
            require(parts.size == 3)
            FinancialDate(parts[if (reversed) 2 else 0].toInt(), parts[1].toInt(), parts[if (reversed) 0 else 2].toInt())
        }.getOrNull()
        fun fromUtcMillis(value: Long): FinancialDate {
            val z = value.floorDiv(86_400_000L) + 719468
            val era = z.floorDiv(146097)
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            var year = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val day = doy - (153 * mp + 2) / 5 + 1
            val month = mp + if (mp < 10) 3 else -9
            year += if (month <= 2) 1 else 0
            return FinancialDate(year.toInt(), month.toInt(), day.toInt())
        }
    }
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

expect fun today(): FinancialDate
expect fun apiDate(date: FinancialDate): String
