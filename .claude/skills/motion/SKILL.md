---
name: motion
description: Motion design, animation, and transitions for Compose Multiplatform — M3 motion tokens, Compose animation APIs, easing curves, transition patterns, choreography, and performance
argument-hint: "<animation-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
---

# Motion & Animation

Use this skill when implementing animations, transitions, or any motion in Compose UI.

## Principles (M3 Motion)

1. **Informative** — motion shows spatial and hierarchical relationships between elements
2. **Focused** — motion draws attention to what matters without distraction
3. **Expressive** — motion celebrates moments and adds personality

**Core rule:** Every animation must have a *purpose*. If removing the animation doesn't degrade UX, remove it.

---

## Lexicon Motion Tokens (`Theme.motion`)

Always use `Theme.motion` — never hardcode durations or easing. Defined in `design-system/src/commonMain/kotlin/theme/AppTheme.kt`:

```kotlin
val motion = Theme.motion

// Durations (ms)
motion.durationXShort   // 100ms — micro-interactions (ripple, icon toggle)
motion.durationShort    // 150ms — small UI (checkbox, switch, FAB press)
motion.durationShort2   // 200ms — tooltips, small fade-ins
motion.durationMedium   // 300ms — DEFAULT for most animations (expand, collapse, slide)
motion.durationMedium2  // 400ms — overlays, bottom sheets, cards
motion.durationLong     // 500ms — container transforms, page transitions
motion.durationXLong    // 800ms — complex multi-step, staggered lists
motion.durationXXLong   // 1200ms — dramatic reveal, onboarding hero

// Easing curves
motion.easingStandard    // FastOutSlowInEasing — cubic-bezier(0.4, 0.0, 0.2, 1.0) — DEFAULT
motion.easingDecelerate  // LinearOutSlowInEasing — cubic-bezier(0.0, 0.0, 0.2, 1.0)
motion.easingAccelerate  // FastOutLinearInEasing — cubic-bezier(0.4, 0.0, 1.0, 1.0)
motion.easingLinear      // LinearEasing — cubic-bezier(0.0, 0.0, 1.0, 1.0)
```

---

## M3 Easing Reference

### When to use which easing

| Easing | Curve | Use for |
|--------|-------|---------|
| **Standard** | `FastOutSlowInEasing` (0.4, 0, 0.2, 1) | Elements that **stay on screen** — moving, resizing, recoloring |
| **Standard decelerate** | `LinearOutSlowInEasing` (0, 0, 0.2, 1) | Elements **entering** — slide in, fade in, expand |
| **Standard accelerate** | `FastOutLinearInEasing` (0.4, 0, 1, 1) | Elements **exiting** — slide out, fade out, collapse |
| **Emphasized** | `CubicBezierEasing(0.2, 0, 0, 1)` | Large/dramatic transitions — container transforms, hero animations |
| **Emphasized decelerate** | `CubicBezierEasing(0.05, 0.7, 0.1, 1)` | Enter stage of emphasized — fast start, gentle landing |
| **Emphasized accelerate** | `CubicBezierEasing(0.3, 0, 0.8, 0.15)` | Exit stage of emphasized — gentle start, fast departure |
| **Linear** | `LinearEasing` | Progress bars, continuous rotation, color loops |

### Custom easing in Compose

```kotlin
import androidx.compose.animation.core.CubicBezierEasing

// M3 Emphasized (not in default tokens — define when needed)
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
```

---

## M3 Duration Scale

| Token | Value | Use for |
|-------|-------|---------|
| `durationShort1` | 50ms | Micro — icon morph, ripple start |
| `durationShort2` | 100ms | Small utility — toggle, checkbox |
| `durationShort3` | 150ms | Quick feedback — FAB, switch |
| `durationShort4` | 200ms | Tooltip, small fade |
| `durationMedium1` | 250ms | Menu open, chip expand |
| `durationMedium2` | 300ms | **Default** — expand/collapse, slide |
| `durationMedium3` | 350ms | Bottom sheet, dialog |
| `durationMedium4` | 400ms | Card expand, navigation |
| `durationLong1` | 450ms | Container transform enter |
| `durationLong2` | 500ms | Page transition |
| `durationLong3` | 550ms | Complex multi-element |
| `durationLong4` | 600ms | Full-screen reveal |
| `durationExtraLong1` | 700ms | Staggered list |
| `durationExtraLong2` | 800ms | Elaborate choreography |
| `durationExtraLong3` | 900ms | Onboarding hero |
| `durationExtraLong4` | 1000ms | Dramatic first-run |

