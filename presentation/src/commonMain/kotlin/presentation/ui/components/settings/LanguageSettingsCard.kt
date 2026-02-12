package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import utils.Language
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.translation_language

@Composable
fun LanguageSettingsCard(
    currentLanguage: Language,
    onShowLanguageDialog: () -> Unit
) {
    SettingsCard(
        icon = Icons.Default.Language,
        title = stringResource(Res.string.translation_language),
        subtitle = currentLanguage.nativeName,
        onClick = onShowLanguageDialog
    )
}



