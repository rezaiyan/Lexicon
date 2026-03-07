# Gesture-Driven Animation

Deep reference for animations that respond to touch input — drag, swipe, fling, snap.

## Core Principle

Gesture animations must feel **directly connected** to the finger. Use spring physics (not tween) for anything the user is actively manipulating — springs preserve velocity on interruption, making the UI feel alive.

---

## Animatable: The Gesture Animation Primitive

`Animatable` is the lowest-level animation tool. It supports:
- `snapTo(value)` — instant jump (finger tracking)
- `animateTo(value, spec)` — animated settle
- `animateDecay(velocity, spec)` — fling with deceleration
- `stop()` — interrupt in-flight animation

```kotlin
val offsetX = remember { Animatable(0f) }

// In a coroutine (gesture handler or LaunchedEffect):
offsetX.snapTo(newValue)                                          // track finger
offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessLow))   // settle back
offsetX.animateDecay(velocity, exponentialDecay())                // fling
```

### Bounds

```kotlin
val offsetX = remember { Animatable(0f) }

// Set hard limits — animation/snap will clamp to these
offsetX.updateBounds(lowerBound = -300f, upperBound = 300f)
```

---

## Drag Gestures

### Basic drag with settle-back

```kotlin
val offsetY = remember { Animatable(0f) }
val scope = rememberCoroutineScope()

Box(
    modifier = Modifier
        .offset { IntOffset(0, offsetY.value.roundToInt()) }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { scope.launch { offsetY.stop() } },
                onDrag = { change, dragAmount ->
                    change.consume()
                    scope.launch { offsetY.snapTo(offsetY.value + dragAmount) }
                },
                onDragEnd = {
                    scope.launch {
                        offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            )
        }
)
```

### Drag with fling (velocity-aware)

```kotlin
val offsetX = remember { Animatable(0f) }
val scope = rememberCoroutineScope()
val decay = remember { exponentialDecay<Float>(frictionMultiplier = 1.5f) }

Modifier.pointerInput(Unit) {
    detectHorizontalDragGestures(
        onDragEnd = {
            scope.launch {
                // Fling using the last known velocity
                val flingResult = offsetX.animateDecay(velocity, decay)

                // After fling settles, optionally snap to nearest anchor
                val nearestAnchor = findNearestAnchor(flingResult.endState.value)
                offsetX.animateTo(nearestAnchor, spring(stiffness = Spring.StiffnessMedium))
            }
        },
        onDrag = { change, dragAmount ->
            change.consume()
            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
        }
    )
}
```

### Drag with resistance (rubber-band effect)

When dragging beyond bounds, apply resistance so it feels constrained:

```kotlin
fun rubberBand(overscroll: Float, maxOverscroll: Float, tension: Float = 0.55f): Float {
    val clamped = overscroll.coerceIn(-maxOverscroll, maxOverscroll)
    return (1f - (1f / ((clamped * tension / maxOverscroll) + 1f))) * maxOverscroll *
        if (clamped < 0f) -1f else 1f
}

// In onDrag:
val raw = currentOffset + dragAmount
val resistance = if (raw < lowerBound || raw > upperBound) {
    val overscroll = if (raw < lowerBound) raw - lowerBound else raw - upperBound
    val bounded = if (raw < lowerBound) lowerBound else upperBound
    bounded + rubberBand(overscroll, maxOverscroll = 200f)
} else raw

scope.launch { offsetX.snapTo(resistance) }
```

---

## Swipe-to-Dismiss

### Pattern: swipe card away with threshold

```kotlin
@Composable
fun SwipeToDismissCard(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissThreshold = 300f

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = offsetX.value
                alpha = 1f - (abs(offsetX.value) / dismissThreshold).coerceIn(0f, 1f) * 0.5f
                rotationZ = (offsetX.value / dismissThreshold) * 8f  // slight tilt
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offsetX.value) > dismissThreshold) {
                                // Fling off screen
                                val target = if (offsetX.value > 0) size.width.toFloat() * 1.5f
                                             else -size.width.toFloat() * 1.5f
                                offsetX.animateTo(target, tween(200))
                                onDismiss()
                            } else {
                                // Snap back
                                offsetX.animateTo(0f, spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ))
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                )
            }
    ) {
        content()
    }
}
```

---

## AnchoredDraggable (M3 pattern)

For multi-anchor swipe states (bottom sheet, swipe actions). Available in Compose Foundation 1.6+.

```kotlin
enum class SheetValue { Hidden, PartiallyExpanded, Expanded }

@Composable
fun AnchoredSheet(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val anchors = with(density) {
        DraggableAnchors {
            SheetValue.Hidden at 0f
            SheetValue.PartiallyExpanded at 400.dp.toPx()
            SheetValue.Expanded at 800.dp.toPx()
        }
    }

    val state = remember {
        AnchoredDraggableState(
            initialValue = SheetValue.Hidden,
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            decayAnimationSpec = exponentialDecay()
        )
    }

    LaunchedEffect(anchors) {
        state.updateAnchors(anchors)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, -state.requireOffset().roundToInt()) }
            .anchoredDraggable(state, Orientation.Vertical)
    ) {
        content()
    }
}
```

---

## Velocity Tracking

When building custom gestures, track velocity for natural feel:

```kotlin
val velocityTracker = remember { VelocityTracker() }

Modifier.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        velocityTracker.resetTracking()

        do {
            val event = awaitPointerEvent()
            val change = event.changes.first()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            // ... handle drag
        } while (event.changes.any { it.pressed })

        val velocity = velocityTracker.calculateVelocity()
        // Use velocity.x / velocity.y for fling
    }
}
```

---

## Gesture + Animation State Coordination

### Pattern: animate properties alongside gesture offset

```kotlin
// As the user drags, derive multiple visual properties from offset:
Box(
    modifier = Modifier.graphicsLayer {
        val progress = (offsetX.value / maxDistance).coerceIn(-1f, 1f)

        translationX = offsetX.value
        scaleX = 1f - abs(progress) * 0.1f        // slight shrink
        scaleY = 1f - abs(progress) * 0.1f
        rotationZ = progress * 12f                  // tilt toward swipe direction
        alpha = 1f - abs(progress) * 0.4f           // fade out
    }
)
```

### Pattern: snap to discrete positions

```kotlin
val anchors = listOf(0f, 200f, 400f)

fun findNearestAnchor(value: Float): Float =
    anchors.minByOrNull { abs(it - value) } ?: 0f

// After drag ends or fling settles:
val target = findNearestAnchor(currentValue)
offset.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
```

---

## Performance Notes

1. **Always use `graphicsLayer` for gesture-driven transforms** — never `Modifier.offset(x.dp)` or `.alpha(value)` in composition
2. **`stop()` before new gesture** — always stop in-flight animations when a new gesture begins
3. **`snapTo` tracks the finger** — don't use `animateTo` for live tracking; it adds latency
4. **Use `rememberCoroutineScope`** — gesture callbacks need a scope; don't launch from composition
5. **Consume pointer changes** — call `change.consume()` to prevent parent scroll from stealing
