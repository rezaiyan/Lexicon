package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import theme.AppColors
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.tts_models
import lexicon.resources.generated.resources.tts_models_subtitle

@Composable
fun TtsModelCacheCard(
    onClick: () -> Unit,
) {
    SettingsCard(
        icon = Icons.Default.RecordVoiceOver,
        title = stringResource(Res.string.tts_models),
        subtitle = stringResource(Res.string.tts_models_subtitle),
        iconBackgroundColor = AppColors.settingsTtsIcon,
        onClick = onClick,
    )
}
