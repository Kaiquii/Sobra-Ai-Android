package com.example.appfinanceiro.core.network

/** API eligibility: paying a fixed expense never moves it to an earlier month. */
fun supportsExpenseAdvance(type: String): Boolean = when (type.trim().lowercase()) {
    "única", "unica", "parcelada" -> true
    else -> false
}

val Expense.supportsAdvance: Boolean get() = supportsExpenseAdvance(type)
