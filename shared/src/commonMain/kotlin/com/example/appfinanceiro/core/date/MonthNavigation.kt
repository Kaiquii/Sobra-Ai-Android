package com.example.appfinanceiro.core.date

data class MonthYear(
    val monthIndex: Int,
    val year: Int
)

fun shiftMonth(monthIndex: Int, year: Int, amount: Int): MonthYear {
    require(monthIndex in 0..11) { "monthIndex must be between 0 and 11" }

    val totalMonths = year.toLong() * 12L + monthIndex + amount
    return MonthYear(
        monthIndex = totalMonths.mod(12L).toInt(),
        year = totalMonths.floorDiv(12L).toInt()
    )
}
