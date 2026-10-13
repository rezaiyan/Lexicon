package components.haptics

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberHeavyHaptic(): () -> Unit {
    val view = LocalView.current
    // LONG_PRESS is the strongest effect a view can request without the VIBRATE permission
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } }
}
