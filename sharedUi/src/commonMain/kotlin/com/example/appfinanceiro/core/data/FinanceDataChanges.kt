package com.example.appfinanceiro.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// A revision retains invalidation even while a financial screen is off the back stack.
object FinanceDataChanges {
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()

    fun notifyExpenseCreated() {
        notifyExpenseChanged()
    }

    fun notifyExpenseChanged() {
        _revision.update { it + 1 }
    }

    fun notifyCategoryChanged() {
        _revision.update { it + 1 }
    }

    fun notifyIncomeChanged() {
        _revision.update { it + 1 }
    }
}
