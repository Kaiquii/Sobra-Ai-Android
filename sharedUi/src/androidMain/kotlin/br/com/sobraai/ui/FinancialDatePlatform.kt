package br.com.sobraai.ui

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

actual fun today(): FinancialDate = Calendar.getInstance().let { FinancialDate(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH)) }
actual fun apiDate(date: FinancialDate): String {
    val calendar = Calendar.getInstance().apply { clear(); set(date.year, date.month - 1, date.day, 0, 0, 0) }
    return SimpleDateFormat("yyyy-MM-dd'T'00:00:00XXX", Locale.ROOT).format(calendar.time)
}
