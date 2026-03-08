package overlay.bottomsheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
 */
@Composable
fun <T> BottomSheetPages(
    navigator: BottomSheetPageNavigator<T>,
    label: String = "BottomSheetPages",
    content: @Composable (T) -> Unit
) {
    BackHandler(enabled = navigator.canNavigateBack) {
        navigator.navigateBack()
    }

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
