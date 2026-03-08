package feature.study.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.back
import lexicon.resources.generated.resources.browse_your_words
import lexicon.resources.generated.resources.next
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Browse-mode navigation buttons. Back is outlined, Forward is filled.
 */
@Composable
fun NavigationButtons(
    currentIndex: Int,
    totalCount: Int,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
    ) {
        Text(
            text = stringResource(Res.string.browse_your_words),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.extraSmall3)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                enabled = currentIndex > 0,
                modifier = Modifier.weight(1f).height(Theme.dimensions.buttonHeight)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                    )
                    Spacer(Modifier.width(Theme.spacing.extraSmall2))
                    Text(stringResource(Res.string.back), fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onNavigateForward,
                enabled = currentIndex < totalCount - 1,
                modifier = Modifier.weight(1f).height(Theme.dimensions.buttonHeight)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.next), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(Theme.spacing.extraSmall2))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(Res.string.next),
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                    )
                }
            }
        }
    }
}

