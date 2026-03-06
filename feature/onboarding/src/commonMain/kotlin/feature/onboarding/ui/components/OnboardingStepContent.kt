package feature.onboarding.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.onboarding_are_you_learning
import lexicon.resources.generated.resources.onboarding_continue
import lexicon.resources.generated.resources.onboarding_native_language_question
import lexicon.resources.generated.resources.onboarding_native_language_subtitle
import lexicon.resources.generated.resources.onboarding_target_language_subtitle
import lexicon.resources.generated.resources.onboarding_which_language
import lexicon.resources.generated.resources.onboarding_whats_your
import lexicon.resources.generated.resources.skip_preferences
import org.jetbrains.compose.resources.stringResource
import feature.onboarding.model.OnboardingUiState
import theme.Theme

@Composable
fun OnboardingStep1Content(
    state: OnboardingUiState,
    onTargetLanguageSelected: (String) -> Unit,
    onNextStep: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = Theme.spacing
    val scrollState = rememberScrollState()
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
                line1 = stringResource(Res.string.onboarding_which_language),
                line2 = stringResource(Res.string.onboarding_are_you_learning),
                subtitle = stringResource(Res.string.onboarding_target_language_subtitle)
            )
            Spacer(modifier = Modifier.height(spacing.md))

            LanguageGrid(
                languages = state.availableLanguages,
                selectedLanguage = state.selectedTargetLanguage,
                onLanguageSelected = onTargetLanguageSelected
            )
        }

        OnboardingButtons(
            onPrimaryClick = onNextStep,
            onSecondaryClick = onSkip,
            primaryText = stringResource(Res.string.onboarding_continue),
            secondaryText = stringResource(Res.string.skip_preferences),
            primaryEnabled = state.selectedTargetLanguage != null
        )
    }
}

@Composable
internal fun OnboardingStep2Content(
    state: OnboardingUiState,
    onNativeLanguageSelected: (String) -> Unit,
    onNextStep: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = Theme.spacing
    val scrollState = rememberScrollState()
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
                line2 = stringResource(Res.string.onboarding_native_language_question),
                subtitle = stringResource(Res.string.onboarding_native_language_subtitle)
            )
            Spacer(modifier = Modifier.height(spacing.md))

            LanguageGrid(
                languages = state.availableLanguages.filter { it != state.selectedTargetLanguage },
                selectedLanguage = state.selectedNativeLanguage,
                onLanguageSelected = onNativeLanguageSelected
            )
        }

        OnboardingButtons(
            onPrimaryClick = onNextStep,
            onSecondaryClick = onSkip,
            primaryText = stringResource(Res.string.onboarding_continue),
            secondaryText = stringResource(Res.string.skip_preferences),
            primaryEnabled = state.selectedNativeLanguage != null
        )
    }
}

@Composable
internal fun StepHeadline(
    line1: String,
    line2: String,
    subtitle: String
) {
    val spacing = Theme.spacing
    Text(
        text = line1,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = line2,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(spacing.xxs))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun OnboardingButtons(
    primaryText: String,
    secondaryText: String,
    primaryEnabled: Boolean = true,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = dimensions.contentMaxWidth),
            enabled = primaryEnabled,
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(50)
        ) {
            Text(primaryText, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.size(spacing.xs))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium)
            )
        }

        TextButton(
            onClick = onSecondaryClick,
            modifier = Modifier
                .padding(top = spacing.xs)
                .fillMaxWidth()
                .widthIn(max = dimensions.contentMaxWidth)
        ) {
            Text(
                secondaryText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(spacing.lg))
    }
}
