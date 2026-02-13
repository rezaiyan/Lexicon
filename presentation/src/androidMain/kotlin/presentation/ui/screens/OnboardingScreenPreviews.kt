package presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import presentation.model.OnboardingUiState
import theme.AppDimensions
import theme.AppSpacing
import theme.LexiconTheme

@Preview(showBackground = true, name = "Onboarding Step 1 - German Selected")
@Composable
private fun OnboardingStep1GermanPreview() {
    LexiconTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            OnboardingStep1Content(
                state = OnboardingUiState(
                    currentStep = 1,
                    selectedTargetLanguage = "German"
                ),
                onTargetLanguageSelected = {},
                onNextStep = {},
                onSkip = {},
                spacing = AppSpacing(),
                dimensions = AppDimensions()
            )
        }
    }
}
