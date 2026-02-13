package presentation.ui.screens.study

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.start_review

@Composable
fun ReviewActionSection(
    hasDueCards: Boolean,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (hasDueCards) {
        Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))
        Button(
            onClick = onStartReview,
            modifier = modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
            )
            Spacer(modifier = Modifier.width(Theme.spacing.extraSmall2))
            Text(stringResource(Res.string.start_review))
        }
    }
}

