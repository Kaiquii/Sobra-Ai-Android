package br.com.sobraai.ui

import platform.Foundation.*

actual fun currentPeriod(): Period {
    val parts = NSCalendar.currentCalendar.components(NSCalendarUnitMonth or NSCalendarUnitYear, NSDate())
    return Period(parts.month.toInt(), parts.year.toInt())
}
