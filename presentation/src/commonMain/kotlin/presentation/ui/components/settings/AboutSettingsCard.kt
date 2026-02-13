package presentation.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import expects.openUrl
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.about
import lexicon.resources.generated.resources.app_description

@Composable
fun AboutSettingsCard(appVersion: String) {
    var clickCount by remember { mutableStateOf(0) }
    LaunchedEffect(clickCount) {
        if (clickCount > 0) {
            delay(2000)
            clickCount = 0
        }
    }

    val handleClick = {
        clickCount++
        if (clickCount >= 3) {
            openUrl("https://alirezaiyan.com/vokab/")
            clickCount = 0
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = handleClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSize)
            )

            Spacer(modifier = Modifier.width(Theme.spacing.extraSmall))

            Column {
                Text(
                    text = stringResource(Res.string.about),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.app_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Version $appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