**Rule of thumb:** match `Theme.motion.duration*` to the closest M3 token for the element type.

---

## Compose Animation API Hierarchy

Choose the **highest-level** API that fits your need. Lower = more control but more code.

### Level 1: High-level composables (preferred)

```kotlin
// Visibility — enter/exit with customizable transitions
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(tween(motion.durationMedium)) + slideInVertically { it / 4 },
    exit = fadeOut(tween(motion.durationShort2)) + slideOutVertically { it / 4 }
) {
    Content()
}

// Content switching — animate between different content
AnimatedContent(
    targetState = currentTab,
    transitionSpec = {
        fadeIn(tween(motion.durationMedium, easing = motion.easingDecelerate)) togetherWith
        fadeOut(tween(motion.durationShort2, easing = motion.easingAccelerate))
    },
    label = "tab-switch"
) { tab ->
    TabContent(tab)
}

// Simple crossfade
Crossfade(
    targetState = screenState,
    animationSpec = tween(motion.durationMedium),
    label = "screen-crossfade"
) { state ->
    when (state) { ... }
}
```

### Level 2: State-driven animation

```kotlin
// Single value — animates whenever target changes
val alpha by animateFloatAsState(
    targetValue = if (selected) 1f else 0.6f,
    animationSpec = tween(motion.durationShort2, easing = motion.easingStandard),
    label = "alpha"
)

val color by animateColorAsState(
    targetValue = if (active) Theme.colors.primary else Theme.colors.surface,
    animationSpec = tween(motion.durationMedium),
    label = "color"
)

val size by animateDpAsState(
    targetValue = if (expanded) 200.dp else 56.dp,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    label = "size"
)
```

### Level 3: Transition — coordinated multi-property animation

```kotlin
val transition = updateTransition(targetState = cardState, label = "card")

val elevation by transition.animateDp(
    transitionSpec = { tween(motion.durationMedium) },
    label = "elevation"
) { state -> if (state == CardState.Expanded) 8.dp else 1.dp }

val cornerRadius by transition.animateDp(
    transitionSpec = { tween(motion.durationMedium) },
    label = "corner"
) { state -> if (state == CardState.Expanded) 0.dp else 12.dp }

val backgroundColor by transition.animateColor(
    transitionSpec = { tween(motion.durationMedium) },
    label = "bg"
) { state -> if (state == CardState.Expanded) surface else surfaceVariant }
```

### Level 4: Animatable — imperative, coroutine-driven

```kotlin
val offset = remember { Animatable(0f) }

LaunchedEffect(isOpen) {
    if (isOpen) {
        offset.animateTo(
            targetValue = 1f,
            animationSpec = tween(motion.durationLong, easing = EmphasizedDecelerateEasing)
        )
    } else {
        offset.animateTo(
            targetValue = 0f,
            animationSpec = tween(350, easing = motion.easingStandard)
        )
    }
}
```

### Level 5: InfiniteTransition — looping animations

```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "pulse")

val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
        animation = tween(900, easing = motion.easingStandard),
        repeatMode = RepeatMode.Reverse
    ),
    label = "pulse-scale"
)
```

---

## Spring vs Tween

| Property | `tween` | `spring` |
|----------|---------|----------|
| Duration | Fixed (ms) | Physics-based (no fixed duration) |
| Feel | Predictable, designed | Natural, organic |
| Interruption | Snaps to new animation | Preserves velocity (seamless) |
| Best for | Opacity, color, rotation | Position, size, scale |
| M3 alignment | Standard/emphasized easing | Bouncy/snappy interactions |

```kotlin
// Spring presets
spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)      // smooth settle
spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)   // gentle bounce
spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)      // playful bounce

// Spring stiffness scale
Spring.StiffnessHigh         // 10000 — snap (keyboard appear)
Spring.StiffnessMediumHigh   // 5000
Spring.StiffnessMedium       // 1500 — default
Spring.StiffnessMediumLow    // 400  — gentle (drag settle)
Spring.StiffnessLow          // 200  — slow (page morph)
Spring.StiffnessVeryLow      // 50   — very slow (background parallax)
```

