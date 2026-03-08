# Layout Animation

Deep reference for animating layout changes — content switching, size changes, list item reordering, expand/collapse, and lazy list animations.

## AnimatedContent: Content Switching

The most powerful high-level animation composable. Animates between different content based on a state key.

### Basic usage

```kotlin
AnimatedContent(
    targetState = currentPage,
    transitionSpec = {
        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
    },
    label = "page-switch"
) { page ->
    when (page) {
        Page.Home -> HomeContent()
        Page.Detail -> DetailContent()
    }
}
```

### Direction-aware transitions

From `OnboardingScreen.kt` — forward slides right-to-left, backward slides left-to-right:

```kotlin
AnimatedContent(
    targetState = step,
    transitionSpec = {
        val forward = targetState > initialState
        ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = tween(300),
                initialOffsetX = { if (forward) it else -it }
            ) + fadeIn(tween(300)),
            initialContentExit = slideOutHorizontally(
                animationSpec = tween(300),
                targetOffsetX = { if (forward) -it else it }
            ) + fadeOut(tween(300))
        )
    },
    label = "step"
) { currentStep -> ... }
```

### Fade-through (M3 pattern for unrelated content)

```kotlin
transitionSpec = {
    val enterDelay = 150
    val exitDuration = 150

    (fadeIn(tween(150, delayMillis = enterDelay)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(150, delayMillis = enterDelay)))
        .togetherWith(
            fadeOut(tween(exitDuration)) +
            scaleOut(targetScale = 1.08f, animationSpec = tween(exitDuration))
        )
}
```

### SizeTransform

Control how AnimatedContent handles size changes:

```kotlin
AnimatedContent(
    targetState = expanded,
    transitionSpec = {
        fadeIn() togetherWith fadeOut() using SizeTransform { initialSize, targetSize ->
            // Animate width first, then height
            if (targetState) {
                keyframes {
                    IntSize(targetSize.width, initialSize.height) at 150
                    IntSize(targetSize.width, targetSize.height) at 300
                }
            } else {
                keyframes {
                    IntSize(initialSize.width, targetSize.height) at 150
                    IntSize(targetSize.width, targetSize.height) at 300
                }
            }
        }
    }
)
```

---

## AnimatedVisibility: Enter/Exit

### Standard patterns

```kotlin
val motion = Theme.motion

// Expand from top
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
)

// Slide from bottom (bottom sheet feel)
enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()

// Scale + fade (dialog feel)
enter = scaleIn(initialScale = 0.85f) + fadeIn()
exit = scaleOut(targetScale = 0.85f) + fadeOut()
```

### Accessing transition state inside AnimatedVisibility

```kotlin
AnimatedVisibility(visible = show) {
    // `transition` is available inside AnimatedVisibility scope
    val bgAlpha by transition.animateFloat(label = "bg") { state ->
        if (state == EnterExitState.Visible) 1f else 0f
    }

    Box(Modifier.graphicsLayer { alpha = bgAlpha }) {
        Content()
    }
}
```

---

## animateContentSize

Smoothly animates a composable's size when its content changes:

```kotlin
Column(
    modifier = Modifier.animateContentSize(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
) {
    Text("Title")
    if (expanded) {
        Text("Additional details that make the column taller...")
    }
}
```

**Use when:** a single composable's content changes size (expanding text, showing/hiding children).

**Don't use when:** you need to animate enter/exit of the content itself — use `AnimatedVisibility` instead.

---

## LazyList Animations

### Item appearance animation

```kotlin
LazyColumn {
    items(list, key = { it.id }) { item ->
        val alpha = remember { Animatable(0f) }
        val offsetY = remember { Animatable(16f) }

        LaunchedEffect(Unit) {
            launch { alpha.animateTo(1f, tween(250)) }
            launch { offsetY.animateTo(0f, tween(250)) }
        }

        ItemRow(
            item = item,
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = alpha.value
                    translationY = offsetY.value
                }
                .animateItem()  // Compose 1.7+ reorder animation
        )
    }
}
```

### Modifier.animateItem() (Compose 1.7+)

Animates item placement changes (reorder, insert, remove):

```kotlin
LazyColumn {
    items(sortedList, key = { it.id }) { item ->
        ItemRow(
            item = item,
            modifier = Modifier.animateItem(
                fadeInSpec = tween(300),
                fadeOutSpec = tween(200),
                placementSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        )
    }
}
```

### Swipe-to-dismiss in LazyColumn

```kotlin
LazyColumn {
    items(list, key = { it.id }) { item ->
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete(item)
                    true
                } else false
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            },
            modifier = Modifier.animateItem()
        ) {
            ItemRow(item)
        }
    }
}
```

---

## Expand/Collapse Patterns

### Section with rotate-on-expand icon

```kotlin
@Composable
fun ExpandableSection(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val motion = Theme.motion

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(motion.durationMedium, easing = motion.easingStandard),
        label = "chevron"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(Theme.spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                tween(motion.durationMedium, easing = motion.easingDecelerate),
                expandFrom = Alignment.Top
            ) + fadeIn(tween(motion.durationShort2)),
            exit = shrinkVertically(
                tween(motion.durationShort2, easing = motion.easingAccelerate),
                shrinkTowards = Alignment.Top
            ) + fadeOut(tween(motion.durationXShort))
        ) {
            content()
        }
    }
}
```

### Height animation with Animatable

For more control than `AnimatedVisibility`:

```kotlin
var contentHeight by remember { mutableIntStateOf(0) }
val heightAnim = remember { Animatable(0f) }

LaunchedEffect(expanded) {
    heightAnim.animateTo(
        if (expanded) contentHeight.toFloat() else 0f,
        spring(stiffness = Spring.StiffnessMediumLow)
    )
}

Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(with(LocalDensity.current) { heightAnim.value.toDp() })
        .clipToBounds()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { contentHeight = it.height }
    ) {
        ExpandedContent()
    }
}
```

---

## Crossfade

Simplest content-switching animation — only opacity, no size/position changes:

```kotlin
Crossfade(
    targetState = isLoading,
    animationSpec = tween(Theme.motion.durationMedium),
    label = "loading-crossfade"
) { loading ->
    if (loading) LoadingIndicator() else Content()
}
```

**Use when:** two states are exactly the same size and position. Simpler than AnimatedContent.

---

## Performance Notes

1. **Always provide `key`** for LazyColumn/LazyRow items — without keys, animations can't track items
2. **`animateContentSize` triggers relayout** — fine for occasional size changes, not for per-frame animation
3. **`AnimatedVisibility` is cheaper than `AnimatedContent`** when you only need enter/exit
4. **Avoid nesting `AnimatedContent` inside `AnimatedContent`** — use a single AnimatedContent with a combined state
5. **`Crossfade` allocates less** than `AnimatedContent` — prefer it for simple opacity switches
6. **Use `clipToBounds()`** on animated size containers to prevent content from drawing outside bounds during animation
