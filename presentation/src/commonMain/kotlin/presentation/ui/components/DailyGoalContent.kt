package presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.sheet.SheetPage
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.daily_goal
import lexicon.resources.generated.resources.onboarding_words_per_day
import lexicon.resources.generated.resources.settings_daily_goal_subtitle
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val GoalOptions = listOf(5, 10, 20, 30)
private val GoalCardMinHeight = 104.dp
private val GoalSelectionBadgeSize = 22.dp
private val GoalCheckIconSize = 14.dp
private val SelectedBorder = 2.dp

/** Daily goal picker; tapping a card applies it. */
@Composable
fun DailyGoalContent(
    selectedGoal: Int,
    onGoalSelected: (Int) -> Unit,
    onClose: (() -> Unit)? = null,
) {
    val wordsPerDayLabel = stringResource(Res.string.onboarding_words_per_day)
    SheetPage(
        title = stringResource(Res.string.daily_goal),
        subtitle = stringResource(Res.string.settings_daily_goal_subtitle),
        onClose = onClose,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            GoalOptions.chunked(2).forEach { rowGoals ->
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                    rowGoals.forEach { goal ->
                        DailyGoalCard(
                            goal = goal,
                            wordsPerDayLabel = wordsPerDayLabel,
                            selected = selectedGoal == goal,
                            onClick = { onGoalSelected(goal) },
                            modifier = Modifier.weight(1f),
                        )
                    }
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
    val accent = MaterialTheme.colorScheme.primary
    val container by animateColorAsState(
        if (selected) accent.copy(alpha = Theme.opacity.focus) else MaterialTheme.colorScheme.surface,
        label = "goal_bg_$goal",
    )
    val borderColor by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
        label = "goal_border_$goal",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            this.selected = selected
            role = Role.RadioButton
        },
        shape = RoundedCornerShape(Theme.shapes.large),
        color = container,
        border = BorderStroke(if (selected) SelectedBorder else Theme.dimensions.borderWidth, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = GoalCardMinHeight)) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Theme.spacing.xs)
                        .size(GoalSelectionBadgeSize)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(GoalCheckIconSize),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
            ) {
                Text(
                    text = goal.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = wordsPerDayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