**Prefer spring for interruptible animations** (drag, swipe, gesture-driven) since springs preserve velocity on interruption.

---

## M3 Transition Patterns

### 1. Container Transform

A source element morphs into a destination. The container expands/collapses while content cross-fades.

**When:** navigating between a list item and its detail, FAB to screen, card to full-screen.

Lexicon has `TransitionOverlay` in `design-system/src/commonMain/kotlin/overlay/transition/TransitionOverlay.kt`:

```kotlin
// Usage with OverlayHost
val sourceRect = remember { mutableStateOf(Rect.Zero) }

Card(
    modifier = Modifier.onGloballyPositioned { coords ->
        val pos = coords.positionInWindow()
        sourceRect.value = Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height)
    },
    onClick = {
        overlayHost.show(TransitionOverlay(
            sourceRect = sourceRect.value,
            sourceCornerRadiusDp = 12f
        ) { progress, navigator ->
            DetailScreen(transitionProgress = progress, onDismiss = { navigator.dismiss() })
        })
    }
)
```

Key implementation details:
- Enter: 500ms with emphasized-decelerate easing
- Exit: 350ms with standard easing
- Scrim fades to 32% black during first half
- Content revealed via "window reveal" (clipped by expanding container)
- `transitionProgress` (0f-1f) passed to content for per-element choreography

### 2. Shared Axis

Elements move together along a shared X, Y, or Z axis.

**When:** navigating between steps (onboarding), tabs with spatial order, forward/backward in a flow.

```kotlin
// Shared X-axis (horizontal page transition)
AnimatedContent(
    targetState = step,
    transitionSpec = {
        val direction = if (targetState > initialState) 1 else -1
        slideInHorizontally { direction * it / 3 } + fadeIn(tween(motion.durationMedium)) togetherWith
        slideOutHorizontally { -direction * it / 3 } + fadeOut(tween(motion.durationShort2))
    },
    label = "shared-x"
) { currentStep ->
    StepContent(currentStep)
}

// Shared Y-axis (vertical drill-down)
slideInVertically { it / 4 } + fadeIn() togetherWith
slideOutVertically { -it / 4 } + fadeOut()

// Shared Z-axis (depth — parent to child)
scaleIn(initialScale = 0.92f) + fadeIn() togetherWith
scaleOut(targetScale = 1.08f) + fadeOut()
```

### 3. Fade Through

Sequential fade-out then fade-in with slight scale. No spatial relationship.

**When:** switching bottom nav tabs, swapping unrelated content.

```kotlin
AnimatedContent(
    targetState = selectedTab,
    transitionSpec = {
        (fadeIn(tween(motion.durationMedium / 2, delayMillis = motion.durationMedium / 2)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(motion.durationMedium / 2, delayMillis = motion.durationMedium / 2)))
            .togetherWith(
                fadeOut(tween(motion.durationMedium / 2)) +
                scaleOut(targetScale = 1.08f, animationSpec = tween(motion.durationMedium / 2))
            )
    },
    label = "fade-through"
) { tab -> ... }
```

### 4. Fade

Simple opacity change. Minimal spatial meaning.

**When:** dialogs, snackbars, tooltips, overlays appearing/disappearing.

```kotlin
AnimatedVisibility(
    visible = show,
    enter = fadeIn(tween(motion.durationShort2, easing = motion.easingDecelerate)),
    exit = fadeOut(tween(motion.durationShort, easing = motion.easingAccelerate))
)
```

---

## Choreography

Choreography coordinates multiple elements within a single transition.

### Staggered Entrance

Lexicon has `Modifier.staggeredFadeSlide()` in `design-system/src/commonMain/kotlin/components/animation/StaggeredFadeSlide.kt`:

```kotlin
// Usage — each item delays by index * 55ms
Column {
    items.forEachIndexed { index, item ->
        ItemRow(
            item = item,
            modifier = Modifier.staggeredFadeSlide(index)
        )
    }
}
```

### Manual choreography within a transition

Use `transitionProgress` (0-1) with `smoothStep` to sequence element appearances:

