package presentation.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import presentation.model.OnboardingUiState
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.flag_gb
import vokab.resources.generated.resources.flag_de
import vokab.resources.generated.resources.flag_fr
import vokab.resources.generated.resources.flag_es
import vokab.resources.generated.resources.flag_it
import vokab.resources.generated.resources.flag_pt
import vokab.resources.generated.resources.flag_nl
import vokab.resources.generated.resources.flag_ru
import vokab.resources.generated.resources.flag_cn
import vokab.resources.generated.resources.flag_jp
import vokab.resources.generated.resources.flag_kr
import vokab.resources.generated.resources.flag_sa
import vokab.resources.generated.resources.flag_tr
import vokab.resources.generated.resources.flag_ir

private const val OnboardingTransitionDuration = 300
private const val OnboardingDisplayTotalSteps = 4

private val languageNativeNames = mapOf(
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

private val languageFlags = mapOf(
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
    ) {
        LinearProgressIndicator(
            progress = { state.currentStep.toFloat() / state.totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.progressBarHeight),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.extraSmall2, vertical = spacing.extraSmall2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (state.currentStep > 1) {
                IconButton(onClick = onPreviousStep) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
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
private fun OnboardingStep1Content(
    state: OnboardingUiState,
    onTargetLanguageSelected: (String) -> Unit,
    onNextStep: () -> Unit,
    onSkip: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(state.availableLanguages, searchQuery) {
        state.availableLanguages.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = spacing.medium)
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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search languages...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        )
        Spacer(modifier = Modifier.height(spacing.small))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            items(filteredLanguages) { language ->
                LanguageGridCard(
                    language = language,
                    nativeName = languageNativeNames[language] ?: language.uppercase(),
                    flag = languageFlags[language],
                    selected = state.selectedTargetLanguage == language,
                    onClick = { onTargetLanguageSelected(language) },
                    spacing = spacing,
                    dimensions = dimensions
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onNextStep,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedTargetLanguage != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        ) {
            Text("Continue", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.size(spacing.extraSmall2))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium)
            )
        }
        Spacer(modifier = Modifier.height(spacing.extraSmall2))
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            border = BorderStroke(dimensions.borderWidth, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        ) {
            Text("Skip", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(spacing.medium))
    }
}

@Composable
private fun LanguageGridCard(
    language: String,
    nativeName: String,
    flag: DrawableResource?,
    selected: Boolean,
    onClick: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensions.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(if (selected) dimensions.borderWidthThick else dimensions.borderWidth, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(spacing.extraSmall2),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (flag != null) {
                        Image(
                            painter = painterResource(flag),
                            contentDescription = "$language flag",
                            modifier = Modifier.height(32.dp),
                            contentScale = ContentScale.FillHeight
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.extraSmall2))
                Text(
                    text = language,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = nativeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OnboardingStep2Content(
    state: OnboardingUiState,
    onNativeLanguageSelected: (String) -> Unit,
    onNextStep: () -> Unit,
    onSkip: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(state.availableLanguages, searchQuery) {
        state.availableLanguages.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = spacing.medium)
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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search languages...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        )
        Spacer(modifier = Modifier.height(spacing.small))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            items(filteredLanguages) { language ->
                LanguageGridCard(
                    language = language,
                    nativeName = languageNativeNames[language] ?: language.uppercase(),
                    flag = languageFlags[language],
                    selected = state.selectedNativeLanguage == language,
                    onClick = { onNativeLanguageSelected(language) },
                    spacing = spacing,
                    dimensions = dimensions
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onNextStep,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedNativeLanguage != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        ) {
            Text("Continue", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.size(spacing.extraSmall2))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium)
            )
        }
        Spacer(modifier = Modifier.height(spacing.extraSmall2))
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            border = BorderStroke(dimensions.borderWidth, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(dimensions.cardCornerRadius)
        ) {
            Text("Skip", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(spacing.medium))
    }
}

@Composable
private fun OnboardingStep3Content(
    state: OnboardingUiState,
    onLevelSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = spacing.medium)
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
            dimensions = dimensions
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
        Spacer(modifier = Modifier.weight(1f))
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.medium),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(dimensions.borderWidth, MaterialTheme.colorScheme.outline),
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

private val levelIcons = mapOf(
    "beginner" to Icons.Default.School,
    "intermediate" to Icons.AutoMirrored.Filled.TrendingUp,
    "advanced" to Icons.Default.Settings
)

@Composable
private fun LevelCards(
    selectedLevel: String?,
    onLevelSelected: (String) -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
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
                onClick = { onLevelSelected(level) },
                modifier = Modifier.fillMaxWidth(),
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
