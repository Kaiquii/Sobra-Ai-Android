package com.example.appfinanceiro.core.date

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MonthNavigationTest {
    @Test
    fun previousMonthDoesNotDependOnDayOfMonth() {
        assertEquals(MonthYear(7, 2026), shiftMonth(8, 2026, -1))
        assertEquals(MonthYear(9, 2026), shiftMonth(10, 2026, -1))
    }

    @Test
    fun crossesYearInBothDirections() {
        assertEquals(MonthYear(0, 2027), shiftMonth(11, 2026, 1))
        assertEquals(MonthYear(11, 2025), shiftMonth(0, 2026, -1))
    }

    @Test
    fun handlesMultipleYearsAndNegativeYearsWithFloorDivision() {
        assertEquals(MonthYear(10, 2023), shiftMonth(0, 2026, -26))
        assertEquals(MonthYear(1, 2028), shiftMonth(11, 2025, 26))
        assertEquals(MonthYear(11, -1), shiftMonth(0, 0, -1))
        assertEquals(MonthYear(0, -2), shiftMonth(0, 0, -24))
        assertEquals(MonthYear(0, 0), shiftMonth(11, -1, 1))
    }

    @Test
    fun preservesMonthWhenShiftIsZero() {
        for (month in 0..11) {
            assertEquals(MonthYear(month, 2026), shiftMonth(month, 2026, 0))
        }
    }

    @Test
    fun rejectsInvalidMonthIndices() {
        for (month in listOf(-1, 12, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { shiftMonth(month, 2026, 1) }
        }
    }

    @Test
    fun usesLongArithmeticForExtremeShifts() {
        assertEquals(MonthYear(7, 178958996), shiftMonth(0, 2026, Int.MAX_VALUE))
        assertEquals(MonthYear(4, -178954945), shiftMonth(0, 2026, Int.MIN_VALUE))
    }
}
