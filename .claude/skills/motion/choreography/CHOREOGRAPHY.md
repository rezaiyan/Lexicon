# Animation Choreography

Deep reference for coordinating multiple elements within a single transition — staggered entrances, sequenced reveals, and orchestrated multi-element animations.

## Principles

1. **Hero first** — the most important element animates first; supporting elements follow
2. **Cascade outward** — elements farther from the hero animate later
3. **Overlap timing** — elements should overlap by ~30-50%; pure sequential feels sluggish
4. **Consistent direction** — all elements should share a coherent motion direction
5. **Fast out, slow settle** — elements move quickly to approximate position, then settle gently

---

## Technique 1: smoothStep Windows

Map a single `progress` (0-1) to per-element windows using `smoothStep`. Each element fades/slides within its own sub-range of the overall progress.

```kotlin
/** Hermite smoothstep — smooth S-curve mapping [edge0..edge1] to [0..1]. */
fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
```

### Layout pattern

```
Overall progress:  0.0 ─────────── 0.5 ─────────── 1.0
                   │                │                │
Hero image:        [███████████]    │                │    0.0 - 0.45
Title:              [██████████]   │                │    0.1 - 0.50
Subtitle:             [█████████]  │                │    0.2 - 0.55
Body:                   [████████████]              │    0.3 - 0.65
Actions:                      [██████████████]      │    0.45 - 0.80
```

### Implementation

```kotlin
@Composable
fun ChoreographedContent(progress: Float) {
    val heroAlpha = smoothStep(0.0f, 0.45f, progress)
    val titleAlpha = smoothStep(0.1f, 0.50f, progress)
    val titleSlide = lerp(24f, 0f, smoothStep(0.1f, 0.50f, progress))
    val subtitleAlpha = smoothStep(0.2f, 0.55f, progress)
    val bodyAlpha = smoothStep(0.3f, 0.65f, progress)
    val actionsAlpha = smoothStep(0.45f, 0.80f, progress)
    val actionsSlide = lerp(32f, 0f, smoothStep(0.45f, 0.80f, progress))

    Column {
        Hero(Modifier.graphicsLayer { alpha = heroAlpha })
        Title(Modifier.graphicsLayer { alpha = titleAlpha; translationY = titleSlide })
        Subtitle(Modifier.graphicsLayer { alpha = subtitleAlpha })
        Body(Modifier.graphicsLayer { alpha = bodyAlpha })
        Actions(Modifier.graphicsLayer { alpha = actionsAlpha; translationY = actionsSlide })
    }
}
```

### Real example: ReviewComponents.kt

The review screen choreographs 7+ elements within a single container transform:

```
Element              Window        Effect
────────────────────────────────────────────────
Card                 0.20 - 0.55  scale 0.88->1, slideY 20->0
Close button         0.35 - 0.60  fade + slideX -24->0
Title                0.40 - 0.65  fade + slideY -16->0
Counter pill         0.45 - 0.70  fade + scale 0.6->1
Progress bar         0.50 - 0.75  fade + scaleX from left origin
Edit button          0.50 - 0.78  fade + slideY 32->0
Rating label         0.55 - 0.82  fade + slideY 12->0
Rating buttons       0.60 - 0.88  fade + slideY + scale 0.92->1
```

---

## Technique 2: Staggered Entrance for Lists

Use index-based delay for list items appearing one after another.

### Using Modifier.staggeredFadeSlide

Existing utility in `design-system/src/commonMain/kotlin/components/animation/StaggeredFadeSlide.kt`:

```kotlin
Column {
    items.forEachIndexed { index, item ->
        ItemRow(
            item = item,
            modifier = Modifier.staggeredFadeSlide(index, baseDelayMs = 55)
        )
    }
}
```

Each item: 55ms delay per index, then 320ms fade+slide with `FastOutSlowInEasing`.

### Manual stagger with LaunchedEffect

For more control over individual item animations:

