package presentation.ui.screens.settings

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

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

private fun LazyListState.indexAt(y: Float): Int =
    layoutInfo.visibleItemsInfo.firstOrNull { info ->
        y.toInt() in info.offset until (info.offset + info.size)
    }?.index ?: -1

private const val AUTO_SCROLL_ZONE_PX = 180f
private const val AUTO_SCROLL_MAX_SPEED_PX = 18f

/**
 * Apple Photos-style hold-and-drag multi-select gesture.
 *
 * Uses [detectDragGesturesAfterLongPress] — the same approach as the production
 * `jordond/drag-select-compose` Compose Multiplatform library. This is a single
 * unified gesture: long press detection AND drag tracking happen in the same
 * gesture scope on the LazyColumn, which works reliably on all platforms including iOS.
 *
 * The previous split-gesture approach (LazyColumn polls isSelectionMode() set by
 * WordCard's combinedClickable.onLongPress) fails on iOS because iOS stops sending
 * pointer events while the finger is stationary, so the polling loop never fires.
 *
 * Auto-scroll speed is written to [DragSelectState.autoScrollSpeed]. The caller
 * must run a LaunchedEffect watching that value to actually scroll the list.
 *
 * [onDragStarted]: long press threshold reached at [index] — enter selection mode.
 * [onItemEntered]: finger moved onto a new item at [index] — toggle selection.
 */
fun Modifier.dragSelectGesture(
    lazyListState: LazyListState,
    dragSelectState: DragSelectState,
    onDragStarted: (index: Int) -> Unit,
    onItemEntered: (index: Int) -> Unit,
): Modifier = pointerInput(Unit) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            val index = lazyListState.indexAt(offset.y)
            if (index >= 0) {
                dragSelectState.start(index)
                onDragStarted(index)
            }
        },
        onDragEnd = dragSelectState::end,
        onDragCancel = dragSelectState::end,
        onDrag = { change, _ ->
            change.consume()
            val y = change.position.y
            val viewportH = lazyListState.layoutInfo.viewportSize.height.toFloat()
            dragSelectState.autoScrollSpeed = when {
                y < AUTO_SCROLL_ZONE_PX ->
                    -AUTO_SCROLL_MAX_SPEED_PX * (1f - y / AUTO_SCROLL_ZONE_PX)
                y > viewportH - AUTO_SCROLL_ZONE_PX ->
                    AUTO_SCROLL_MAX_SPEED_PX * (1f - (viewportH - y) / AUTO_SCROLL_ZONE_PX)
                else -> 0f
            }
            val itemIndex = lazyListState.indexAt(y)
            if (dragSelectState.moveTo(itemIndex)) onItemEntered(itemIndex)
        },
    )
}

/**
 * Platform hook — all platforms are no-ops since [detectDragGesturesAfterLongPress]
 * works natively in common Compose code on all platforms.
 */
@Composable
internal expect fun DragSelectScrollViewSetup()
