package components.toast

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun ToastHost(state: ToastState, modifier: Modifier) = ToastPill(state, modifier)
