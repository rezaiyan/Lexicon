package presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.daily_goal
import lexicon.resources.generated.resources.onboarding_words_per_day
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val GoalOptions = listOf(5, 10, 20, 30)
private val GoalSelectionBadgeSize = 22.dp
private val GoalCheckIconSize = 14.dp

@Composable
fun DailyGoalContent(
    selectedGoal: Int,
    onGoalSelected: (Int) -> Unit,
) {
    val spacing = Theme.spacing
    val wordsPerDayLabel = stringResource(Res.string.onboarding_words_per_day)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md)) {
        Spacer(modifier = Modifier.height(spacing.sm))
        Text(
            text = stringResource(Res.string.daily_goal),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(spacing.md))
        DailyGoalCards(
            selectedGoal = selectedGoal,
            wordsPerDayLabel = wordsPerDayLabel,
            onGoalSelected = onGoalSelected,
        )
        Spacer(modifier = Modifier.height(spacing.md))
    }
}

@Composable
private fun DailyGoalCards(
    selectedGoal: Int,
    wordsPerDayLabel: String,
    onGoalSelected: (Int) -> Unit,
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
                    DailyGoalCard(
                        goal = goal,
                        wordsPerDayLabel = wordsPerDayLabel,
                        selected = selectedGoal == goal,
                        onClick = { onGoalSelected(goal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyGoalCard(
    goal: Int,
    wordsPerDayLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
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
