package feature.onboarding.ui.components

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.back
import lexicon.resources.generated.resources.lets_go
import lexicon.resources.generated.resources.onboarding_daily_goal_subtitle
import lexicon.resources.generated.resources.onboarding_daily_goal_title1
import lexicon.resources.generated.resources.onboarding_daily_goal_title2
import lexicon.resources.generated.resources.onboarding_words_per_day
import org.jetbrains.compose.resources.stringResource
import feature.onboarding.model.OnboardingUiState
import theme.Theme

private val GoalOptions = listOf(5, 10, 20, 30)
private val GoalSelectionBadgeSize = 22.dp
private val GoalCheckIconSize = 14.dp

@Composable
internal fun OnboardingStep4Content(
    state: OnboardingUiState,
    onDailyGoalSelected: (Int) -> Unit,
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
                    line1 = stringResource(Res.string.onboarding_daily_goal_title1),
                    line2 = stringResource(Res.string.onboarding_daily_goal_title2),
                    subtitle = stringResource(Res.string.onboarding_daily_goal_subtitle)
                )
                Spacer(modifier = Modifier.height(spacing.md))
                GoalCards(
                    selectedGoal = state.selectedDailyGoal,
                    wordsPerDayLabel = stringResource(Res.string.onboarding_words_per_day),
                    onGoalSelected = onDailyGoalSelected,
                    enabled = !state.isLoading
                )
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                stringResource(Res.string.lets_go),
                                style = MaterialTheme.typography.labelLarge
                            )
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
private fun GoalCards(
    selectedGoal: Int,
    wordsPerDayLabel: String,
    onGoalSelected: (Int) -> Unit,
    enabled: Boolean
) {
    val spacing = Theme.spacing
    val rows = GoalOptions.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        rows.forEach { rowGoals ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowGoals.forEach { goal ->
                    GoalCard(
                        goal = goal,
                        wordsPerDayLabel = wordsPerDayLabel,
                        selected = selectedGoal == goal,
                        enabled = enabled,
                        onClick = { onGoalSelected(goal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: Int,
    wordsPerDayLabel: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "goal_border_$goal"
    )

    Card(
        onClick = onClick,
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        .size(GoalSelectionBadgeSize)
                        .clip(CircleShape)
                        .background(primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(GoalCheckIconSize),
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.lg, horizontal = Theme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
            ) {
                Text(
                    text = goal.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = wordsPerDayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
