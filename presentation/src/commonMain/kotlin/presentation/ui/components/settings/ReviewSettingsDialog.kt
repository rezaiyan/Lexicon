package presentation.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.BasicAlertDialog
import presentation.ui.components.ButtonState
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.apply_button
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.choose_learning_pace
import lexicon.resources.generated.resources.learning_mode
import lexicon.resources.generated.resources.mode_balanced
import lexicon.resources.generated.resources.mode_balanced_description
import lexicon.resources.generated.resources.mode_balanced_subtitle
import lexicon.resources.generated.resources.mode_easy
import lexicon.resources.generated.resources.mode_easy_description
import lexicon.resources.generated.resources.mode_easy_subtitle
import lexicon.resources.generated.resources.mode_rigorous
import lexicon.resources.generated.resources.mode_rigorous_description
import lexicon.resources.generated.resources.mode_rigorous_subtitle

@Composable
internal fun ReviewSettingsDialog(
    successesToAdvance: Int,
    forgotPenalty: Int,
    onDismiss: () -> Unit,
    onSettingsChanged: (successesToAdvance: Int, forgotPenalty: Int) -> Unit
) {
    var selectedMode by remember {
        mutableStateOf(
            when {
                successesToAdvance == 1 && forgotPenalty == 1 -> 0 // Easy
                successesToAdvance == 1 && forgotPenalty == 2 -> 1 // Balanced
                successesToAdvance == 2 && forgotPenalty == 3 -> 2 // Rigorous
                else -> 1 // Default to Balanced
            }
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.learning_mode),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
            ) {
                Text(
                    text = stringResource(Res.string.choose_learning_pace),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Theme.spacing.extraSmall2)
                )

                LearningModeOption(
                    title = stringResource(Res.string.mode_easy),
                    subtitle = stringResource(Res.string.mode_easy_subtitle),
                    description = stringResource(Res.string.mode_easy_description),
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 }
                )

                LearningModeOption(
                    title = stringResource(Res.string.mode_balanced),
                    subtitle = stringResource(Res.string.mode_balanced_subtitle),
                    description = stringResource(Res.string.mode_balanced_description),
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 }
                )

                LearningModeOption(
                    title = stringResource(Res.string.mode_rigorous),
                    subtitle = stringResource(Res.string.mode_rigorous_subtitle),
                    description = stringResource(Res.string.mode_rigorous_description),
                    selected = selectedMode == 2,
                    onClick = { selectedMode = 2 }
                )
            }
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.apply_button),
            onClick = {
                val (successes, penalty) = when (selectedMode) {
                    0 -> 1 to 1
                    1 -> 1 to 2
                    2 -> 2 to 3
                    else -> 1 to 2
                }
                onSettingsChanged(successes, penalty)
            }
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss
        )
    )
}

@Composable
private fun LearningModeOption(
    title: String,
    subtitle: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(Theme.spacing.small))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = Theme.spacing.extraSmall4)
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    modifier = Modifier.padding(top = Theme.spacing.extraSmall3)
                )
            }
        }
    }
}
