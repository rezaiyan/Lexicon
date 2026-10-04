package presentation.ui.components.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.change_languages
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

/*
 * Language-aware pieces of the add-words flow. Generic sheet building blocks live in
 * design-system `components.sheet`.
 */

/** "German → English ⌄" — shows the import language pair and opens the language picker. */
@Composable
internal fun LanguagePairChip(
    source: Language,
    target: Language,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.change_languages)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$description: ${source.displayName} to ${target.displayName}" }
            .heightIn(min = Theme.dimensions.touchTargetSmall)
            .padding(start = Theme.spacing.sm, end = Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
    ) {
        Text(source.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall - Theme.spacing.xxxs),
        )
        Text(target.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
        )
    }
}
