@file:OptIn(ExperimentalAnimationApi::class)

package presentation.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import expects.SetSystemBarsColor
import expects.isSystemInDarkTheme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.flag_cn
import lexicon.resources.generated.resources.flag_de
import lexicon.resources.generated.resources.flag_es
import lexicon.resources.generated.resources.flag_fr
import lexicon.resources.generated.resources.flag_gb
import lexicon.resources.generated.resources.flag_ir
import lexicon.resources.generated.resources.flag_it
import lexicon.resources.generated.resources.flag_jp
import lexicon.resources.generated.resources.flag_kr
import lexicon.resources.generated.resources.flag_nl
import lexicon.resources.generated.resources.flag_pt
import lexicon.resources.generated.resources.flag_ru
import lexicon.resources.generated.resources.flag_sa
import lexicon.resources.generated.resources.flag_tr
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import presentation.model.OnboardingUiState
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

private const val OnboardingTransitionDuration = 300
private const val OnboardingDisplayTotalSteps = 4

internal val languageNativeNames = mapOf(
    "English" to "ENGLISH",
    "German" to "DEUTSCH",
    "French" to "FRANÇAIS",
    "Spanish" to "ESPAÑOL",
    "Italian" to "ITALIANO",
    "Portuguese" to "PORTUGUÊS",
    "Dutch" to "NEDERLANDS",
    "Russian" to "РУССКИЙ",
    "Chinese" to "中文",
    "Japanese" to "日本語",
    "Korean" to "한국어",
    "Arabic" to "العربية",
    "Turkish" to "TÜRKÇE",
    "Persian" to "فارسی"
)

internal val languageFlags = mapOf(
    "English" to Res.drawable.flag_gb,
    "German" to Res.drawable.flag_de,
    "French" to Res.drawable.flag_fr,
    "Spanish" to Res.drawable.flag_es,
    "Italian" to Res.drawable.flag_it,
    "Portuguese" to Res.drawable.flag_pt,
    "Dutch" to Res.drawable.flag_nl,
    "Russian" to Res.drawable.flag_ru,
    "Chinese" to Res.drawable.flag_cn,
    "Japanese" to Res.drawable.flag_jp,
    "Korean" to Res.drawable.flag_kr,
    "Arabic" to Res.drawable.flag_sa,
    "Turkish" to Res.drawable.flag_tr,
    "Persian" to Res.drawable.flag_ir
)

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onTargetLanguageSelected: (String) -> Unit,
    onNativeLanguageSelected: (String) -> Unit,
    onLevelSelected: (String) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions
    val isDarkMode = isSystemInDarkTheme()

    // Set status bar appearance
    SetSystemBarsColor(
        statusBarColor = MaterialTheme.colorScheme.background,
        navigationBarColor = MaterialTheme.colorScheme.background,
        darkIcons = !isDarkMode
    )

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LinearProgressIndicator(
            progress = { state.currentStep.toFloat() / state.totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.progressBarHeight),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Only show step counter for actual numbered steps (not intro step)
        if (state.currentStep > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.extraSmall2, vertical = spacing.extraSmall2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.currentStep > 1) {
                    IconButton(
                        onClick = onPreviousStep,
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (state.isLoading) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                Text(
                    text = "STEP ${state.currentStep} OF $OnboardingDisplayTotalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        AnimatedContent(
            targetState = state.currentStep,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                val initialStep = initialState
                val targetStep = targetState
                val forward = targetStep > initialStep
                ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        animationSpec = tween(OnboardingTransitionDuration),
                        initialOffsetX = { if (forward) it else -it }
                    ) + fadeIn(animationSpec = tween(OnboardingTransitionDuration)),
                    initialContentExit = slideOutHorizontally(
                        animationSpec = tween(OnboardingTransitionDuration),
                        targetOffsetX = { if (forward) -it else it }
                    ) + fadeOut(animationSpec = tween(OnboardingTransitionDuration))
                )
            },
            label = "onboarding_step"
        ) { step ->
            when (step) {
                0 -> OnboardingIntroContent(
                    onContinue = onNextStep,
                    onSkip = onSkip,
                    spacing = spacing,
                    dimensions = dimensions
                )

                1 -> OnboardingStep1Content(
                    state = state,
                    onTargetLanguageSelected = onTargetLanguageSelected,
                    onNextStep = onNextStep,
                    onSkip = onSkip,
                    spacing = spacing,
                    dimensions = dimensions
                )

                2 -> OnboardingStep2Content(
                    state = state,
                    onNativeLanguageSelected = onNativeLanguageSelected,
                    onNextStep = onNextStep,
                    onSkip = onSkip,
                    spacing = spacing,
                    dimensions = dimensions
                )

                else -> OnboardingStep3Content(
                    state = state,
                    onLevelSelected = onLevelSelected,
                    onSubmit = onSubmit,
                    onBack = onPreviousStep,
                    spacing = spacing,
                    dimensions = dimensions
                )
            }
        }
    }
}

