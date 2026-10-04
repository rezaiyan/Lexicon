package presentation.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Tracks hold-and-drag multi-select gesture state.
 *
 * [autoScrollSpeed] is written by the gesture and consumed by a LaunchedEffect
 * in the caller — negative = scroll up, positive = scroll down, zero = stopped.
 */
class DragSelectState {
    var isDragging by mutableStateOf(false)
        private set

    var autoScrollSpeed by mutableFloatStateOf(0f)
        internal set

    private var lastIndex = -1

    fun start(index: Int) {
        isDragging = true
        lastIndex = index
    }

    /** Returns true when the finger crossed into a new, valid item. */
    fun moveTo(index: Int): Boolean {
        if (index < 0 || index == lastIndex) return false
        lastIndex = index
        return true
    }

    fun end() {
        isDragging = false
        lastIndex = -1
        autoScrollSpeed = 0f
    }
}
