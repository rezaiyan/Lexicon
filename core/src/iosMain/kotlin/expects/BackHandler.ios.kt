@file:OptIn(ExperimentalComposeUiApi::class)

package expects

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler as ComposeBackHandler

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    ComposeBackHandler(enabled = enabled, onBack = onBack)
}

