# Advanced Animation Specs

Deep reference for keyframes, snap, decay, custom easing, multi-segment animations, and animation spec composition.

## Keyframes

Define specific values at specific times within an animation. Useful for multi-phase or non-linear motion.

### Basic keyframes

```kotlin
val offset by animateFloatAsState(
    targetValue = if (expanded) 1f else 0f,
    animationSpec = keyframes {
        durationMillis = 600
        0f at 0 using LinearEasing           // start at 0
        0.4f at 150 using FastOutSlowInEasing // quick jump to 40%
        0.35f at 250 using LinearEasing       // slight overshoot back
        1f at 600 using FastOutSlowInEasing   // settle to final
    },
    label = "bouncy-offset"
)
```

### Keyframes for size animation

```kotlin
SizeTransform { initialSize, targetSize ->
    keyframes {
        durationMillis = 500
        // Width expands first (0-200ms)
        IntSize(targetSize.width, initialSize.height) at 200 using FastOutSlowInEasing
        // Then height expands (200-500ms)
        IntSize(targetSize.width, targetSize.height) at 500 using FastOutSlowInEasing
    }
}
```

### Keyframes for color

```kotlin
val color by animateColorAsState(
    targetValue = if (error) Color.Red else Color.Green,
    animationSpec = keyframes {
        durationMillis = 600
        Color.Red at 0
        Color.Red at 300                // hold red for 300ms
        Color.Yellow at 400             // flash yellow
        Color.Green at 600              // settle to green
    },
    label = "status-color"
)
```

---

## KeyframesWithSplines (Compose 1.7+)

Smooth interpolation through keyframe points using cubic splines — gives natural curves through waypoints:

```kotlin
val x by animateFloatAsState(
    targetValue = targetX,
    animationSpec = keyframesWithSpline {
        durationMillis = 800
        0f at 0
        100f at 200     // spline automatically curves through these points
        80f at 400      // no need to specify easing — spline handles smoothness
        200f at 800
    },
    label = "spline-x"
)
```

Use for **path-like motion** where an element should follow a smooth curve through multiple waypoints.

---

## Snap

Instant jump — zero duration, no animation. Useful for resetting state.

```kotlin
animateFloatAsState(
    targetValue = newValue,
    animationSpec = snap(delayMillis = 0),  // instant
    label = "instant"
)

// snap with delay — wait, then jump
snap(delayMillis = 200)  // wait 200ms, then snap to target
```

**Use when:** resetting after a completed animation, or when animation would be distracting.

---

## Exponential Decay

Physics-based deceleration — starts at a velocity and slows to zero. Used for fling gestures.

```kotlin
import androidx.compose.animation.core.exponentialDecay

val offset = remember { Animatable(0f) }

// After a fling gesture:
offset.animateDecay(
    initialVelocity = flingVelocity,
    animationSpec = exponentialDecay(
        frictionMultiplier = 1f,        // 1.0 = default friction
        absVelocityThreshold = 0.1f     // stop when velocity drops below this
    )
)
```

**`frictionMultiplier`:**
- `< 1.0` — less friction, slides farther
- `1.0` — default
- `> 1.0` — more friction, stops sooner

---

## Custom Easing Curves

### CubicBezierEasing

The most common custom easing. Two control points define the curve:

```kotlin
import androidx.compose.animation.core.CubicBezierEasing

// M3 Emphasized (dramatic, large transitions)
val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// M3 Emphasized Decelerate (entering elements)
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

// M3 Emphasized Accelerate (exiting elements)
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

// Custom: aggressive start, very gentle end
val CustomEasing = CubicBezierEasing(0.0f, 0.0f, 0.1f, 1.0f)
```

### Visualizing bezier curves

```
Control point format: CubicBezierEasing(x1, y1, x2, y2)

(0,0) = start of animation
(1,1) = end of animation

x axis = time (0 to 1)
y axis = value progress (0 to 1)

Linear:          (0, 0, 1, 1)      — straight line
Fast start:      (0.4, 0, 1, 1)    — starts fast, ends linear
Slow start:      (0, 0, 0.2, 1)    — starts slow, ends fast
Standard M3:     (0.4, 0, 0.2, 1)  — fast out, slow in
Bounce overshoot: (0.2, 1.5, 0.5, 1) — overshoots then settles (y1 > 1)
```

### Custom Easing function

```kotlin
val BounceEasing = Easing { fraction ->
    // Implement any mathematical function mapping [0,1] -> [0,1]
    val n1 = 7.5625f
    val d1 = 2.75f
    var t = fraction

    when {
        t < 1f / d1 -> n1 * t * t
        t < 2f / d1 -> { t -= 1.5f / d1; n1 * t * t + 0.75f }
        t < 2.5f / d1 -> { t -= 2.25f / d1; n1 * t * t + 0.9375f }
        else -> { t -= 2.625f / d1; n1 * t * t + 0.984375f }
    }
}
```

---

## Repeatable & InfiniteRepeatable

### Finite repetition

```kotlin
val pulse by animateFloatAsState(
    targetValue = 1f,
    animationSpec = repeatable(
        iterations = 3,
        animation = tween(200),
        repeatMode = RepeatMode.Reverse  // forward-backward-forward
    ),
    label = "pulse"
)
```

### Infinite repetition

```kotlin
val transition = rememberInfiniteTransition(label = "loading")

val rotation by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart  // 0->360, jump to 0, repeat
    ),
    label = "spin"
)

val breathe by transition.animateFloat(
    initialValue = 0.8f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse  // 0.8->1->0.8->1...
    ),
    label = "breathe"
)
```

---

## Combining Specs

### Tween with delay

```kotlin
tween<Float>(
    durationMillis = 300,
    delayMillis = 150,      // wait 150ms before starting
    easing = FastOutSlowInEasing
)
```

### Spring with initial velocity

```kotlin
spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = 0.01f  // stop when within 0.01 of target
)
```

### Different specs per direction

```kotlin
val transition = updateTransition(expanded, label = "expand")

val height by transition.animateDp(
    transitionSpec = {
        when {
            false isTransitioningTo true ->
                // Expanding: slower, decelerate
                tween(400, easing = LinearOutSlowInEasing)
            else ->
                // Collapsing: faster, accelerate
                tween(250, easing = FastOutLinearInEasing)
        }
    },
    label = "height"
) { if (it) 300.dp else 80.dp }
```

---

## Animation Inspector

All animation specs support the `label` parameter. This shows up in Android Studio's Animation Inspector:

```kotlin
// Always label for debugging
animateFloatAsState(target, tween(300), label = "card-alpha")
updateTransition(state, label = "card-expand")
rememberInfiniteTransition(label = "shimmer")
AnimatedVisibility(visible, label = "section-visibility")
AnimatedContent(state, label = "page-content")
```

---

## Decision Guide: Which Spec?

| Scenario | Spec |
|----------|------|
| Simple A->B with duration | `tween(durationMs, easing)` |
| Natural, interruptible motion | `spring(damping, stiffness)` |
| Multi-phase with specific timing | `keyframes { }` |
| Smooth curve through waypoints | `keyframesWithSpline { }` |
| Instant change, no animation | `snap()` |
| Fling deceleration from velocity | `exponentialDecay()` |
| Repeating N times | `repeatable(iterations, animation)` |
| Continuous loop | `infiniteRepeatable(animation)` |
| Different enter/exit specs | `transitionSpec = { when { A isTransitioningTo B -> ... } }` |
