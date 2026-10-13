package components.haptics

import androidx.compose.runtime.Composable

/**
 * A strong "thunk" for moments that should be felt, like a control landing on a stop. Compose's
 * built-in haptic types top out at a medium impact on iOS, so this goes to the platform. Respects
 * the system's haptics setting; does nothing where there is no haptic engine.
 */
@Composable
expect fun rememberHeavyHaptic(): () -> Unit
