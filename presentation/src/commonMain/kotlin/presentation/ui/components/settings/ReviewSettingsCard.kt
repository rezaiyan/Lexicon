package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Rule
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.learning_mode
import vokab.resources.generated.resources.learning_mode_settings

@Composable
fun ReviewSettingsCard(
    successesToAdvance: Int,
    forgotPenalty: Int,
    onShowReviewSettingsDialog: () -> Unit
) {
    SettingsCard(
        icon = Icons.Default.Rule,
        title = stringResource(Res.string.learning_mode),
        subtitle = stringResource(Res.string.learning_mode_settings, successesToAdvance, forgotPenalty),
        onClick = onShowReviewSettingsDialog
    )
}

