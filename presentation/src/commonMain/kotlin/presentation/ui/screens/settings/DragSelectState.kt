package presentation.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Pointer-side state of one hold-and-drag selection. Which words end up selected is decided
 * by the ViewModel from the row index; this only tracks the finger.
 *
 * [autoScrollSpeed] is written by the gesture and consumed by a frame loop in the caller —
 * negative = scroll up, positive = scroll down, zero = stopped. [fingerY] lets that loop
 * keep selecting rows that scroll under a finger that is holding still.
 */
class DragSelectState {
    var isDragging by mutableStateOf(false)
        private set

    var autoScrollSpeed by mutableFloatStateOf(0f)
        internal set

    var fingerY = 0f
        internal set

    private var lastIndex = -1

    fun start(index: Int, y: Float) {
        isDragging = true
        lastIndex = index
        fingerY = y
    }

    /** Returns true when the finger is over a different, valid row than last time. */
    fun moveTo(index: Int): Boolean {
        if (!isDragging || index < 0 || index == lastIndex) return false
        lastIndex = index
        return true
    }

    fun end() {
        isDragging = false
        lastIndex = -1
        autoScrollSpeed = 0f
    }
}