```kotlin
@Composable
fun StaggeredList(items: List<Item>) {
    val alphas = remember(items.size) { items.map { Animatable(0f) } }
    val offsets = remember(items.size) { items.map { Animatable(24f) } }

    LaunchedEffect(items) {
        items.forEachIndexed { index, _ ->
            launch {
                delay(index * 50L)
                launch { alphas[index].animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
                launch { offsets[index].animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
            }
        }
    }

    Column {
        items.forEachIndexed { index, item ->
            ItemRow(
                item = item,
                modifier = Modifier.graphicsLayer {
                    alpha = alphas[index].value
                    translationY = offsets[index].value
                }
            )
        }
    }
}
```

### Stagger in LazyColumn

For lazy lists, trigger per-item animation on first appearance:

```kotlin
LazyColumn {
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        val alpha = remember { Animatable(0f) }
        val offsetY = remember { Animatable(20f) }

        LaunchedEffect(Unit) {
            // Small delay based on visible index (not data index)
            delay(index.coerceAtMost(10) * 40L)
            launch { alpha.animateTo(1f, tween(250)) }
            launch { offsetY.animateTo(0f, tween(250)) }
        }

        ItemRow(
            item = item,
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value
            }
        )
    }
}
```

---

## Technique 3: updateTransition for State-Based Choreography

When multiple properties need to animate together based on a state change:

```kotlin
enum class CardState { Collapsed, Expanded }

@Composable
fun ExpandableCard(state: CardState) {
    val transition = updateTransition(state, label = "card")

    val height by transition.animateDp(
        transitionSpec = {
            when {
                CardState.Collapsed isTransitioningTo CardState.Expanded ->
                    tween(400, easing = FastOutSlowInEasing)
                else ->
                    tween(300, easing = FastOutSlowInEasing)
            }
        },
        label = "height"
    ) { if (it == CardState.Expanded) 300.dp else 80.dp }

    val cornerRadius by transition.animateDp(
        transitionSpec = { tween(400) },
        label = "corner"
    ) { if (it == CardState.Expanded) 0.dp else 16.dp }

    val elevation by transition.animateDp(
        transitionSpec = { tween(300) },
        label = "elevation"
    ) { if (it == CardState.Expanded) 12.dp else 2.dp }

    val contentAlpha by transition.animateFloat(
        transitionSpec = {
            when {
                CardState.Collapsed isTransitioningTo CardState.Expanded ->
                    tween(300, delayMillis = 150) // content fades in AFTER height expands
                else ->
                    tween(200)  // content fades out first
            }
        },
        label = "contentAlpha"
    ) { if (it == CardState.Expanded) 1f else 0f }

    // ...
}
```

---

## Technique 4: Parallel Coroutine Choreography

Use coroutine `launch` blocks for precise sequencing:

```kotlin
LaunchedEffect(trigger) {
    // Phase 1: fade out old content
    launch { oldContentAlpha.animateTo(0f, tween(150)) }
    launch { oldContentScale.animateTo(0.95f, tween(150)) }

    delay(100) // overlap: start phase 2 before phase 1 finishes

    // Phase 2: morph container
    launch { containerHeight.animateTo(newHeight, tween(350, easing = EmphasizedEasing)) }
    launch { containerCorner.animateTo(newCorner, tween(350, easing = EmphasizedEasing)) }

    delay(150) // overlap again

    // Phase 3: fade in new content
    launch { newContentAlpha.animateTo(1f, tween(250)) }
    launch { newContentOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
}
```

---

## Timing Guidelines

| # Elements | Total duration | Per-element stagger |
|------------|---------------|-------------------|
| 2-3 | 300-400ms | 60-80ms |
| 4-6 | 400-600ms | 50-70ms |
| 7-10 | 500-800ms | 40-60ms |
| 10+ (list) | 600-1000ms | 30-55ms |

**Rule:** total stagger time = `(n-1) * stagger_delay + element_duration`. Keep total under 1200ms for any transition.

---

## Anti-Patterns

1. **Pure sequential** — waiting for each element to finish before starting the next feels slow. Always overlap by 30-50%.
2. **All at once** — animating everything simultaneously looks chaotic. Stagger creates visual hierarchy.
3. **Inconsistent direction** — elements sliding in from different directions feels disjointed. Pick one direction.
4. **Too many moving parts** — if more than ~8 elements animate independently, group some together.
5. **Animating during exit** — exit animations should be simpler and faster than enter. Users want to leave quickly.
