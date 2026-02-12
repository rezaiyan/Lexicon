package presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import presentation.model.OnboardingUiState

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onTargetLanguageSelected: (String) -> Unit,
    onNativeLanguageSelected: (String) -> Unit,
    onLevelSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set Your Preferences",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Help us personalize your learning experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Target Language
            Text(
                text = "I want to learn:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LanguageChipRow(
                languages = state.availableLanguages,
                selectedLanguage = state.selectedTargetLanguage,
                onLanguageSelected = onTargetLanguageSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Native Language
            Text(
                text = "I speak:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LanguageChipRow(
                languages = state.availableLanguages,
                selectedLanguage = state.selectedNativeLanguage,
                onLanguageSelected = onNativeLanguageSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Level
            Text(
                text = "My level:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("beginner", "intermediate", "advanced").forEach { level ->
                    FilterChip(
                        selected = state.selectedLevel == level,
                        onClick = { onLevelSelected(level) },
                        label = { Text(level.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.selectedTargetLanguage != null && state.selectedNativeLanguage != null && state.selectedLevel != null
                ) {
                    Text("Submit")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip")
                }
            }

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LanguageChipRow(
    languages: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(languages) { language ->
            FilterChip(
                selected = selectedLanguage == language,
                onClick = { onLanguageSelected(language) },
                label = { Text(language) }
            )
        }
    }
}
