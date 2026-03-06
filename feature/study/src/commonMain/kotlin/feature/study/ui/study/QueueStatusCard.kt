package feature.study.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cards_waiting
import lexicon.resources.generated.resources.everything_reviewed
import lexicon.resources.generated.resources.queue_is_empty
import lexicon.resources.generated.resources.queue_ready_subtitle
import lexicon.resources.generated.resources.refresh
import lexicon.resources.generated.resources.start_review

@Composable
fun QueueStatusCard(
    dueCards: Int,
    totalWords: Int,
    onStartReview: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalWords == 0) return

    when {
        dueCards == 0 -> {
            // Queue empty state
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        color = AppColors.master.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(Theme.shapes.large)
                    )
                    .border(
                        width = Theme.dimensions.borderWidth,
                        color = AppColors.master.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(Theme.shapes.large)
                    )
                    .padding(horizontal = 20.dp, vertical = Theme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Theme.dimensions.iconSizeXLarge)
                        .background(
                            color = AppColors.master.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = AppColors.master
                    )
                }
                Spacer(Modifier.width(Theme.spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.queue_is_empty),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.everything_reviewed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(Theme.shapes.pill)
                ) {
                    Text(
                        text = stringResource(Res.string.refresh),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        else -> {
            // Due cards available
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(Theme.shapes.large)
                    )
                    .border(
                        width = Theme.dimensions.borderWidth,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(Theme.shapes.large)
                    )
                    .padding(horizontal = 20.dp, vertical = Theme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Theme.dimensions.iconSizeXLarge)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(Theme.spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.cards_waiting, dueCards),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.queue_ready_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onStartReview,
                    shape = RoundedCornerShape(Theme.shapes.pill)
                ) {
                    Text(
                        text = stringResource(Res.string.start_review),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
