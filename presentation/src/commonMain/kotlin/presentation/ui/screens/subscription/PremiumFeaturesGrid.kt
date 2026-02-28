package presentation.ui.screens.subscription

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
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_book
import lexicon.resources.generated.resources.everything_you_get
import lexicon.resources.generated.resources.feature_ai_extraction
import lexicon.resources.generated.resources.feature_ai_extraction_desc
import lexicon.resources.generated.resources.feature_export_data
import lexicon.resources.generated.resources.feature_export_data_desc
import lexicon.resources.generated.resources.feature_priority_support
import lexicon.resources.generated.resources.feature_priority_support_desc
import lexicon.resources.generated.resources.feature_unlimited_words
import lexicon.resources.generated.resources.feature_unlimited_words_desc

sealed class FeatureIcon {
    data class Vector(val imageVector: ImageVector) : FeatureIcon()
    data class Drawable(val painter: Painter) : FeatureIcon()
}

data class FeatureItem(
    val icon: FeatureIcon,
    val title: String,
    val description: String
)
@Composable
fun PremiumFeaturesGrid() {
    val features = listOf(
        FeatureItem(
            FeatureIcon.Vector(Icons.Rounded.StarOutline),
            stringResource(Res.string.feature_unlimited_words),
            stringResource(Res.string.feature_unlimited_words_desc)
        ),
        FeatureItem(
            FeatureIcon.Drawable(painterResource(Res.drawable.ai_book)),
            stringResource(Res.string.feature_ai_extraction),
            stringResource(Res.string.feature_ai_extraction_desc)
        ),
//        FeatureItem(
//            FeatureIcon.Vector(Icons.AutoMirrored.Rounded.ShowChart),
//            stringResource(Res.string.feature_advanced_analytics),
//            stringResource(Res.string.feature_advanced_analytics_desc)
//        ),
        FeatureItem(
            FeatureIcon.Vector(Icons.Outlined.Info),
            stringResource(Res.string.feature_priority_support),
            stringResource(Res.string.feature_priority_support_desc)
        ),
//        FeatureItem(
//            FeatureIcon.Vector(Icons.Rounded.WorkspacePremium),
//            stringResource(Res.string.vokab_pro),
//            stringResource(Res.string.feature_export_data_desc)
//        ),
        FeatureItem(
            FeatureIcon.Vector(Icons.Outlined.Cloud),
            stringResource(Res.string.feature_export_data),
            stringResource(Res.string.feature_export_data_desc)
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
    ) {
        Text(
            text = stringResource(Res.string.everything_you_get),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
        ) {
            features.forEach { feature ->
                FeatureCard(
                    feature = feature,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    feature: FeatureItem,
    modifier: Modifier = Modifier
) {
    val cardColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.large),
        color = cardColor,
        shadowElevation = Theme.elevation.none
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = Theme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = 0.18f),
                                accent.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (val icon = feature.icon) {
                    is FeatureIcon.Vector -> {
                        Icon(
                            imageVector = icon.imageVector,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(Theme.dimensions.iconSizeLarge)
                        )
                    }
                    is FeatureIcon.Drawable -> {
                        Icon(
                            painter = icon.painter,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(Theme.dimensions.iconSizeLarge)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Theme.spacing.md))

            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Theme.spacing.xxs))

                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

