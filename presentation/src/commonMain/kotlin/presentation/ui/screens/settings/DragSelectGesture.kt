package presentation.ui.screens.settings

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val AutoScrollZone = 72.dp
private val AutoScrollMaxSpeed = 14.dp

/**
 * Row under [y] (viewport coordinates). A finger above or below the visible rows maps to the
 * first / last visible row, so dragging past the list edge still extends the range.
 */
internal fun LazyListState.rowAt(y: Float): Int {
    val visible = layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return -1
    val row = visible.firstOrNull { y.toInt() in it.offset until (it.offset + it.size) }
    return when {
        row != null -> row.index
        y < visible.first().offset -> visible.first().index
        else -> visible.last().index
    }
}

/**
 * Photos-style hold-and-drag multi-select on a LazyColumn.
 *
 * Long press waits on the main pass, so a scroll that starts first (and consumes) cancels it.
 * Once the long press fires, every event is read and consumed on the *initial* pass: the list's
 * scrollable and the rows' click handlers never see the drag, so they can't steal it and the
 * release doesn't count as a tap.
 *
 * Auto-scroll speed goes to [DragSelectState.autoScrollSpeed]; the caller runs the scroll loop.
 */
fun Modifier.dragSelectGesture(
    lazyListState: LazyListState,
    dragSelectState: DragSelectState,
    onDragStart: (index: Int) -> Unit,
    onDragTo: (index: Int) -> Unit,
    onDragEnd: () -> Unit,
): Modifier = pointerInput(lazyListState, dragSelectState) {
    val zone = AutoScrollZone.toPx()
    val maxSpeed = AutoScrollMaxSpeed.toPx()

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        val startIndex = lazyListState.rowAt(longPress.position.y)
        if (startIndex < 0) return@awaitEachGesture

        longPress.consume()
        dragSelectState.start(startIndex, longPress.position.y)
        onDragStart(startIndex)

        try {
            var change = awaitOwnChange(longPress.id)
            while (change != null && change.pressed) {
                val y = change.position.y
                val viewport = lazyListState.layoutInfo.viewportSize.height.toFloat()
                dragSelectState.fingerY = y
                dragSelectState.autoScrollSpeed = when {
                    y < zone -> -maxSpeed * (1f - y.coerceAtLeast(0f) / zone)
                    y > viewport - zone -> maxSpeed * (1f - (viewport - y).coerceAtLeast(0f) / zone)
                    else -> 0f
                }
                val index = lazyListState.rowAt(y)
                if (dragSelectState.moveTo(index)) onDragTo(index)
                change = awaitOwnChange(longPress.id)
            }
        } finally {
            dragSelectState.end()
            onDragEnd()
        }
    }
}

/** Next event for [pointerId], taken on the initial pass and consumed so nothing below reacts to it. */
private suspend fun AwaitPointerEventScope.awaitOwnChange(pointerId: PointerId): PointerInputChange? =
    awaitPointerEvent(PointerEventPass.Initial).changes
        .firstOrNull { it.id == pointerId }
        ?.also { it.consume() }

/**
 * Platform hook — all platforms are no-ops since the gesture runs in common Compose code.
 */
@Composable
internal expect fun DragSelectScrollViewSetup()