@Composable
private fun OnboardingIntroContent(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(spacing.extraLarge2))

        // Main content
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon or illustration
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMassive),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(spacing.large))

            // Welcome text
            Text(
                text = "Welcome to Lexicon!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.small))

            // Description
            Text(
                text = "We'll help you get started on your vocabulary learning journey",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.large))

            // Feature list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                IntroFeatureItem(
                    icon = Icons.Default.Settings,
                    title = "Personalized Setup",
                    description = "Choose your target language and learning level",
                    spacing = spacing,
                    dimensions = dimensions
                )

                IntroFeatureItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Starter Vocabulary",
                    description = "Get a curated pack of essential words to begin with",
                    spacing = spacing,
                    dimensions = dimensions
                )

                IntroFeatureItem(
                    icon = Icons.Default.School,
                    title = "Smart Learning",
                    description = "Track your progress with spaced repetition",
                    spacing = spacing,
                    dimensions = dimensions
                )
            }
        }

        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(dimensions.cardCornerRadius)
            ) {
                Text(
                    "Get Started",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.size(spacing.extraSmall2))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(dimensions.borderWidth, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(dimensions.cardCornerRadius)
            ) {
                Text(
                    "Start with Blank App",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))
        }
    }
}

@Composable
private fun IntroFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensions.iconSizeXLarge)
                .clip(RoundedCornerShape(spacing.extraSmall2))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun OnboardingStep1Content(
    state: OnboardingUiState,
    onTargetLanguageSelected: (String) -> Unit,
    onNextStep: () -> Unit,
    onSkip: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.extraSmall2)
        ) {
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = "Choose Your",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Target Language",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "We'll tailor your vocabulary sets based on your choice. You can change this later in settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.extraSmall2)
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            FlowRow(
                modifier = Modifier
                    .padding(vertical = Theme.spacing.extraSmall2)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
                maxItemsInEachRow = 2
            ) {
                state.availableLanguages.forEach { language ->
                    Box(modifier = Modifier.weight(1f)) {
                        LanguageGridCard(
                            language = language,
                            nativeName = languageNativeNames[language] ?: language.uppercase(),
                            flag = languageFlags[language],
                            selected = state.selectedTargetLanguage == language,
                            onClick = { onTargetLanguageSelected(language) },
                            spacing = spacing
                        )
                    }
                }
            }
        }

        // Fixed buttons at bottom
        OnboardingButtons(
            onPrimaryClick = onNextStep,
            onSecondaryClick = onSkip,
            primaryText = "Continue",
            secondaryText = "Skip",
            primaryEnabled = state.selectedTargetLanguage != null,
            spacing = spacing,
            dimensions = dimensions
        )
    }
}

@Composable
internal fun OnboardingButtons(
    primaryText: String,
    secondaryText: String,
    primaryEnabled: Boolean = true,
    spacing: AppSpacing,
    dimensions: AppDimensions,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            enabled = primaryEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        ) {
            Text(primaryText, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.size(spacing.extraSmall2))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium)
            )
        }
        Spacer(modifier = Modifier.height(spacing.extraSmall2))
        OutlinedButton(
            onClick = onSecondaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(dimensions.borderWidth, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        ) {
            Text(secondaryText, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(spacing.medium))
    }
}

@Composable
internal fun LanguageGridCard(
    language: String,
    nativeName: String,
    flag: DrawableResource?,
    selected: Boolean,
    onClick: () -> Unit,
    spacing: AppSpacing
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(spacing.small),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 0.dp,
            pressedElevation = 6.dp
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Check icon badge for selected state
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(spacing.extraSmall2)
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Flag container with circular background
                if (flag != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(spacing.extraSmall2))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(flag),
                            contentDescription = "$language flag",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(spacing.extraSmall3)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.extraSmall))

                // Language name
                Text(
                    text = language,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )

                // Native name
                Text(
                    text = nativeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun OnboardingStep2Content(
    state: OnboardingUiState,
    onNativeLanguageSelected: (String) -> Unit,
    onNextStep: () -> Unit,
    onSkip: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.extraSmall2)
        ) {
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = "Choose Your",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Native Language",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "We'll use this as the base for translations and hints.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.extraSmall2)
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            FlowRow(
                modifier = Modifier
                    .padding(vertical = Theme.spacing.extraSmall2)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
                maxItemsInEachRow = 2
            ) {
                // Filter out the target language from native language options
                state.availableLanguages
                    .filter { it != state.selectedTargetLanguage }
                    .forEach { language ->
                        Box(modifier = Modifier.weight(1f)) {
                            LanguageGridCard(
                                language = language,
                                nativeName = languageNativeNames[language] ?: language.uppercase(),
                                flag = languageFlags[language],
                                selected = state.selectedNativeLanguage == language,
                                onClick = { onNativeLanguageSelected(language) },
                                spacing = spacing
                            )
                        }
                    }
            }
        }

        // Fixed buttons at bottom
        OnboardingButtons(
            onPrimaryClick = onNextStep,
            onSecondaryClick = onSkip,
            primaryText = "Continue",
            secondaryText = "Skip",
            primaryEnabled = state.selectedNativeLanguage != null,
            spacing = spacing,
            dimensions = dimensions
        )
    }
}

