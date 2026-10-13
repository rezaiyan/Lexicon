package components.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

@Composable
actual fun rememberHeavyHaptic(): () -> Unit {
    val generator = remember {
        UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy).apply { prepare() }
    }
    return remember(generator) {
        {
            generator.impactOccurred()
            // Keeps the Taptic Engine warm so the next snap lands without lag
            generator.prepare()
        }
    }
}
