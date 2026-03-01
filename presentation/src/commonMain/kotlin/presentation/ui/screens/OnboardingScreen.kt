@file:OptIn(ExperimentalAnimationApi::class)

package presentation.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        // Segmented step progress — only shown for actual steps (not intro)
        if (state.currentStep > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.extraSmall2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2)
            ) {
                if (state.currentStep > 1) {
                    IconButton(
                        onClick = onPreviousStep,
                        enabled = !state.isLoading,
                        modifier = Modifier.size(dimensions.touchTargetSmall)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (state.isLoading)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(dimensions.touchTargetSmall))
                }

                // Animated pill segments
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall3)
                ) {
                    repeat(state.totalSteps) { index ->
                        val filled = index < state.currentStep
                        val segmentColor by animateColorAsState(
                            targetValue = if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            animationSpec = tween(300),
                            label = "segment_$index"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(Theme.spacing.xxs)
                                .clip(RoundedCornerShape(Theme.spacing.xxxs))
                                .background(segmentColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(dimensions.touchTargetSmall))
            }
        }

        AnimatedContent(
            targetState = state.currentStep,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                val forward = targetState > initialState
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
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gradient hero circle
            Box(contentAlignment = Alignment.Center) {
                // Outer soft glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Inner gradient circle
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.large))

            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Lexicon",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.extraSmall2))

            Text(
                text = "Let's personalize your vocabulary learning experience",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )

            Spacer(modifier = Modifier.height(spacing.large))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2)
            ) {
                IntroFeatureItem(
                    icon = Icons.Default.Settings,
                    title = "Personalized Setup",
                    description = "Choose your target language and learning level",
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                IntroFeatureItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Starter Vocabulary",
                    description = "Get a curated pack of essential words to begin with",
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                )
                IntroFeatureItem(
                    icon = Icons.Default.School,
                    title = "Smart Learning",
                    description = "Track your progress with spaced repetition",
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    iconBackground = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall3)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text("Get Started", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.size(spacing.extraSmall2))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth)
            ) {
                Text(
                    "Start with Blank App",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    iconTint: Color,
    iconBackground: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.touchTarget)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSize),
                tint = iconTint
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
private fun StepHeadline(
    line1: String,
    line2: String,
    subtitle: String,
    spacing: AppSpacing
) {
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
internal fun LanguageGrid(
    languages: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit,
    spacing: AppSpacing
) {
    val rows = (languages.size + 1) / 2
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                val firstIndex = rowIndex * 2
                LanguageGridCard(
                    language = languages[firstIndex],
                    nativeName = languageNativeNames[languages[firstIndex]] ?: languages[firstIndex].uppercase(),
                    flag = languageFlags[languages[firstIndex]],
                    selected = selectedLanguage == languages[firstIndex],
                    onClick = { onLanguageSelected(languages[firstIndex]) },
                    modifier = Modifier.weight(1f)
                )
                val secondIndex = firstIndex + 1
                if (secondIndex < languages.size) {
                    LanguageGridCard(
                        language = languages[secondIndex],
                        nativeName = languageNativeNames[languages[secondIndex]] ?: languages[secondIndex].uppercase(),
                        flag = languageFlags[languages[secondIndex]],
                        selected = selectedLanguage == languages[secondIndex],
                        onClick = { onLanguageSelected(languages[secondIndex]) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
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
                line1 = "Which language",
                line2 = "are you learning?",
                subtitle = "We'll tailor your vocabulary sets based on your choice. You can change this later.",
                spacing = spacing
            )
            Spacer(modifier = Modifier.height(spacing.md))

            LanguageGrid(
                languages = state.availableLanguages,
                selectedLanguage = state.selectedTargetLanguage,
                onLanguageSelected = onTargetLanguageSelected,
                spacing = spacing
            )
        }

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
                .widthIn(max = Theme.dimensions.contentMaxWidth),
            enabled = primaryEnabled,
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(50)
        ) {
            Text(primaryText, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.size(spacing.extraSmall2))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium)
            )
        }

        TextButton(
            onClick = onSecondaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = Theme.dimensions.contentMaxWidth)
        ) {
            Text(
                secondaryText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bg_$language"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "border_$language"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) Theme.elevation.medium else Theme.elevation.none
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Theme.spacing.xs)
                        .size(22.dp)
                        .clip(CircleShape)
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
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.md, horizontal = Theme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
            ) {
                if (flag != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(Theme.shapes.medium))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(flag),
                            contentDescription = "$language flag",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(Theme.shapes.small)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Text(
                    text = language,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = nativeName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
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
                line1 = "What's your",
                line2 = "native language?",
                subtitle = "We'll use this as the base for translations and hints.",
                spacing = spacing
            )
            Spacer(modifier = Modifier.height(spacing.md))

            LanguageGrid(
                languages = state.availableLanguages.filter { it != state.selectedTargetLanguage },
                selectedLanguage = state.selectedNativeLanguage,
                onLanguageSelected = onNativeLanguageSelected,
                spacing = spacing
            )
        }

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
                    line1 = "What's your",
                    line2 = "current level?",
                    subtitle = "We'll tailor your vocabulary sets to match your current skills.",
                    spacing = spacing
                )
                Spacer(modifier = Modifier.height(spacing.md))
                LevelCards(
                    selectedLevel = state.selectedLevel,
                    onLevelSelected = onLevelSelected,
                    spacing = spacing,
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
                if (state.isLoading) {
                    OnboardingLoadingCard(spacing = spacing)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = Theme.dimensions.contentMaxWidth),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Text(
                                "Back",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onSubmit,
                            modifier = Modifier.weight(2f),
                            enabled = state.selectedLevel != null,
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
                                modifier = Modifier.size(dimensions.iconSizeMedium)
                            )
                            Spacer(modifier = Modifier.size(spacing.xs))
                            Text("Let's Go!", style = MaterialTheme.typography.labelLarge)
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
internal fun OnboardingLoadingCard(
    spacing: AppSpacing
) {
    var currentTipIndex by remember { mutableStateOf(0) }
    val loadingTips = listOf(
        "Curating personalized vocabulary...",
        "Matching words to your level...",
        "Building your starter pack...",
        "Almost there..."
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            currentTipIndex = (currentTipIndex + 1) % loadingTips.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
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
        shape = RoundedCornerShape(Theme.shapes.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.medium)
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
                        .clip(CircleShape)
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
                        .graphicsLayer { rotationZ = rotation },
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
                        fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
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
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000,
                                delayMillis = index * 200,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(Theme.spacing.xs)
                            .clip(CircleShape)
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
    "advanced" to Icons.Default.Star
)

@Composable
internal fun LevelCards(
    selectedLevel: String?,
    onLevelSelected: (String) -> Unit,
    spacing: AppSpacing,
    enabled: Boolean = true
) {
    val levels = listOf(
        "beginner" to "Just starting out. Learn basic phrases and everyday words.",
        "intermediate" to "Hold basic conversations. Expand vocabulary and grammar patterns.",
        "advanced" to "Fluent speaker. Master nuances, idioms, and specialized topics."
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        levels.forEach { (level, description) ->
            LevelTile(
                level = level,
                description = description,
                icon = levelIcons[level],
                levelColor = when (level) {
                    "beginner" -> MaterialTheme.colorScheme.secondary
                    "intermediate" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                selected = selectedLevel == level,
                enabled = enabled,
                onClick = { onLevelSelected(level) }
            )
        }
    }
}

@Composable
private fun LevelTile(
    level: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    levelColor: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) levelColor.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "level_bg_$level"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) levelColor
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "level_border_$level"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = bgColor),
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
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(levelColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.md, horizontal = Theme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(Theme.dimensions.touchTarget)
                        .clip(RoundedCornerShape(14.dp))
                        .background(levelColor.copy(alpha = if (selected) 0.18f else 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSize),
                            tint = levelColor
                        )
                    }
                }

                Text(
                    text = level.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) levelColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