@Composable
internal fun OnboardingStep3Content(
    state: OnboardingUiState,
    onLevelSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing.medium)
                    .padding(bottom = spacing.extraSmall2)
            ) {
                Spacer(modifier = Modifier.height(spacing.small))
                Text(
                    text = "What's your level?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "We'll tailor your vocabulary sets to match your current skills.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.extraSmall2)
                )
                Spacer(modifier = Modifier.height(spacing.medium))
                LevelCards(
                    selectedLevel = state.selectedLevel,
                    onLevelSelected = onLevelSelected,
                    spacing = spacing,
                    dimensions = dimensions,
                    enabled = !state.isLoading
                )
                Spacer(modifier = Modifier.height(spacing.small))
                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(spacing.extraSmall2))
                }
            }

            // Fixed buttons or loading at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isLoading) {
                    OnboardingLoadingCard(spacing = spacing, dimensions = dimensions)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            border = BorderStroke(
                                dimensions.borderWidth,
                                MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(dimensions.cardCornerRadius)
                        ) {
                            Text("Back", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = onSubmit,
                            modifier = Modifier.weight(1f),
                            enabled = state.selectedLevel != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(dimensions.cardCornerRadius)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(dimensions.iconSizeMedium)
                            )
                            Spacer(modifier = Modifier.size(spacing.extraSmall2))
                            Text("Finish", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(spacing.medium))
            }
        }

        // Invisible overlay to block all interactions during loading
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}

@Composable
internal fun OnboardingLoadingCard(
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    var currentTipIndex by remember { mutableStateOf(0) }
    val loadingTips = listOf(
        "Curating personalized vocabulary...",
        "Matching words to your level...",
        "Building your starter pack...",
        "Almost there..."
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            currentTipIndex = (currentTipIndex + 1) % loadingTips.size
        }
    }

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "loading_animation"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scale_animation"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(3000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.small),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(dimensions.cardCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
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
                        .graphicsLayer {
                            rotationZ = rotation
                        },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2)
            ) {
                Text(
                    text = "Generating Your Vocabulary",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                AnimatedContent(
                    targetState = loadingTips[currentTipIndex],
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(300)
                        ) with fadeOut(
                            animationSpec = tween(300)
                        )
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
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall, Alignment.CenterHorizontally)
            ) {
                repeat(4) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000,
                                delayMillis = index * 200,
                                easing = androidx.compose.animation.core.LinearEasing
                            ),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

internal val levelIcons = mapOf(
    "beginner" to Icons.Default.School,
    "intermediate" to Icons.AutoMirrored.Filled.TrendingUp,
    "advanced" to Icons.Default.Settings
)

@Composable
internal fun LevelCards(
    selectedLevel: String?,
    onLevelSelected: (String) -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
    enabled: Boolean = true
) {
    val levels = listOf(
        "beginner" to "I'm just starting out. I want to learn basic phrases and common everyday words.",
        "intermediate" to "I can hold basic conversations and want to expand my vocabulary and grammar patterns.",
        "advanced" to "I am fluent and looking to master nuances, idioms, and specialized professional topics."
    )
    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2)) {
        levels.forEach { (level, description) ->
            val selected = selectedLevel == level
            val levelIcon = levelIcons[level]
            Card(
                onClick = { if (enabled) onLevelSelected(level) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
                enabled = enabled,
                colors = CardDefaults.cardColors(
                    containerColor = if (selected)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(dimensions.cardCornerRadius),
                border = if (selected)
                    BorderStroke(
                        dimensions.borderWidth,
                        MaterialTheme.colorScheme.primary
                    )
                else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    levelIcon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSizeLarge),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(spacing.small))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = level.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = spacing.extraSmall4)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}