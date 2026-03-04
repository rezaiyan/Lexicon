---
name: design-system
description: Use and extend Lexicon's design system — theme tokens, reusable components, brand colors, and rules for when components belong in design-system vs presentation
argument-hint: "<component-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
---

# Lexicon Design System

Use this skill when creating UI components or working with theming.

## Design System First

Before creating any UI component, **always check the design-system module first** (`design-system/src/commonMain/kotlin/`):
- Search existing components in `components/` — reuse before creating new ones
- Search existing theme tokens in `theme/AppTheme.kt` — never invent new spacing/color/shape values

## Theme Access

Always use the `Theme` object — never hardcode values:

```kotlin
val spacing = Theme.spacing       // xxxs(2dp) xs(4dp) s(8dp) m(16dp) l(24dp) xl(32dp) xxl(40dp) xxxl(48dp)
val dimensions = Theme.dimensions // iconSmall(16dp) iconMedium(24dp) iconLarge(32dp) touchTarget(48dp)
val shapes = Theme.shapes         // extraSmall(4dp) small(8dp) medium(12dp) large(16dp) pill(100dp)
val elevation = Theme.elevation   // none(0dp) low(1dp) medium(4dp) high(8dp) modal(12dp)
val motion = Theme.motion         // instant(100ms) fast(200ms) normal(300ms) slow(500ms)
val semantic = Theme.semanticColors  // success/warning/info + container variants
val gradients = Theme.gradients   // premiumHero, primaryWash, surfaceFade
```

## Brand Colors

- Primary: `#7F5AF0` (Purple)
- Secondary: `#2CB67D` (Green)
- Tertiary: `#FF8906` (Orange)
- Error: `#E53170` (Red)

## Available Components

- `LoadingScreen(modifier, message?)` — centered spinner
- `ErrorScreen(message, title?, icon?, onRetry?)` — full error with retry
- `EmptyScreen(title, subtitle?, icon?)` — empty state
- `ListTile(icon, title, onClick, subtitle?, trailingContent?)` — settings/nav rows
- `Pill(text, color)` / `CounterPill(text, color?)` — badges
- `AnimatedProgressBar(progress, color)` / `GradientProgressBar(progress, gradientColors)` — progress
- `SectionHeader(title)` — section dividers

## When to Add to design-system

Add to `design-system/src/commonMain/kotlin/components/` when:
- Component is **reusable across 2+ screens**
- Takes content/data via parameters with no domain knowledge
- References `Theme.*` tokens but never imports from `domain/` or `data/`
- Examples: buttons, cards, inputs, badges, progress indicators, status screens

## When to Keep in presentation

Keep in the screen's package under `presentation/` when:
- Component is tightly coupled to a specific ViewModel or domain model
- Composable calls `koinViewModel` or accesses `OverlayHost`
- One-off layout only used in a single screen

## Platform

- Window insets: `Modifier.safeContentPadding()`, `.navigationBarsPadding()`
- Platform fonts: `platformFontFamily()` — already handled by theme
- Touch targets: minimum 48dp (`Theme.dimensions.touchTarget`)

## Checklist

1. Checked existing components before creating new ones
2. All values from `Theme.*` — no hardcoded dp/colors/durations
3. Shared components in `design-system/` — not duplicated in presentation
4. No `domain/` or `data/` imports in design-system components
