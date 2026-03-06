package feature.profile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.languages
import lexicon.resources.generated.resources.words
import org.jetbrains.compose.resources.stringResource
import feature.profile.model.LanguagePairUiModel
import components.animation.rememberAnimatedCounter
import components.animation.staggeredFadeSlide
import theme.Theme

@Composable
fun LanguagesSection(
    languages: List<LanguagePairUiModel>,
    modifier: Modifier = Modifier
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val totalWords = languages.sumOf { it.wordCount }
    val animatedTotal = rememberAnimatedCounter(target = totalWords)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        tonalElevation = Theme.elevation.none,
        border = BorderStroke(
            width = Theme.dimensions.borderWidth,
            brush = Brush.linearGradient(
                listOf(
                    secondaryColor.copy(alpha = 0.15f),
                    tertiaryColor.copy(alpha = 0.10f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.languages),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(Res.string.words, animatedTotal),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))

            languages.forEachIndexed { index, pair ->
                LanguagePairRow(
                    pair = pair,
                    accentColor = languageColors[index % languageColors.size],
                    modifier = Modifier.staggeredFadeSlide(index)
                )
                if (index < languages.lastIndex) {
                    HorizontalDivider(
                        thickness = Theme.dimensions.hairlineThickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.padding(start = 52.dp, top = Theme.spacing.xxs, bottom = Theme.spacing.xxs)
                    )
                }
            }
        }
    }
}

private val languageColors: List<androidx.compose.ui.graphics.Color>
    @Composable
    get() = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error
    )

@Composable
private fun LanguagePairRow(
    pair: LanguagePairUiModel,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = Theme.spacing.extraSmall, vertical = Theme.spacing.extraSmall2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.touchTargetSmall)
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.18f),
                            accentColor.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(Theme.shapes.medium)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
            )
        }

        Spacer(modifier = Modifier.width(Theme.spacing.extraSmall))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${pair.sourceLanguage} \u2192 ${pair.targetLanguage}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = stringResource(Res.string.words, pair.wordCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
            maxLines = 1
        )
    }
}
