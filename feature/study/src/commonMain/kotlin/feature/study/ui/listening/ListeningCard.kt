package feature.study.ui.listening

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import feature.study.ui.components.PracticeModeTile
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.listening_card_add_words
import lexicon.resources.generated.resources.listening_card_subtitle
import lexicon.resources.generated.resources.listening_title
import org.jetbrains.compose.resources.stringResource

/** Study-tab entry point for hands-free listening mode. */
@Composable
fun ListeningCard(
    hasWords: Boolean,
    onListen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PracticeModeTile(
        title = stringResource(Res.string.listening_title),
        subtitle = stringResource(
            if (hasWords) Res.string.listening_card_subtitle else Res.string.listening_card_add_words
        ),
        icon = Icons.Rounded.Headphones,
        accent = MaterialTheme.colorScheme.primary,
        enabled = hasWords,
        onClick = onListen,
        modifier = modifier,
    )
}
