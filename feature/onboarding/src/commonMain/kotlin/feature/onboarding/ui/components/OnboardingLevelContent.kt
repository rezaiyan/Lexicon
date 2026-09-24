package feature.onboarding.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.back
import lexicon.resources.generated.resources.onboarding_continue
import lexicon.resources.generated.resources.onboarding_current_level
import lexicon.resources.generated.resources.onboarding_level_subtitle
import lexicon.resources.generated.resources.onboarding_whats_your
import org.jetbrains.compose.resources.stringResource
import feature.onboarding.model.OnboardingUiState
import theme.Theme

@Composable
internal fun OnboardingStep3Content(
    state: OnboardingUiState,
    onLevelSelected: (String) -> Unit,
    onNextStep: () -> Unit,
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
                        onClick = onNextStep,
                        modifier = Modifier.weight(2f),
                        enabled = state.selectedLevel != null,
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = spacing.lg),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(stringResource(Res.string.onboarding_continue), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(spacing.md))
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                OnboardingLoadingCard()
            }
        }
    }
}
