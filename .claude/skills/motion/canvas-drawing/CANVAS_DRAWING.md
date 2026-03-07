# Canvas & DrawScope Animation

Deep reference for animating within `Canvas` and `Modifier.drawBehind`/`drawWithContent` — arcs, paths, gradients, particles, and custom painted effects.

## When to Use Canvas Animation

- Progress rings / arcs (like `ProgressRing`)
- Custom charts and graphs
- Particle effects, confetti
- Path drawing / tracing
- Gradient sweeps and shimmers
- Waveforms, ripples, organic shapes

---

## Animated Arc (Progress Ring Pattern)

From `feature/study/ui/study/ProgressRing.kt`:

```kotlin
@Composable
fun AnimatedArc(
    progress: Float,       // 0f..1f target
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = Color.LightGray
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "arc"
    )

    Canvas(modifier = modifier) {
        val sw = strokeWidth.toPx()
        val diameter = size.minDimension - sw
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        // Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(sw, cap = StrokeCap.Round)
        )

        // Progress
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(sw, cap = StrokeCap.Round)
        )
    }
}
```

### Gradient Arc

```kotlin
Canvas(modifier = modifier) {
    val brush = Brush.sweepGradient(
        0f to startColor,
        animatedProgress to endColor,
        1f to startColor.copy(alpha = 0.1f)
    )
    drawArc(
        brush = brush,
        startAngle = -90f,
        sweepAngle = 360f * animatedProgress,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(sw, cap = StrokeCap.Round)
    )
}
```

---

## Animated Path Drawing

Trace a path from start to end using `PathMeasure`:

```kotlin
@Composable
fun AnimatedPathTrace(
    path: Path,
    color: Color,
    durationMs: Int = 1500,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, false)
        val totalLength = pathMeasure.length

        val partialPath = Path()
        pathMeasure.getSegment(0f, totalLength * progress.value, partialPath, true)

        drawPath(
            path = partialPath,
            color = color,
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
```

---

## Shimmer Effect

Gradient that sweeps across a surface:

```kotlin
@Composable
fun ShimmerModifier(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shimmer-x"
    )

    return Modifier.drawBehind {
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.3f),
                Color.Transparent
            ),
            start = Offset(size.width * translateX, 0f),
            end = Offset(size.width * (translateX + 1f), size.height)
        )
        drawRect(brush)
    }
}
```

---

## Animated Gradient Rotation

Rotate a gradient around a center point:

```kotlin
@Composable
fun RotatingGradientBorder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gradient-rotate")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "angle"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Compute gradient endpoints from angle
        val radians = Math.toRadians(angle.toDouble())
        val dx = (radius * kotlin.math.cos(radians)).toFloat()
        val dy = (radius * kotlin.math.sin(radians)).toFloat()

        val brush = Brush.linearGradient(
            colors = listOf(Color(0xFF7F5AF0), Color(0xFF2CB67D), Color(0xFFFF8906)),
            start = Offset(center.x - dx, center.y - dy),
            end = Offset(center.x + dx, center.y + dy)
        )

        drawCircle(
            brush = brush,
            radius = radius,
            center = center,
            style = Stroke(4.dp.toPx())
        )
    }
}
```

---

## Particle System

Basic particle animation using Canvas:

```kotlin
@Immutable
data class Particle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val alpha: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun ParticleEffect(
    particleCount: Int = 30,
    modifier: Modifier = Modifier
) {
    var particles by remember {
        mutableStateOf(List(particleCount) { createRandomParticle() })
    }

    // Animate at 60fps
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis { frameTimeMs ->
                particles = particles.map { p ->
                    val newY = p.y + p.velocityY
                    val newAlpha = (p.alpha - 0.008f).coerceAtLeast(0f)
                    if (newAlpha <= 0f) createRandomParticle()
                    else p.copy(
                        x = p.x + p.velocityX,
                        y = newY,
                        alpha = newAlpha
                    )
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.radius,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}
```

---

## Waveform / Sine Animation

```kotlin
@Composable
fun AnimatedWave(
    color: Color,
    modifier: Modifier = Modifier,
    amplitude: Float = 20f,
    frequency: Float = 2f
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val path = Path()
        val steps = size.width.toInt()
        path.moveTo(0f, size.height / 2)

        for (x in 0..steps) {
            val xNorm = x.toFloat() / steps
            val y = size.height / 2 + amplitude * sin(frequency * 2 * PI.toFloat() * xNorm + phase)
            path.lineTo(x.toFloat(), y)
        }

        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}
```

---

## drawWithContent: Overlay Effects

Draw on top of or behind existing composable content:

```kotlin
// Animated border glow
Modifier.drawWithContent {
    drawContent()  // draw the composable first

    // Then draw glow overlay
    val glowAlpha = animatedGlowAlpha.value
    if (glowAlpha > 0f) {
        drawRoundRect(
            color = Color(0xFF7F5AF0).copy(alpha = glowAlpha * 0.3f),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(3.dp.toPx()),
            blendMode = BlendMode.Screen
        )
    }
}

// Animated scrim over content
Modifier.drawWithContent {
    drawContent()
    drawRect(Color.Black.copy(alpha = scrimAlpha.value))
}
```

---

## Performance Notes

1. **Canvas redraws every frame** when animated values change — keep draw operations simple
2. **Use `remember` for Path objects** — don't recreate paths every frame
3. **`drawBehind`** is slightly more efficient than `Canvas` composable when you only need to draw behind content
4. **Avoid allocations inside draw lambdas** — pre-compute colors, offsets, paths
5. **`withFrameMillis`** gives frame-perfect timing for particle/physics simulations
6. **Clip large canvas areas** to avoid overdraw outside visible bounds

```kotlin
// Pre-compute outside draw lambda
val precomputedPath = remember(data) { buildPathFromData(data) }
val precomputedColor = remember(isActive) { if (isActive) activeColor else inactiveColor }

Canvas(modifier) {
    drawPath(precomputedPath, precomputedColor)
}
```
