package br.com.sobraai.ui.runtime

private fun calendar(zone: String?) = java.util.Calendar.getInstance(zone?.let(java.util.TimeZone::getTimeZone) ?: java.util.TimeZone.getDefault())
private val fields = intArrayOf(1, 2, 5, 11, 12, 13, 14)
internal actual fun dateFields(millis: Long, zone: String?): IntArray = calendar(zone).apply { timeInMillis = millis }.let { c -> fields.map(c::get).toIntArray() }
internal actual fun dateMillis(fields: IntArray, zone: String?): Long = calendar(zone).apply {
    clear(); set(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]); set(14, fields[6])
}.timeInMillis
private fun formatter(pattern: String, zone: String?) = java.text.SimpleDateFormat(pattern, java.util.Locale.forLanguageTag("pt-BR")).apply {
    timeZone = zone?.let(java.util.TimeZone::getTimeZone) ?: java.util.TimeZone.getDefault()
}
internal actual fun formatDatePattern(millis: Long, pattern: String, zone: String?): String = formatter(pattern, zone).format(java.util.Date(millis))
internal actual fun parseDatePattern(text: String, pattern: String, zone: String?, lenient: Boolean): Long? = formatter(pattern, zone).apply { isLenient = lenient }.parse(text)?.time
