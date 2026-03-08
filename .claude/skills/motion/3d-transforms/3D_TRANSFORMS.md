# 3D Transforms & graphicsLayer

Deep reference for 3D rotation, perspective, card flips, and advanced `graphicsLayer` usage.

## graphicsLayer Fundamentals

`graphicsLayer` modifies the RenderNode **without triggering recomposition or relayout**. This makes it the most performant way to animate visual properties.

### Available properties

```kotlin
Modifier.graphicsLayer {
    // 2D transforms
    translationX = 0f           // horizontal offset (px)
    translationY = 0f           // vertical offset (px)
    scaleX = 1f                 // horizontal scale (1 = 100%)
    scaleY = 1f                 // vertical scale
    rotationZ = 0f              // rotation around Z axis (degrees, 2D rotation)

    // 3D transforms
    rotationX = 0f              // rotation around X axis (tilt forward/backward)
    rotationY = 0f              // rotation around Y axis (turn left/right, card flip)
    cameraDistance = 8f         // perspective depth (higher = less perspective)

    // Transform origin
    transformOrigin = TransformOrigin(0.5f, 0.5f)  // center by default
    // TransformOrigin(0f, 0f) = top-left
    // TransformOrigin(1f, 1f) = bottom-right
    // TransformOrigin(0.5f, 0f) = top-center

    // Visual
    alpha = 1f                  // opacity (0-1)
    shadowElevation = 0f        // shadow (px)
    ambientShadowColor = Color.Black
    spotShadowColor = Color.Black

    // Clipping
    clip = false                // clip content to shape
    shape = RectangleShape      // clip shape (RoundedCornerShape, CircleShape, etc.)
    renderEffect = null         // blur, color matrix, etc.
}
```

---

## Card Flip Animation

From `FlashCard.kt` — a full 180-degree Y-axis rotation:

```kotlin
@Composable
fun FlippableCard(
    isFlipped: Boolean,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    // Camera distance controls perspective distortion
    // Too close (< 8): extreme perspective, looks warped
    // Too far (> 20): flat, no 3D feel
    val density = LocalDensity.current
    val cameraDistance = with(density) { 12.dp.toPx() }

    Card(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            this.cameraDistance = cameraDistance
        }
    ) {
        if (rotation <= 90f) {
            // Front face — normal
            front()
        } else {
            // Back face — counter-rotate text so it reads correctly
            Box(Modifier.graphicsLayer { rotationY = 180f }) {
                back()
            }
        }
    }
}
```

### Key details:

1. **Camera distance** — `12.dp.toPx()` gives natural perspective. Scale with card size.
2. **Content switching at 90 degrees** — at exactly 90deg the card is edge-on (invisible). Switch content here.
3. **Counter-rotate back content** — back face text would be mirrored without `rotationY = 180f`.
4. **Color transition** — animate card color at the flip midpoint for a polished feel:

```kotlin
val cardColor by animateColorAsState(
    targetValue = if (rotation > 90f) backColor else frontColor,
    animationSpec = tween(200),
    label = "flipColor"
)
```

---

## Camera Distance

Controls the strength of perspective distortion. Higher = flatter, lower = more dramatic.

```kotlin
graphicsLayer {
    rotationY = angle
    cameraDistance = distance
}
```

| Distance (dp) | Effect |
|---------------|--------|
| 4.dp | Extreme perspective — dramatic but can look warped |
| 8.dp | Default Compose value — moderate perspective |
| 12.dp | Recommended for card flips — natural feel |
| 16.dp | Subtle perspective — good for large elements |
| 24.dp+ | Nearly orthographic — minimal 3D feel |

**Rule:** scale camera distance with element size. Larger elements need more distance.

```kotlin
val cameraDistanceDp = when {
    cardHeight < 200.dp -> 10.dp
    cardHeight < 400.dp -> 12.dp
    else -> 16.dp
}
```

---

## Transform Origin

Controls the pivot point for rotation and scaling:

```kotlin
// Rotate from top edge (door hinge effect)
graphicsLayer {
    rotationX = tilt
    transformOrigin = TransformOrigin(0.5f, 0f) // top center
}

// Scale from bottom-right corner
graphicsLayer {
    scaleX = scale
    scaleY = scale
    transformOrigin = TransformOrigin(1f, 1f) // bottom right
}

// Progress bar expanding from left
graphicsLayer {
    scaleX = progress
    transformOrigin = TransformOrigin(0f, 0.5f) // left center
}
```

### Real example from ReviewComponents.kt:

```kotlin
// Progress bar scales from left edge
GradientProgressBar(
    modifier = Modifier.graphicsLayer {
        alpha = barProgress
        scaleX = barProgress
        transformOrigin = TransformOrigin(0f, 0.5f) // expand from left
    }
)
```

---

## 3D Tilt Effects

