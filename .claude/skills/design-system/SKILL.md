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

## Visual Style Philosophy

### Content-First

The UI chrome stays calm and neutral — user content (words, progress, achievements) is the visual focus:
- Accent colors used **sparingly** — primary color appears almost exclusively on the single primary CTA per screen
- Background and surface colors do the heavy lifting; color is reserved for meaning
- Let imagery, data, and user content carry the visual richness — not the interface frame

### Whitespace as Design Element

Whitespace is a first-class design element, not empty filler. Every content block needs breathing room:
- `Theme.spacing.lg` (24dp) between content sections
- `Theme.spacing.md` (16dp) within cards and between related items
- Screen horizontal margins: `Theme.spacing.md` (16dp) on phone, `Theme.spacing.lg` (24dp) on tablet
- Between heading and its content: `Theme.spacing.xs` (8dp) to `Theme.spacing.sm` (12dp)
- Generous spacing reduces cognitive load — users scan content quickly without fatigue

### Surface Hierarchy

Layer surfaces with distinct background/elevation to communicate depth:

| Layer | Background | Elevation | Usage |
|-------|-----------|-----------|-------|
| **Base** | `colorScheme.background` | none | Page background |
| **Surface** | `colorScheme.surface` | `Theme.elevation.low` (1dp) | Cards, content areas |
| **Raised** | `colorScheme.surface` | `Theme.elevation.high` (4dp) | Bottom sheets, floating search |
| **Overlay** | `colorScheme.surface` | `Theme.elevation.modal` (12dp) | Modals, dialogs |
| **Scrim** | Black at 32% opacity | — | Behind modals/sheets |

Shadows are **soft and subtle** — never harsh or dramatic. Cards at rest use very subtle shadows (1dp). Shadows deepen on interaction (press/focus) to provide feedback.

### Card Design

Cards are the primary content container — the signature building block:

```kotlin
Card(
    shape = RoundedCornerShape(Theme.shapes.medium),     // 12dp — friendly, consistent
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.low),
    modifier = Modifier.padding(horizontal = Theme.spacing.md),
) {
    Column(modifier = Modifier.padding(Theme.spacing.md)) {   // 16dp internal padding
        // content
    }
}
```

Rules:
- Internal padding: `Theme.spacing.md` (16dp)
- Border radius: `Theme.shapes.medium` (12dp) — all cards use the same radius
- Shadow: `Theme.elevation.low` (1dp) at rest — subtle, not dramatic
- Space between cards in lists: `Theme.spacing.md` (16dp) to `Theme.spacing.lg` (24dp)
- Images inside cards: clip with same 12dp radius, `ContentScale.Crop`
- Pressed state: slight scale reduction (0.96-0.98x) via `graphicsLayer` for tactile feedback

### Button Hierarchy

| Type | Style | When |
|------|-------|------|
| **Primary** | Filled, pill shape (`Theme.shapes.pill`), primary color | Single most important action per screen |
| **Secondary** | Outlined, pill shape, `onSurface` border | Alternative actions |
| **Tertiary** | Filled tonal, pill shape | Less prominent actions |
| **Text** | Underlined or plain text, no background | Inline links, minor actions |

Rules:
- **Only ONE primary (filled) CTA per screen** — color scarcity directs attention
- Minimum touch target: `Theme.dimensions.touchTarget` (48dp)
- Pill shape (`Theme.shapes.pill`) for all buttons — visually distinct from content cards
- Vertical padding: 14dp, horizontal padding: 24dp
- Pressed state: darken + scale(0.97f) via `graphicsLayer`

### Typography Usage

Use hierarchy through weight and color, not excessive size jumps:

| Role | Style | Weight | Color | Max per screen |
|------|-------|--------|-------|---------------|
| **Page title** | `headlineLarge`/`headlineMedium` | Bold/ExtraBold | `onSurface` | 1 |
| **Section header** | `titleLarge`/`titleMedium` | Bold | `onSurface` | 2-3 |
| **Card title** | `titleSmall`/`bodyLarge` | SemiBold/Medium | `onSurface` | Per card |
| **Body text** | `bodyLarge` (16sp) / `bodyMedium` (14sp) | Normal | `onSurface` | — |
| **Secondary text** | `bodySmall`/`labelMedium` | Normal | `onSurfaceVariant` | — |
| **Caption** | `labelSmall` (11sp) | Normal | `onSurfaceVariant` | — |

Rule: Maximum 3 distinct font sizes per visible screen section. Create hierarchy through **weight** and **color** before reaching for size changes.

### Dividers

- Hairline: `Theme.dimensions.dividerHairline` (0.5dp), `outlineVariant` color
- Space above and below: `Theme.spacing.lg` (24dp) — dividers are visual breaths, not just lines
- Never stack heavy visual elements — dividers are subtle separators between content sections

### Image Treatment

- Clip all images: `Modifier.clip(RoundedCornerShape(Theme.shapes.medium))` (12dp)
- Use `ContentScale.Crop` for card/listing images
- Preferred aspect ratios: 3:2 (cards), 16:9 (hero/cover)
- Loading state: shimmer placeholder animation (see motion skill)

### Loading & Empty States

- Shimmer placeholders match the shape of the content they replace (cards, text lines)
- Empty states: centered icon + title + subtitle — never bare text
- Loading spinners: centered with optional message, no jarring layout shifts
- Skeleton screens preferred over spinners for content-heavy screens

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
