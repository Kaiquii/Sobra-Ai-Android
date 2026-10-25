package com.example.appfinanceiro

import com.example.appfinanceiro.core.designsystem.components.selectorDialogProperties
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectorDialogWindowTest {
    @Test fun customWidthSelectorLetsComposeHandleKeyboardInsetsWithoutFloatingWindowPan() {
        val properties = selectorDialogProperties()
        assertFalse(properties.usePlatformDefaultWidth)
        assertFalse(properties.decorFitsSystemWindows)
        assertTrue(properties.dismissOnBackPress)
        assertTrue(properties.dismissOnClickOutside)
    }
}
