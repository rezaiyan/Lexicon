@file:OptIn(InternalResourceApi::class)

package presentation.ui.screens.review

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import domain.word.model.Word
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.FlashCard
import presentation.ui.components.ReviewButton
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.advance
import vokab.resources.generated.resources.back
import vokab.resources.generated.resources.browse_your_words
import vokab.resources.generated.resources.close
import vokab.resources.generated.resources.did_you_remember
import vokab.resources.generated.resources.forgot
import vokab.resources.generated.resources.next
import vokab.resources.generated.resources.no_words_to_review
import vokab.resources.generated.resources.remembered
import vokab.resources.generated.resources.restart
import vokab.resources.generated.resources.retry

/**
 * Header component for review screen
 * Shows title and close button
 */
@Composable
fun ReviewHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.cardPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(Res.string.close),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Loading state component
 */
@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(30.dp)) // 30dp for prominent loading
    }
}

/**
 * Error state component with retry button
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.retry))
            }
        }
    }
}

/**
 * Empty state component when no words to review
 */
@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.spacing.extraLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(Res.string.no_words_to_review),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Progress indicator component
 * Shows current position and animated progress bar
 */
@Composable
fun ProgressIndicator(
    currentIndex: Int,
    totalCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium)
    ) {
        val progress = (currentIndex + 1).toFloat() / totalCount.toFloat()
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )

        Text(
            "${currentIndex + 1} / $totalCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.extraSmall3)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Theme.dimensions.progressBarHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(Theme.dimensions.progressBarHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Review content component
 * Main content area with flashcard and controls
 */
@Composable
fun ReviewContent(
    words: List<Word>,
    currentIndex: Int,
    isFlipped: Boolean,
    reviewType: presentation.model.ReviewType,
    onFlip: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onReview: (Int) -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar
        ProgressIndicator(
            currentIndex = currentIndex,
            totalCount = words.size
        )

        Spacer(Modifier.weight(0.25f))

        // Flashcard
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            key(words[currentIndex].id) {
                FlashCard(
                    word = words[currentIndex],
                    isFlipped = isFlipped,
                    onFlip = onFlip,
                    onEdit = onEdit
                )
            }
        }

        Spacer(Modifier.weight(0.25f))

        Box(Modifier.padding(Theme.spacing.medium)) {
            when (reviewType) {
                presentation.model.ReviewType.REVIEW -> {
                    // Active review mode with response buttons
                    RatingButtons(onReview = onReview)
                }
                presentation.model.ReviewType.BROWSE -> {
                    // Passive browse mode with navigation buttons
                    NavigationButtons(
                        currentIndex = currentIndex,
                        totalCount = words.size,
                        onNavigateBack = onNavigateBack,
                        onNavigateForward = onNavigateForward
                    )
                }
            }
        }
    }
}

/**
 * Rating buttons component for review mode
 */
@Composable
fun RatingButtons(onReview: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(Res.string.did_you_remember),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        RatingButtonsHorizontal(onReview = onReview)
    }
}

@Composable
private fun RatingButtonsHorizontal(onReview: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
    ) {
        RatingButtonData.values.forEach { data ->
            ReviewButton(
                text = stringResource(data.textRes),
                subText = stringResource(data.subTextRes),
                color = data.color(),
                modifier = Modifier.weight(1f).height(56.dp), // Button height
                onClick = { onReview(data.rating) }
            )
        }
    }
}

/**
 * Navigation buttons component for browse mode
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
            stringResource(Res.string.browse_your_words),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.extraSmall3)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            OutlinedButton(
                onClick = onNavigateBack,
                enabled = currentIndex > 0,
                modifier = Modifier.weight(1f).height(56.dp) // Button height
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
                    Spacer(modifier = Modifier.width(Theme.spacing.extraSmall2))
                    Text(stringResource(Res.string.back), fontWeight = FontWeight.Bold)
                }
            }
            
            // Forward button
            Button(
                onClick = onNavigateForward,
                enabled = currentIndex < totalCount - 1,
                modifier = Modifier.weight(1f).height(56.dp) // Button height
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(Res.string.next),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(Theme.spacing.extraSmall2))
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

/**
 * Rating button configuration data
 */
internal data class RatingButtonData(
    val rating: Int,
    val textRes: org.jetbrains.compose.resources.StringResource,
    val subTextRes: org.jetbrains.compose.resources.StringResource,
    val color: @Composable () -> Color
) {
    companion object {
        val values = listOf(
            RatingButtonData(
                rating = 0,
                textRes = Res.string.forgot,
                subTextRes = Res.string.restart,
                color = { MaterialTheme.colorScheme.error }
            ),
            RatingButtonData(
                rating = 1,
                textRes = Res.string.remembered,
                subTextRes = Res.string.advance,
                color = { MaterialTheme.colorScheme.primary }
            )
        )
    }
}

