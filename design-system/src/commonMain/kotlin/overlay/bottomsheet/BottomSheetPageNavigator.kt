package overlay.bottomsheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import components.dialog.ContentToolbar
import expects.BackHandler
import theme.Theme

/**
 * Manages a back stack of pages within a single bottom sheet.
 *
 * Navigate forward with [navigateTo], back with [navigateBack].
 * System back press automatically navigates back through the stack
 * before falling through to the sheet's own back handler (dismiss).
 *
 * ```
 * val pages = rememberBottomSheetPageNavigator<Page>(Page.First)
 * overlayHost.showDynamicBottomSheet { nav ->
 *     BottomSheetPages(pages) { current ->
 *         when (current) {
 *             Page.First  -> FirstContent(onNext = { pages.navigateTo(Page.Second) })
 *             Page.Second -> SecondContent(onDone = { nav.dismiss() })
 *         }
 *     }
 * }
 * ```
 */
@Stable
class BottomSheetPageNavigator<T>(initial: T) {
    private val backStack = mutableListOf(initial).toMutableStateList()

    /** True when the last navigation was forward (navigateTo), false for back. */
    var isNavigatingForward: Boolean by mutableStateOf(true)
        private set

    val currentPage: T get() = backStack.last()

    val canNavigateBack: Boolean get() = backStack.size > 1

    fun navigateTo(page: T) {
        isNavigatingForward = true
        backStack.add(page)
    }

    fun navigateBack(): Boolean {
        if (backStack.size > 1) {
            isNavigatingForward = false
            backStack.removeLast()
            return true
        }
        return false
    }
}

@Composable
fun <T> rememberBottomSheetPageNavigator(initial: T): BottomSheetPageNavigator<T> {
    return remember { BottomSheetPageNavigator(initial) }
}

private const val SLIDE_OFFSET_DIVISOR = 4

/**
 * Animated page transitions within a single bottom sheet, with back stack.
 *
 * System back navigates through the stack before dismissing the sheet.
 * Pages slide horizontally (forward = left-to-right push, back = right-to-left pop)
 * with crossfade, matching the design system's sheet motion language.
 *
 * A persistent [ContentToolbar] is rendered above the animated content with
 * back/close buttons driven by [pageConfig].
 * When the enclosing overlay provides [LocalBottomSheetOverlayScope], per-page
 * [BottomSheetPageConfig.properties] are auto-synced — no `LaunchedEffect` needed at call sites.
 *
 * **Nesting support:** When a `BottomSheetPages` is rendered inside another
 * `BottomSheetPages`, the inner instance suppresses its own toolbar and
 * registers its toolbar state with the outer (root) instance via
 * [LocalBottomSheetToolbarOwner]. The root toolbar shows the inner state when
 * present, falling back to its own state. This prevents double toolbars.
 */
@Composable
fun <T> BottomSheetPages(
    navigator: BottomSheetPageNavigator<T>,
    onClose: (() -> Unit)? = null,
    label: String = "BottomSheetPages",
    pageConfig: (T) -> BottomSheetPageConfig = { BottomSheetPageConfig() },
    content: @Composable (T) -> Unit
) {
    BackHandler(enabled = navigator.canNavigateBack) {
        navigator.navigateBack()
    }

    val currentPage = navigator.currentPage
    val config = pageConfig(currentPage)

    // Auto-sync overlay properties when pageConfig provides them
    val overlayScope = LocalBottomSheetOverlayScope.current
    LaunchedEffect(currentPage) {
        val props = pageConfig(currentPage).properties
        if (props != null && overlayScope != null) {
            overlayScope.properties = props
        }
    }

    // Resolve toolbar callbacks for this navigator
    val resolvedBack: (() -> Unit)? =
        if (navigator.canNavigateBack && config.showBackButton) {{ navigator.navigateBack() }} else null
    val resolvedClose: (() -> Unit)? =
        if (config.showCloseButton && onClose != null) onClose else null

    val parentOwner = LocalBottomSheetToolbarOwner.current

    if (parentOwner != null) {
        // NESTED: update parent owner with our toolbar state, suppress our toolbar
        SideEffect {
            parentOwner.innerBack = resolvedBack
            parentOwner.innerClose = resolvedClose
        }

        DisposableEffect(Unit) {
            onDispose {
                parentOwner.innerBack = null
                parentOwner.innerClose = null
            }
        }

        AnimatedPageContent(navigator, label, content)
    } else {
        // ROOT: own the toolbar, provide owner to children
        val toolbarOwner = remember { BottomSheetToolbarOwner() }

        // Inner state takes priority over outer
        val finalBack = toolbarOwner.innerBack ?: resolvedBack
        val finalClose = toolbarOwner.innerClose ?: resolvedClose

        Column {
            ContentToolbar(onBack = finalBack, onClose = finalClose)

            CompositionLocalProvider(LocalBottomSheetToolbarOwner provides toolbarOwner) {
                AnimatedPageContent(navigator, label, content)
            }
        }
    }
}

@Composable
private fun <T> AnimatedPageContent(
    navigator: BottomSheetPageNavigator<T>,
    label: String,
    content: @Composable (T) -> Unit,
) {
    val motion = Theme.motion
    val forward = navigator.isNavigatingForward

    AnimatedContent(
        targetState = navigator.currentPage,
        transitionSpec = {
            val enterSlide = slideInHorizontally(
                initialOffsetX = { fullWidth ->
                    if (forward) fullWidth / SLIDE_OFFSET_DIVISOR else -fullWidth / SLIDE_OFFSET_DIVISOR
                },
                animationSpec = tween(motion.durationMedium2, easing = motion.easingDecelerate)
            ) + fadeIn(tween(motion.durationMedium, easing = motion.easingDecelerate))

            val exitSlide = slideOutHorizontally(
                targetOffsetX = { fullWidth ->
                    if (forward) -fullWidth / SLIDE_OFFSET_DIVISOR else fullWidth / SLIDE_OFFSET_DIVISOR
                },
                animationSpec = tween(motion.durationMedium2, easing = motion.easingAccelerate)
            ) + fadeOut(tween(motion.durationShort, easing = motion.easingAccelerate))

            (enterSlide togetherWith exitSlide).using(SizeTransform(clip = false))
        },
        label = label
    ) { page ->
        content(page)
    }
}

/** Simple overload without back stack — just animated page swaps. */
@Composable
fun <T> BottomSheetPages(
    currentPage: T,
    label: String = "BottomSheetPages",
    content: @Composable (T) -> Unit
) {
    val motion = Theme.motion
    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            (fadeIn(tween(motion.durationMedium, easing = motion.easingDecelerate))
                togetherWith fadeOut(tween(motion.durationShort, easing = motion.easingAccelerate)))
                .using(SizeTransform(clip = false))
        },
        label = label
    ) { page ->
        content(page)
    }
}
