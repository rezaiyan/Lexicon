package feature.subscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.feature_advanced_analytics
import lexicon.resources.generated.resources.feature_ai_image_extraction
import lexicon.resources.generated.resources.feature_basic_vocabulary_lists
import lexicon.resources.generated.resources.feature_export_backup
import lexicon.resources.generated.resources.feature_unlimited_words
import lexicon.resources.generated.resources.features
import lexicon.resources.generated.resources.free_label
import lexicon.resources.generated.resources.premium_label

@Composable
fun ComparisonTable() {
    val headerColor = MaterialTheme.colorScheme.onSurfaceVariant
    val checkColor = MaterialTheme.colorScheme.primary
    val crossColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5F)

    val rows = listOf(
        FeatureRow(
            stringResource(Res.string.feature_basic_vocabulary_lists),
            free = true,
            premium = true
        ),
        FeatureRow(
            stringResource(Res.string.feature_unlimited_words),
            free = false,
            premium = true
        ),
        FeatureRow(
            stringResource(Res.string.feature_ai_image_extraction),
            free = false,
            premium = true
        ),
        FeatureRow(
            stringResource(Res.string.feature_advanced_analytics),
            free = false,
            premium = true
        ),
        FeatureRow(stringResource(Res.string.feature_export_backup), free = false, premium = true),
    )

    val iconSize = Theme.dimensions.iconSizeMedium
    val columnSpacing = Theme.spacing.large

    var freeColumnWidth by remember { mutableStateOf<Dp?>(null) }
    var premiumColumnWidth by remember { mutableStateOf<Dp?>(null) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius)
            )
            .border(
                width = Theme.dimensions.borderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius)
            )
            .padding(Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.features),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            freeColumnWidth
                                ?.let { Modifier.width(it) }
                                ?: Modifier
                        )
                        .onGloballyPositioned { layoutCoordinates ->
                            val measuredWidth = with(density) { layoutCoordinates.size.width.toDp() }
                            val target = max(measuredWidth, iconSize)
                            val current = freeColumnWidth
                            if (current == null || target > current) {
                                freeColumnWidth = target
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.free_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = headerColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .then(
                            premiumColumnWidth
                                ?.let { Modifier.width(it) }
                                ?: Modifier
                        )
                        .onGloballyPositioned { layoutCoordinates ->
                            val measuredWidth = with(density) { layoutCoordinates.size.width.toDp() }
                            val target = max(measuredWidth, iconSize)
                            val current = premiumColumnWidth
                            if (current == null || target > current) {
                                premiumColumnWidth = target
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(Theme.spacing.extraSmall),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = stringResource(Res.string.premium_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(
                                horizontal = Theme.spacing.small,
                                vertical = Theme.spacing.extraSmall
                            )
                        )
                    }
                }
            }
        }

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.width(freeColumnWidth ?: iconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        PlanIconCell(
                            enabled = row.free,
                            iconSize = iconSize,
                            checkColor = checkColor,
                            crossColor = crossColor
                        )
                    }

                    Box(
                        modifier = Modifier.width(premiumColumnWidth ?: iconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        PlanIconCell(
                            enabled = row.premium,
                            iconSize = iconSize,
                            checkColor = checkColor,
                            crossColor = crossColor
                        )
                    }
                }
            }
        }
    }
}

private data class FeatureRow(
    val label: String,
    val free: Boolean,
    val premium: Boolean
)

@Composable
private fun PlanIconCell(
    enabled: Boolean,
    iconSize: Dp,
    checkColor: Color,
    crossColor: Color
) {
    Icon(
        imageVector = if (enabled) Icons.Default.Check else Icons.Default.Close,
        contentDescription = null,
        tint = if (enabled) checkColor else crossColor,
        modifier = Modifier.size(iconSize)
    )
}
