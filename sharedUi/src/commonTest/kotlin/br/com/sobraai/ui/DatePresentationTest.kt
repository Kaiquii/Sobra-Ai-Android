package br.com.sobraai.ui

import br.com.sobraai.ui.runtime.*
import kotlin.test.*

class DatePresentationTest {
    @Test fun datePickerUtcDayPreservesLeapDayAndYear() {
        val selected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2028, 1, 29) }
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
        assertEquals("2028-02-29", format.format(selected.time))
        val roundTrip = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = assertNotNull(format.parse("2028-02-29")) }
        assertEquals(2028, roundTrip.get(Calendar.YEAR))
        assertEquals(1, roundTrip.get(Calendar.MONTH))
        assertEquals(29, roundTrip.get(Calendar.DAY_OF_MONTH))
    }
    @Test fun displayAndApiFormatsUseTheSameSelectedDay() {
        val utc = TimeZone.getTimeZone("UTC")
        val display = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply { timeZone = utc; isLenient = false }
        val date = assertNotNull(display.parse("31/12/2026"))
        val api = SimpleDateFormat("yyyy-MM-dd'T'00:00:00XXX", Locale.ROOT).apply { timeZone = utc }
        assertEquals("2026-12-31T00:00:00Z", api.format(date))
        assertEquals("31/12/2026", display.format(date))
    }
    @Test fun clearRemovesClockFieldsBeforeSettingPickerDate() {
        val date = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2027, 0, 1) }
        assertEquals(0, date.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, date.get(Calendar.MINUTE))
        assertEquals(0, date.get(Calendar.SECOND))
        assertEquals(0, date.get(Calendar.MILLISECOND))
    }
}
