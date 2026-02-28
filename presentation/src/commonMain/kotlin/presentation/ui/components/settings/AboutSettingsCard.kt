package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import expects.openUrl
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.about
import lexicon.resources.generated.resources.version_format

private val AboutIconColor = Color(0xFF78909C)

@Composable
fun AboutSettingsCard(appVersion: String) {
    var clickCount by remember { mutableStateOf(0) }
    LaunchedEffect(clickCount) {
        if (clickCount > 0) {
            delay(2000)
            clickCount = 0
        }
    }

    SettingsCard(
        icon = Icons.Default.Info,
        title = stringResource(Res.string.about),
        subtitle = stringResource(Res.string.version_format, appVersion),
        iconBackgroundColor = AboutIconColor,
        showTrailingArrow = false,
        onClick = {
            clickCount++
            if (clickCount >= 3) {
                openUrl("https://alirezaiyan.com/lexicon/")
                clickCount = 0
            }
        }
    )
}
