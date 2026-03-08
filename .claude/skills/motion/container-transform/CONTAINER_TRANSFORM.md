# Container Transform

Deep reference for M3 container transform transitions — an element morphs from source to destination.

## When to Use

- List item -> detail screen
- FAB -> new screen
- Card -> expanded view
- Search bar -> search results
- Any navigation where source and destination share a visual container

## Anatomy

A container transform has 4 synchronized layers:

1. **Container** — the bounding shape morphs (position, size, corner radius)
2. **Scrim** — background dims to focus attention
3. **Outgoing content** — fades/scales out as the container begins expanding
4. **Incoming content** — fades/scales in as the container reaches destination

---

## Lexicon's TransitionOverlay

Located at `design-system/src/commonMain/kotlin/overlay/transition/TransitionOverlay.kt`.

### Architecture

```
OverlayHost
  └── TransitionOverlay(sourceRect, content)
        ├── Scrim (0% -> 32% black, first half)
        ├── Container (morphs position, size, corner radius)
        │     └── Content (window-reveal technique, full-size clipped by container)
        └── progress: Animatable(0f -> 1f)
```

### How It Works

1. Source element captures its **window position** via `onGloballyPositioned`
2. Overlay measures its own window position to compute relative coordinates
3. `Animatable` drives `progress` from 0 to 1 (enter) or 1 to 0 (exit)
4. Every frame: container position/size/corner = `lerp(source, fullScreen, progress)`
5. Content is rendered at **full screen size**, offset into the container, and **clipped** by the morphing shape — this is the "window reveal" technique

### Timing

| Phase | Duration | Easing |
|-------|----------|--------|
| Enter | 500ms | `CubicBezierEasing(0.05, 0.7, 0.1, 1.0)` — emphasized decelerate |
| Exit | 350ms | `FastOutSlowInEasing` — standard |
| Scrim fade-in | First 50% of enter | `smoothStep(0, 0.5, progress)` |
| Surface fill | 5%-35% of enter | `smoothStep(0.05, 0.35, progress)` |
| Content reveal | 12%-50% of enter | `smoothStep(0.12, 0.5, progress)` |

### Usage

```kotlin
// 1. Capture source rect
var sourceRect by remember { mutableStateOf(Rect.Zero) }

Card(
    modifier = Modifier.onGloballyPositioned { coords ->
        val pos = coords.positionInWindow()
        sourceRect = Rect(
            left = pos.x,
            top = pos.y,
            right = pos.x + coords.size.width.toFloat(),
            bottom = pos.y + coords.size.height.toFloat()
        )
    },
    onClick = {
        overlayHost.show(
            TransitionOverlay(
                sourceRect = sourceRect,
                sourceCornerRadiusDp = 12f
            ) { progress, navigator ->
                // progress: 0f (collapsed at source) -> 1f (full screen)
                DetailScreen(
                    transitionProgress = progress,
                    onDismiss = { navigator.dismiss() }
                )
            }
        )
    }
)
```

### Choreography Inside the Transform

Use `smoothStep` to stagger child elements based on `transitionProgress`:

```kotlin
@Composable
fun DetailScreen(transitionProgress: Float, onDismiss: () -> Unit) {
    // Hero image appears first
    val heroAlpha = smoothStep(0.15f, 0.5f, transitionProgress)

    // Title slides up slightly after hero
    val titleAlpha = smoothStep(0.25f, 0.6f, transitionProgress)
    val titleSlide = lerp(20f, 0f, smoothStep(0.25f, 0.6f, transitionProgress))

    // Body content last
    val bodyAlpha = smoothStep(0.4f, 0.75f, transitionProgress)

    // Buttons appear last
    val buttonsAlpha = smoothStep(0.55f, 0.85f, transitionProgress)
    val buttonsSlide = lerp(40f, 0f, smoothStep(0.55f, 0.85f, transitionProgress))

    Column(Modifier.fillMaxSize()) {
        HeroImage(Modifier.graphicsLayer { alpha = heroAlpha })
        Title(Modifier.graphicsLayer { alpha = titleAlpha; translationY = titleSlide })
        Body(Modifier.graphicsLayer { alpha = bodyAlpha })
        Buttons(Modifier.graphicsLayer { alpha = buttonsAlpha; translationY = buttonsSlide })
    }
}
```

