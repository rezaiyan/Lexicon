package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.word_manager
import lexicon.resources.generated.resources.word_manager_subtitle

@Composable
fun WordManagerCard(onClick: () -> Unit) {
    SettingsCard(
        icon = Icons.Default.LibraryBooks,
        title = stringResource(Res.string.word_manager),
        subtitle = stringResource(Res.string.word_manager_subtitle),
        onClick = onClick
    )
}