### Tilt on press

```kotlin
@Composable
fun TiltOnPress(content: @Composable () -> Unit) {
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    val animatedTiltX by animateFloatAsState(tiltX, spring(stiffness = Spring.StiffnessMedium), label = "tiltX")
    val animatedTiltY by animateFloatAsState(tiltY, spring(stiffness = Spring.StiffnessMedium), label = "tiltY")

    Box(
        modifier = Modifier
            .graphicsLayer {
                rotationX = animatedTiltX
                rotationY = animatedTiltY
                cameraDistance = 12f * density
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        // Tilt toward touch point
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        tiltY = ((offset.x - centerX) / centerX) * 8f   // max 8 degrees
                        tiltX = -((offset.y - centerY) / centerY) * 8f  // negative = tilt toward finger

                        tryAwaitRelease()
                        tiltX = 0f
                        tiltY = 0f
                    }
                )
            }
    ) {
        content()
    }
}
```

### Parallax tilt with device sensor

```kotlin
// Conceptual — actual sensor access requires platform APIs
@Composable
fun ParallaxCard(
    tiltX: Float, // from accelerometer, -1 to 1
    tiltY: Float,
    content: @Composable () -> Unit
) {
    val maxTilt = 6f // degrees

    Box(
        modifier = Modifier.graphicsLayer {
            rotationX = tiltX * maxTilt
            rotationY = tiltY * maxTilt
            cameraDistance = 16f * density
        }
    ) {
        content()
    }
}
```

---

## Layered 3D (Parallax Depth)

Create depth by moving layers at different rates:

```kotlin
@Composable
fun ParallaxLayers(offsetX: Float, offsetY: Float) {
    Box {
        // Background layer — moves least
        Image(
            modifier = Modifier.graphicsLayer {
                translationX = offsetX * 0.3f
                translationY = offsetY * 0.3f
            }
        )

        // Middle layer
        Content(
            modifier = Modifier.graphicsLayer {
                translationX = offsetX * 0.6f
                translationY = offsetY * 0.6f
            }
        )

        // Foreground layer — moves most
        Overlay(
            modifier = Modifier.graphicsLayer {
                translationX = offsetX * 1.0f
                translationY = offsetY * 1.0f
            }
        )
    }
}
```

---

## Combining Multiple Transforms

Order matters — transforms are applied in this order:
1. Scale
2. Rotation
3. Translation

```kotlin
graphicsLayer {
    // This creates a card that:
    // 1. Shrinks slightly
    // 2. Tilts
    // 3. Moves down
    scaleX = 0.95f
    scaleY = 0.95f
    rotationZ = 3f               // slight rotation
    translationY = 20f           // shift down
    alpha = 0.8f                 // slightly transparent
    shadowElevation = 8f         // cast shadow
}
```

### Stacking cards (deck effect)

```kotlin
items.forEachIndexed { index, item ->
    val depth = items.size - index

    Card(
        modifier = Modifier.graphicsLayer {
            translationY = depth * 8f        // stack offset
            scaleX = 1f - depth * 0.03f      // progressively smaller
            scaleY = 1f - depth * 0.03f
            alpha = 1f - depth * 0.15f       // progressively transparent
            rotationZ = depth * 1.5f         // slight fan
        }
    ) {
        ItemContent(item)
    }
}
```

---

## RenderEffect (Blur)

```kotlin
// Blur behind a scrim (Android 12+ / iOS)
Modifier.graphicsLayer {
    renderEffect = if (showBlur) {
        BlurEffect(
            radiusX = 10f,
            radiusY = 10f,
            edgeTreatment = TileMode.Decal
        )
    } else null
}
```

**Note:** `BlurEffect` may not be available on all KMP targets. Check platform support.

---

## Performance Rules

1. **graphicsLayer is the cheapest animation path** — it only modifies the RenderNode, no relayout
2. **Use lambda form** — `Modifier.graphicsLayer { }` not `Modifier.graphicsLayer(alpha = ...)`. The lambda form defers state reads to the draw phase.
3. **Combine related transforms in one graphicsLayer** — don't chain multiple `.graphicsLayer { }` calls
4. **Camera distance uses pixels** — always convert from dp: `with(density) { 12.dp.toPx() }` or use `density` property inside graphicsLayer
5. **Avoid graphicsLayer on every LazyColumn item** — hundreds of separate RenderNodes can hurt scrolling. Only apply to items that are actively animating.

```kotlin
// GOOD — single graphicsLayer with all transforms
Modifier.graphicsLayer {
    alpha = animatedAlpha
    translationY = animatedOffset
    scaleX = animatedScale
    scaleY = animatedScale
}

// BAD — multiple graphicsLayer calls
Modifier
    .graphicsLayer { alpha = animatedAlpha }
    .graphicsLayer { translationY = animatedOffset }
    .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
```
