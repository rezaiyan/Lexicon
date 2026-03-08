package presentation.ui.components.imports

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import feature.aiimport.model.AiWordImportUiState
import feature.onboarding.ui.components.OnboardingLoadingCard
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

@Composable
internal fun AiTopicsStep(
    state: AiWordImportUiState,
    onToggleTopic: (String) -> Unit,
    onGenerate: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing.md)
                    .padding(bottom = spacing.xs)
            ) {
                AiStepHeader(
                    title = "What topics",
                    highlight = "interest you?",
                    subtitle = "Pick any that appeal to you. We'll mix them to create a varied vocabulary pack.",
                    spacing = spacing
                )

                TopicGrid(
                    topics = state.availableTopics,
                    selectedTopics = state.selectedTopics,
                    isLoading = state.isLoading,
                    onToggleTopic = onToggleTopic,
                )

                state.error?.let {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isLoading) {
                    OnboardingLoadingCard()
                } else {
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = Theme.dimensions.contentMaxWidth),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = spacing.lg),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Spacer(modifier = Modifier.size(spacing.xs))
                        Text("Generate Words", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(spacing.md))
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                    }
            )
        }
    }
}

@Composable
private fun TopicGrid(
    topics: List<String>,
    selectedTopics: Set<String>,
    isLoading: Boolean,
    onToggleTopic: (String) -> Unit,
) {
    val rows = (topics.size + 1) / 2
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
    ) {
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
            ) {
                val firstIndex = rowIndex * 2
                TopicTile(
                    topic = topics[firstIndex],
                    emoji = "",
                    selected = selectedTopics.contains(topics[firstIndex]),
                    enabled = !isLoading,
                    onClick = { onToggleTopic(topics[firstIndex]) },
                    modifier = Modifier.weight(1f)
                )
                val secondIndex = firstIndex + 1
                if (secondIndex < topics.size) {
                    TopicTile(
                        topic = topics[secondIndex],
                        emoji = "",
                        selected = selectedTopics.contains(topics[secondIndex]),
                        enabled = !isLoading,
                        onClick = { onToggleTopic(topics[secondIndex]) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TopicTile(
    topic: String,
    emoji: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tile_bg_$topic"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tile_border_$topic"
    )

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) Theme.elevation.medium else Theme.elevation.none
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Theme.spacing.xs)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.md, horizontal = Theme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = topic,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
