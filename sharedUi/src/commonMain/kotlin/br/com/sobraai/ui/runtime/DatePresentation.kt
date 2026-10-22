package br.com.sobraai.ui.runtime

import kotlin.time.Clock

// Native date parsing/time zones are adapters; the forms and their decisions are shared.
data class Locale(val language: String, val country: String = "") {
    companion object {
        val ROOT = Locale("en", "US")
        fun getDefault() = Locale("pt", "BR")
        fun forLanguageTag(tag: String) = Locale(tag.substringBefore('-'), tag.substringAfter('-', ""))
    }
}
data class TimeZone(val id: String?) {
    companion object { fun getTimeZone(id: String) = TimeZone(id) }
}
data class Date(val time: Long)
class Calendar private constructor(private val zone: TimeZone) {
    var timeInMillis: Long = Clock.System.now().toEpochMilliseconds()
    var time: Date
        get() = Date(timeInMillis)
        set(value) { timeInMillis = value.time }
    fun get(field: Int): Int = dateFields(timeInMillis, zone.id)[index(field)]
    fun clear() { timeInMillis = dateMillis(intArrayOf(1970, 0, 1, 0, 0, 0, 0), zone.id) }
    fun set(year: Int, month: Int, day: Int) {
        val fields = dateFields(timeInMillis, zone.id)
        fields[0] = year; fields[1] = month; fields[2] = day
        timeInMillis = dateMillis(fields, zone.id)
    }
    fun set(field: Int, value: Int) {
        val fields = dateFields(timeInMillis, zone.id); fields[index(field)] = value
        timeInMillis = dateMillis(fields, zone.id)
    }
    fun add(field: Int, amount: Int) = set(field, get(field) + amount)
    companion object {
        const val YEAR = 1; const val MONTH = 2; const val DAY_OF_MONTH = 5
        const val HOUR_OF_DAY = 11; const val MINUTE = 12; const val SECOND = 13; const val MILLISECOND = 14
        fun getInstance(zone: TimeZone = TimeZone(null)) = Calendar(zone)
        private fun index(field: Int) = when (field) {
            YEAR -> 0; MONTH -> 1; DAY_OF_MONTH -> 2; HOUR_OF_DAY -> 3; MINUTE -> 4; SECOND -> 5; MILLISECOND -> 6
            else -> error("Campo de data inválido: $field")
        }
    }
}
class SimpleDateFormat(private val pattern: String, private val locale: Locale) {
    var timeZone = TimeZone(null)
    var isLenient = true
    fun format(date: Date): String = format(date.time)
    fun format(millis: Long): String = formatDatePattern(millis, pattern, timeZone.id)
    fun parse(text: String): Date? = parseDatePattern(text, pattern, timeZone.id, isLenient)?.let(::Date)
}
class NumberFormat private constructor() {
    fun format(value: Double): String = com.example.appfinanceiro.feature.relatorios.utils.formatCurrency(value)
    companion object { fun getCurrencyInstance(locale: Locale) = NumberFormat() }
}
class DateFormatSymbols(locale: Locale) {
    val months = arrayOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
}
internal expect fun dateFields(millis: Long, zone: String?): IntArray
internal expect fun dateMillis(fields: IntArray, zone: String?): Long
internal expect fun formatDatePattern(millis: Long, pattern: String, zone: String?): String
internal expect fun parseDatePattern(text: String, pattern: String, zone: String?, lenient: Boolean): Long?
