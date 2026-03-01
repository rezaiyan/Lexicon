package presentation.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.back
import lexicon.resources.generated.resources.lets_go
import lexicon.resources.generated.resources.onboarding_advanced_desc
import lexicon.resources.generated.resources.onboarding_beginner_desc
import lexicon.resources.generated.resources.onboarding_current_level
import lexicon.resources.generated.resources.onboarding_generating_vocabulary
import lexicon.resources.generated.resources.onboarding_intermediate_desc
import lexicon.resources.generated.resources.onboarding_level_subtitle
import lexicon.resources.generated.resources.onboarding_loading_tip_1
import lexicon.resources.generated.resources.onboarding_loading_tip_2
import lexicon.resources.generated.resources.onboarding_loading_tip_3
import lexicon.resources.generated.resources.onboarding_loading_tip_4
import lexicon.resources.generated.resources.onboarding_whats_your
import org.jetbrains.compose.resources.stringResource
import presentation.model.OnboardingUiState
import theme.Theme

internal val levelIcons = mapOf(
    "beginner" to Icons.Default.School,
    "intermediate" to Icons.AutoMirrored.Filled.TrendingUp,
    "advanced" to Icons.Default.Star
)

private val SelectionBadgeSize = 22.dp
private val CheckIconSize = 14.dp

@Composable
internal fun OnboardingStep3Content(
    state: OnboardingUiState,
    onLevelSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions
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
                Spacer(modifier = Modifier.height(spacing.sm))
                StepHeadline(
                    line1 = stringResource(Res.string.onboarding_whats_your),
                    line2 = stringResource(Res.string.onboarding_current_level),
                    subtitle = stringResource(Res.string.onboarding_level_subtitle)
                )
                Spacer(modifier = Modifier.height(spacing.md))
                LevelCards(
                    selectedLevel = state.selectedLevel,
                    onLevelSelected = onLevelSelected,
                    enabled = !state.isLoading
                )
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = error,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = dimensions.contentMaxWidth),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Text(
                                stringResource(Res.string.back),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onSubmit,
                            modifier = Modifier.weight(2f),
                            enabled = state.selectedLevel != null,
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = spacing.lg),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(dimensions.iconSizeMedium)
                            )
                            Spacer(modifier = Modifier.size(spacing.xs))
                            Text(stringResource(Res.string.lets_go), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(spacing.md))
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
internal fun OnboardingLoadingCard() {
    val spacing = Theme.spacing
    var currentTipIndex by remember { mutableStateOf(0) }
    val loadingTips = listOf(
        stringResource(Res.string.onboarding_loading_tip_1),
        stringResource(Res.string.onboarding_loading_tip_2),
        stringResource(Res.string.onboarding_loading_tip_3),
        stringResource(Res.string.onboarding_loading_tip_4)
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            currentTipIndex = (currentTipIndex + 1) % loadingTips.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(Theme.shapes.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.medium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_generating_vocabulary),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                AnimatedContent(
                    targetState = loadingTips[currentTipIndex],
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "tip_animation"
                ) { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterHorizontally)
            ) {
                repeat(4) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000,
                                delayMillis = index * 200,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(Theme.spacing.xs)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
internal fun LevelCards(
    selectedLevel: String?,
    onLevelSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    val spacing = Theme.spacing
    val levels = listOf(
        Triple("beginner", stringResource(Res.string.onboarding_beginner_desc), levelIcons["beginner"]),
        Triple("intermediate", stringResource(Res.string.onboarding_intermediate_desc), levelIcons["intermediate"]),
        Triple("advanced", stringResource(Res.string.onboarding_advanced_desc), levelIcons["advanced"])
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        levels.forEach { (level, description, icon) ->
            LevelTile(
                level = level,
                description = description,
                icon = icon,
                levelColor = when (level) {
                    "beginner" -> MaterialTheme.colorScheme.secondary
                    "intermediate" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                selected = selectedLevel == level,
                enabled = enabled,
                onClick = { onLevelSelected(level) }
            )
        }
    }
}

@Composable
private fun LevelTile(
    level: String,
    description: String,
    icon: ImageVector?,
    levelColor: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "level_bg_$level"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) levelColor
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "level_border_$level"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(Theme.shapes.large),
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
                        .background(levelColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(CheckIconSize),
                        tint = Color.White
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
                Box(
                    modifier = Modifier
                        .size(Theme.dimensions.touchTarget)
                        .clip(RoundedCornerShape(14.dp))
                        .background(levelColor.copy(alpha = if (selected) 0.18f else 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSize),
                            tint = levelColor
                        )
                    }
                }

                Text(
                    text = level.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) levelColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
