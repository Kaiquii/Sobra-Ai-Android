package br.com.sobraai.ui.runtime
import platform.Foundation.*

private fun zonedCalendar(zone: String?) = (NSCalendar.currentCalendar.copy() as NSCalendar).apply {
    if (zone != null) timeZone = NSTimeZone.timeZoneWithName(zone) ?: NSTimeZone.localTimeZone
}
internal actual fun dateFields(millis: Long, zone: String?): IntArray {
    val c = zonedCalendar(zone).components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond or NSCalendarUnitNanosecond, NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
    return intArrayOf(c.year.toInt(), c.month.toInt() - 1, c.day.toInt(), c.hour.toInt(), c.minute.toInt(), c.second.toInt(), (c.nanosecond / 1_000_000).toInt())
}
internal actual fun dateMillis(fields: IntArray, zone: String?): Long {
    val c = NSDateComponents().apply {
        year = fields[0].toLong(); month = fields[1].toLong() + 1; day = fields[2].toLong()
        hour = fields[3].toLong(); minute = fields[4].toLong(); second = fields[5].toLong(); nanosecond = fields[6].toLong() * 1_000_000
    }
    return (requireNotNull(zonedCalendar(zone).dateFromComponents(c)).timeIntervalSince1970 * 1000).toLong()
}
private fun formatter(pattern: String, zone: String?) = NSDateFormatter().apply {
    locale = NSLocale("pt_BR"); dateFormat = pattern
    timeZone = zone?.let { NSTimeZone.timeZoneWithName(it) } ?: NSTimeZone.localTimeZone
}
internal actual fun formatDatePattern(millis: Long, pattern: String, zone: String?): String = formatter(pattern, zone).stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
internal actual fun parseDatePattern(text: String, pattern: String, zone: String?, lenient: Boolean): Long? = formatter(pattern, zone).apply { this.lenient = lenient }.dateFromString(text)?.let { (it.timeIntervalSince1970 * 1000).toLong() }
