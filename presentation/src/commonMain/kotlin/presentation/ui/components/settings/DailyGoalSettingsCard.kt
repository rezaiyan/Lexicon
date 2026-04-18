package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import theme.AppColors
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.daily_goal
import lexicon.resources.generated.resources.settings_daily_goal_subtitle

@Composable
fun DailyGoalSettingsCard(
    dailyGoalWords: Int,
    onClick: () -> Unit
) {
    SettingsCard(
        icon = Icons.Default.TrackChanges,
        title = stringResource(Res.string.daily_goal),
        subtitle = stringResource(Res.string.settings_daily_goal_subtitle),
        iconBackgroundColor = AppColors.settingsDailyGoalIcon,
        onClick = onClick
    )
}