```kotlin
@Composable
fun DetailContent(transitionProgress: Float) {
    val motion = Theme.motion

    // Header appears first (0.0 - 0.4 of overall transition)
    val headerAlpha = smoothStep(0f, 0.4f, transitionProgress)

    // Title slides up slightly after header (0.15 - 0.6)
    val titleAlpha = smoothStep(0.15f, 0.6f, transitionProgress)
    val titleOffsetY = lerp(16f, 0f, smoothStep(0.15f, 0.6f, transitionProgress))

    // Body content last (0.3 - 0.8)
    val bodyAlpha = smoothStep(0.3f, 0.8f, transitionProgress)

    Column {
        Header(Modifier.graphicsLayer { alpha = headerAlpha })
        Title(Modifier.graphicsLayer { alpha = titleAlpha; translationY = titleOffsetY })
        Body(Modifier.graphicsLayer { alpha = bodyAlpha })
    }
}
```

### Shared `smoothStep` utility

Already in `overlay/transition/TransitionOverlay.kt`:

```kotlin
/** Hermite smoothstep — smooth S-curve mapping [edge0..edge1] to [0..1]. */
fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
```

---

## Performance Rules

### 1. Defer reads to layout/draw phase

```kotlin
// GOOD — only re-reads during draw, no recomposition
Modifier.graphicsLayer {
    alpha = animatedAlpha.value
    translationY = animatedOffset.value
    scaleX = animatedScale.value
    scaleY = animatedScale.value
}

Modifier.offset { IntOffset(x.value.roundToInt(), 0) }

Modifier.drawBehind { drawRect(color.copy(alpha = animatedAlpha.value)) }

// BAD — triggers recomposition on every frame
Modifier
    .alpha(animatedAlpha.value)              // recomposes
    .offset(x = animatedOffset.value.dp)     // recomposes
```

### 2. Use `graphicsLayer` for transform animations

`graphicsLayer` modifies the RenderNode without triggering relayout or recomposition:
- `alpha`, `translationX/Y`, `scaleX/Y`, `rotationX/Y/Z`
- `shadowElevation`, `clip`, `shape`

### 3. Label all animations

Every `animate*AsState`, `AnimatedVisibility`, `AnimatedContent`, and `updateTransition` should have a `label` parameter for debugging in the Animation Inspector.

### 4. Remember expensive specs

```kotlin
// GOOD — spec created once
val spec = remember { spring<Float>(dampingRatio = 0.6f, stiffness = 200f) }

// BAD — new spec object every recomposition
val offset by animateFloatAsState(
    targetValue = target,
    animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f) // allocated every frame
)
```

### 5. Avoid animating layout-affecting properties

Prefer `graphicsLayer { translationY = ... }` over `Modifier.offset(y = ...)` or `Modifier.padding()` for continuous animations. Layout changes are expensive; RenderNode transforms are cheap.

---

## Common Patterns

### Expand/collapse section

```kotlin
val motion = Theme.motion
var expanded by remember { mutableStateOf(false) }

AnimatedVisibility(
    visible = expanded,
    enter = expandVertically(
        animationSpec = tween(motion.durationMedium, easing = motion.easingDecelerate),
        expandFrom = Alignment.Top
    ) + fadeIn(tween(motion.durationShort2)),
    exit = shrinkVertically(
        animationSpec = tween(motion.durationShort2, easing = motion.easingAccelerate),
        shrinkTowards = Alignment.Top
    ) + fadeOut(tween(motion.durationXShort))
) {
    ExpandedContent()
}
```

### Rotate icon on expand

```kotlin
val rotation by animateFloatAsState(
    targetValue = if (expanded) 180f else 0f,
    animationSpec = tween(motion.durationMedium, easing = motion.easingStandard),
    label = "chevron-rotation"
)

Icon(
    imageVector = Icons.Default.ExpandMore,
    modifier = Modifier.graphicsLayer { rotationZ = rotation }
)
```

### Shimmer loading placeholder

```kotlin
val transition = rememberInfiniteTransition(label = "shimmer")
val translateX by transition.animateFloat(
    initialValue = -1f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
        tween(motion.durationXXLong, easing = motion.easingLinear),
        RepeatMode.Restart
    ),
    label = "shimmer-x"
)

Modifier.drawBehind {
    val brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.4f), Color.Transparent),
        start = Offset(size.width * translateX, 0f),
        end = Offset(size.width * (translateX + 1f), size.height)
    )
    drawRect(brush)
}
```

### Press/scale feedback

```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()

val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumHigh),
    label = "press-scale"
)

Card(
    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    interactionSource = interactionSource,
    onClick = { ... }
)
```

