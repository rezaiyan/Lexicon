package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.LottieMotionIcon
import feature.aiimport.model.AiWordImportUiState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.onboarding_generating_vocabulary
import lexicon.resources.generated.resources.onboarding_loading_tip_1
import lexicon.resources.generated.resources.onboarding_loading_tip_2
import lexicon.resources.generated.resources.onboarding_loading_tip_3
import lexicon.resources.generated.resources.onboarding_loading_tip_4
import lexicon.resources.generated.resources.ai_wizard_generate_words
import lexicon.resources.generated.resources.ai_wizard_topics_highlight
import lexicon.resources.generated.resources.ai_wizard_topics_subtitle
import lexicon.resources.generated.resources.ai_wizard_topics_title
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

private val SelectionBadgeSize = 22.dp
private val CheckIconSize = 14.dp

private const val AI_LOADING_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/91ccdf52-1150-11ee-b7cc-8f23ce57c5d5/zUi6h6u4zD.json"

@Composable
internal fun AiTopicsStep(
    state: AiWordImportUiState,
    onToggleTopic: (String) -> Unit,
    onGenerate: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    if (state.isLoading) {
        AiGeneratingContent(spacing = spacing)
    } else {
        AiTopicsContent(
            state = state,
            onToggleTopic = onToggleTopic,
            onGenerate = onGenerate,
            spacing = spacing,
            dimensions = dimensions,
        )
    }
}

@Composable
private fun AiGeneratingContent(spacing: AppSpacing) {
    val motion = Theme.motion
    val loadingTips = listOf(
        stringResource(Res.string.onboarding_loading_tip_1),
        stringResource(Res.string.onboarding_loading_tip_2),
        stringResource(Res.string.onboarding_loading_tip_3),
        stringResource(Res.string.onboarding_loading_tip_4),
    )
    var currentTipIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            currentTipIndex = (currentTipIndex + 1) % loadingTips.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LottieMotionIcon(
            url = AI_LOADING_LOTTIE_URL,
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(spacing.md))

        Text(
            text = stringResource(Res.string.onboarding_generating_vocabulary),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        AnimatedContent(
            targetState = loadingTips[currentTipIndex],
            transitionSpec = {
                (fadeIn(animationSpec = tween(motion.durationMedium2)) togetherWith fadeOut(animationSpec = tween(motion.durationMedium2)))
                    .using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> snap() }))
            },
            label = "loading_tip",
        ) { tip ->
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
            )
        }
    }
}

@Composable
private fun AiTopicsContent(
    state: AiWordImportUiState,
    onToggleTopic: (String) -> Unit,
    onGenerate: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = spacing.xs)
        ) {
            AiStepHeader(
                title = stringResource(Res.string.ai_wizard_topics_title),
                highlight = stringResource(Res.string.ai_wizard_topics_highlight),
                subtitle = stringResource(Res.string.ai_wizard_topics_subtitle),
                spacing = spacing
            )

            TopicGrid(
                topics = state.availableTopics,
                selectedTopics = state.selectedTopics,
                isLoading = false,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGenerate,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth),
                contentPadding = PaddingValues(vertical = spacing.md, horizontal = spacing.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(Theme.shapes.pill)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
                Spacer(modifier = Modifier.size(spacing.xs))
                Text(stringResource(Res.string.ai_wizard_generate_words), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private val topicPalette = listOf(
    Color(0xFFFFF3E0), // Daily Life — warm amber
    Color(0xFFE3F2FD), // Travel — sky blue
    Color(0xFFECEFF1), // Business — cool slate
    Color(0xFFFBE9E7), // Food — soft orange
    Color(0xFFEDE7F6), // Technology — lavender
    Color(0xFFE8F5E9), // Sports — fresh green
    Color(0xFFFCE4EC), // Health — rose pink
    Color(0xFFF3E5F5), // Arts — orchid
    Color(0xFFE0F2F1), // Nature — teal mint
    Color(0xFFE8EAF6), // Academic — indigo mist
)

private val topicPaletteDark = listOf(
    Color(0xFF4E3B24), // Daily Life
    Color(0xFF1A3A5C), // Travel
    Color(0xFF2C3440), // Business
    Color(0xFF4A2C22), // Food
    Color(0xFF2D1F4E), // Technology
    Color(0xFF1B3A25), // Sports
    Color(0xFF3E1F2A), // Health
    Color(0xFF3A1E42), // Arts
    Color(0xFF1A3836), // Nature
    Color(0xFF1F2346), // Academic
)

@Composable
private fun TopicGrid(
    topics: List<String>,
    selectedTopics: Set<String>,
    isLoading: Boolean,
    onToggleTopic: (String) -> Unit,
) {
    val isDark = !MaterialTheme.colorScheme.surface.luminance().let { it > 0.5f }
    val palette = if (isDark) topicPaletteDark else topicPalette

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
                    tileColor = palette[firstIndex % palette.size],
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
                        tileColor = palette[secondIndex % palette.size],
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
    tileColor: Color,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f).compositeOver(tileColor)
        else tileColor,
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
                        .size(SelectionBadgeSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(CheckIconSize),
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
