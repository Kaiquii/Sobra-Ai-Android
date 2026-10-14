package com.example.appfinanceiro.core.designsystem

import com.example.appfinanceiro.core.designsystem.components.FinancialPrivacyState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialPrivacyStateTest {
    @Test
    fun sharedStateTogglesVisibilityInBothDirections() {
        val state = FinancialPrivacyState()
        assertFalse(state.valuesHidden)
        state.toggle()
        assertTrue(state.valuesHidden)
        state.toggle()
        assertFalse(state.valuesHidden)
    }

    @Test
    fun restoredStateKeepsThePrivacyChoice() {
        val hidden = FinancialPrivacyState.Saver.restore(true)!!
        val visible = FinancialPrivacyState.Saver.restore(false)!!
        assertTrue(hidden.valuesHidden)
        assertFalse(visible.valuesHidden)
        hidden.toggle()
        assertFalse(hidden.valuesHidden)
    }
}
