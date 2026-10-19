package br.com.sobraai.ui

import platform.Foundation.*
import kotlin.math.abs

actual fun today(): FinancialDate {
    val parts = NSCalendar.currentCalendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, NSDate())
    return FinancialDate(parts.year.toInt(), parts.month.toInt(), parts.day.toInt())
}
actual fun apiDate(date: FinancialDate): String {
    val calendar = NSCalendar.currentCalendar
    val parts = NSDateComponents().apply { year = date.year.toLong(); month = date.month.toLong(); day = date.day.toLong(); hour = 0; minute = 0; second = 0 }
    val local = requireNotNull(calendar.dateFromComponents(parts))
    val seconds = calendar.timeZone.secondsFromGMTForDate(local).toInt()
    val offset = if (seconds == 0) "Z" else "${if (seconds < 0) "-" else "+"}${(abs(seconds) / 3600).toString().padStart(2, '0')}:${(abs(seconds) % 3600 / 60).toString().padStart(2, '0')}"
    return "${date.iso}T00:00:00$offset"
}