---

## MaterialTheme.motionScheme (M3 1.3+)

Compose Material 3 ships `MotionScheme` — a theme-level provider of spring-based animation specs. Available in this project (Compose Multiplatform 1.10.2+).

### API

```kotlin
// Access from theme
val motionScheme = MaterialTheme.motionScheme

// Spatial animations — position, size, shape changes (layout-affecting)
motionScheme.fastSpatialSpec<T>()     // Quick spatial — small elements, short distance
motionScheme.slowSpatialSpec<T>()     // Slow spatial — large elements, long distance, container transforms
motionScheme.defaultSpatialSpec<T>()  // Alias for fastSpatialSpec (most common)

// Effects animations — opacity, color, elevation (non-layout)
motionScheme.fastEffectsSpec<T>()     // Quick effects — fade, color flash
motionScheme.slowEffectsSpec<T>()     // Slow effects — gradual color shift, background tint
motionScheme.defaultEffectsSpec<T>()  // Alias for fastEffectsSpec (most common)
```

### Default values (ExpressiveMotionScheme)

```kotlin
// Spatial springs — position, size
fastSpatialSpec  -> spring(dampingRatio = 0.7f, stiffness = 500f)   // ~300ms settle
slowSpatialSpec  -> spring(dampingRatio = 0.8f, stiffness = 200f)   // ~500ms settle

// Effects springs — opacity, color
fastEffectsSpec  -> spring(dampingRatio = 1.0f, stiffness = 1600f)  // ~150ms, no bounce
slowEffectsSpec  -> spring(dampingRatio = 1.0f, stiffness = 800f)   // ~250ms, no bounce
```

### Usage

```kotlin
// Preferred: use motionScheme for M3-consistent springs
val motionScheme = MaterialTheme.motionScheme

val offset by animateIntOffsetAsState(
    targetValue = if (expanded) IntOffset.Zero else collapsedOffset,
    animationSpec = motionScheme.slowSpatialSpec(),
    label = "card-offset"
)

val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = motionScheme.fastEffectsSpec(),
    label = "content-alpha"
)

// AnimatedVisibility with motionScheme
AnimatedVisibility(
    visible = expanded,
    enter = fadeIn(motionScheme.fastEffectsSpec()) + expandVertically(motionScheme.fastSpatialSpec()),
    exit = fadeOut(motionScheme.fastEffectsSpec()) + shrinkVertically(motionScheme.fastSpatialSpec())
)

// AnimatedContent with motionScheme
AnimatedContent(
    targetState = page,
    transitionSpec = {
        (fadeIn(motionScheme.fastEffectsSpec()) + slideInHorizontally { it / 3 })
            .togetherWith(fadeOut(motionScheme.fastEffectsSpec()) + slideOutHorizontally { -it / 3 })
    }
)
```

### When to use MotionScheme vs Theme.motion

| Scenario | Use |
|----------|-----|
| M3 components, standard interactions | `MaterialTheme.motionScheme` — consistent with M3 library internals |
| Custom tween with specific duration/easing | `Theme.motion` duration + easing tokens |
| Container transform / custom transition overlays | `Theme.motion` for precise timing control |
| Gesture-driven / interruptible animations | `motionScheme` spatial springs (velocity-preserving) |
| Choreographed multi-element sequences | `Theme.motion` durations + `smoothStep` for precise scheduling |

**Both are valid.** `MotionScheme` gives physics-based consistency; `Theme.motion` gives precise timing control. Use `MotionScheme` as the default for simple animations, fall back to `Theme.motion` tween specs when you need exact choreography.

---

## Checklist

1. Every animation uses `Theme.motion` tokens — no hardcoded durations/easing
2. Animation has a clear purpose (spatial relationship, feedback, or attention)
3. Chose the highest-level API that fits (AnimatedVisibility > animate*AsState > Animatable)
4. Enter uses decelerate easing, exit uses accelerate easing, persistent uses standard
5. Continuous/frame-by-frame values read in `graphicsLayer`/`offset { }` — not in composition
6. All animations have `label` parameters
7. Spring used for interruptible/gesture animations, tween for designed/fixed animations
8. Duration matches element size: small elements = short, large elements = long
9. Staggered lists use `staggeredFadeSlide` or manual delay choreography
10. Container transforms use `TransitionOverlay` with `OverlayHost`
