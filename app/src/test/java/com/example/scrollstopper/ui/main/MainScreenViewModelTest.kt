package com.example.scrollstopper.ui.main

import junit.framework.TestCase.assertEquals
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun uiState_initialization_hasDefaultValues() {
        val state = MainUiState()
        assertEquals(0, state.xp)
        assertEquals(0, state.streak)
        assertEquals(0, state.customBlocks.size)
    }
}