---

## Building Custom Container Transforms

When `TransitionOverlay` doesn't fit (e.g., non-fullscreen destination, card-to-card):

### Step 1: Capture source and destination bounds

```kotlin
var sourceBounds by remember { mutableStateOf(Rect.Zero) }
var destBounds by remember { mutableStateOf(Rect.Zero) }

// Source
SourceElement(
    modifier = Modifier.onGloballyPositioned { coords ->
        val pos = coords.positionInWindow()
        sourceBounds = Rect(pos.x, pos.y, pos.x + coords.size.width, pos.y + coords.size.height)
    }
)
```

### Step 2: Animate progress

```kotlin
val progress = remember { Animatable(0f) }

LaunchedEffect(isExpanded) {
    if (isExpanded) {
        progress.animateTo(1f, tween(500, easing = EmphasizedDecelerateEasing))
    } else {
        progress.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
    }
}
```

### Step 3: Render morphing container

```kotlin
val p = progress.value

Box(
    modifier = Modifier
        .offset { IntOffset(
            lerp(sourceBounds.left, destBounds.left, p).toInt(),
            lerp(sourceBounds.top, destBounds.top, p).toInt()
        )}
        .requiredSize(
            width = with(density) { lerp(sourceBounds.width, destBounds.width, p).toDp() },
            height = with(density) { lerp(sourceBounds.height, destBounds.height, p).toDp() }
        )
        .clip(RoundedCornerShape(lerp(sourceCorner, destCorner, p).dp))
        .background(MaterialTheme.colorScheme.surface)
) {
    // Cross-fade content
    SourceContent(Modifier.graphicsLayer { alpha = 1f - smoothStep(0.1f, 0.4f, p) })
    DestContent(Modifier.graphicsLayer { alpha = smoothStep(0.3f, 0.7f, p) })
}
```

---

## Window Reveal vs Scale

**Window reveal** (what TransitionOverlay uses): content is rendered at full destination size, clipped by the expanding container. Looks polished because text/images don't distort.

**Scale** (simpler but lower quality): content scales from source size to destination size. Text and images stretch. Only use for simple icons or uniform shapes.

```kotlin
// Window reveal (preferred)
Box(modifier = Modifier.requiredSize(containerW, containerH).clip(shape)) {
    Box(
        modifier = Modifier
            .requiredSize(destW, destH)          // full destination size
            .offset { IntOffset(-offsetX, -offsetY) }  // align with screen
    ) {
        DestinationContent()
    }
}

// Scale (simple, lower quality)
Box(
    modifier = Modifier
        .requiredSize(containerW, containerH)
        .graphicsLayer {
            scaleX = containerW / destW
            scaleY = containerH / destH
        }
) {
    DestinationContent()
}
```

---

## Shared Element Transitions (Compose 1.7+)

For navigation-level shared element transitions:

```kotlin
SharedTransitionLayout {
    AnimatedContent(targetState = showDetail) { isDetail ->
        if (isDetail) {
            DetailScreen(
                modifier = Modifier.sharedElement(
                    state = rememberSharedContentState(key = "card-$id"),
                    animatedVisibilityScope = this@AnimatedContent
                )
            )
        } else {
            ListScreen(
                modifier = Modifier.sharedElement(
                    state = rememberSharedContentState(key = "card-$id"),
                    animatedVisibilityScope = this@AnimatedContent
                )
            )
        }
    }
}
```

**Note:** SharedTransitionLayout is experimental and may not be stable in Compose Multiplatform yet. Prefer `TransitionOverlay` for production code.

---

## Common Pitfalls

1. **Don't animate `Modifier.size()`** for the container — use `Modifier.requiredSize()` with `graphicsLayer` or manual sizing. Regular `size()` can be overridden by parent constraints.
2. **Window coordinates shift** when the keyboard appears or system bars change — re-capture source rect if needed.
3. **Camera distance** for 3D transforms: default is too close. Use `graphicsLayer { cameraDistance = 12.dp.toPx() }`.
4. **Scrim must be behind the container** in z-order, not on top of existing content.
5. **Exit animation should be faster** than enter (350ms vs 500ms) — users want to get back quickly.
