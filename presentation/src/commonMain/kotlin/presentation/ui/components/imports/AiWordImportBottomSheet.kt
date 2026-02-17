@file:OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)

package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import events.OnEvents
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.aiimport.AiWordImportViewModel
import presentation.model.AiWordImportStep
import presentation.model.AiWordImportUiState
import presentation.ui.screens.LanguageGridCard
import presentation.ui.screens.OnboardingLoadingCard
import presentation.ui.screens.languageFlags
import presentation.ui.screens.languageNativeNames
import presentation.ui.screens.levelIcons
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

private const val AiWizardTransitionDuration = 300
private const val AiWizardTotalSteps = 4

private val topicEmojis = mapOf(
    "Daily Life" to "🏠",
    "Travel" to "✈️",
    "Business" to "💼",
    "Food" to "🍽️",
    "Technology" to "💻",
    "Sports" to "⚽",
    "Health" to "❤️",
    "Arts" to "🎨",
    "Nature" to "🌿",
    "Academic" to "📚"
)

// ──────────────────────────────────────────────
// Import Method Selector
// ──────────────────────────────────────────────

@Composable
fun ImportMethodSelectorContent(
    onManual: () -> Unit,
    onAiAssistant: () -> Unit,
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }

        Column(
            modifier = Modifier.padding(
                horizontal = spacing.medium,
                vertical = spacing.small
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2)
        ) {
            Text(
                text = "Add Words",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose how you'd like to build your vocabulary",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(spacing.extraSmall2))

        // AI card — gradient featured option
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .clip(RoundedCornerShape(dimensions.cardCornerRadius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(spacing.small)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
            )
            Card(
                onClick = onAiAssistant,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensions.cardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(spacing.extraSmall))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2)
                        ) {
                            Text(
                                text = "AI Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Get personalized vocabulary tailored to your level and interests",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.extraSmall2))

        // OR divider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "or",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(spacing.extraSmall2))

        // Manual card
        Card(
            onClick = onManual,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium),
            shape = RoundedCornerShape(dimensions.cardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(spacing.extraSmall))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manually",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Type, paste, or import from file or image",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))
    }
}

// ──────────────────────────────────────────────
// AI Word Import Wizard
// ──────────────────────────────────────────────

@Composable
fun AiWordImportBottomSheet(
    onDismiss: () -> Unit,
    onShowSnackBar: (String) -> Unit,
) {
    val viewModel = koinViewModel<AiWordImportViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions
    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleDismiss = {
        viewModel.reset()
        onDismiss()
    }

    OnEvents(viewModel.events) { event ->
        when (event) {
            is AiWordImportViewModel.Event.ImportSuccess -> {
                onShowSnackBar("Added ${event.count} words to your library!")
                handleDismiss()
            }

            is AiWordImportViewModel.Event.Dismiss -> handleDismiss()
        }
    }

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val stepIndex = state.step.ordinal
        val isPreview = state.step == AiWordImportStep.PREVIEW

        val progressTarget = if (isPreview) 1f else (stepIndex + 1).toFloat() / AiWizardTotalSteps
        val animatedProgress by animateFloatAsState(
            targetValue = progressTarget,
            animationSpec = tween(durationMillis = AiWizardTransitionDuration),
            label = "wizard_progress"
        )
        val progressColor by animateColorAsState(
            targetValue = if (isPreview) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.primary,
            animationSpec = tween(durationMillis = AiWizardTransitionDuration),
            label = "progress_color"
        )

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.progressBarHeight),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Nav bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.extraSmall2, vertical = spacing.extraSmall2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (state.step != AiWordImportStep.TARGET_LANG && !isPreview && !state.isLoading) {
                IconButton(onClick = { viewModel.previousStep() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            if (!isPreview) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Step ${stepIndex + 1} of $AiWizardTotalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = spacing.extraSmall,
                            vertical = spacing.extraSmall3
                        )
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall3)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AI Suggestions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = { if (isPreview) showDiscardDialog = true else handleDismiss() },
                enabled = !state.isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (state.isLoading) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        AnimatedContent(
            targetState = state.step,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        animationSpec = tween(AiWizardTransitionDuration),
                        initialOffsetX = { if (forward) it else -it }
                    ) + fadeIn(animationSpec = tween(AiWizardTransitionDuration)),
                    initialContentExit = slideOutHorizontally(
                        animationSpec = tween(AiWizardTransitionDuration),
                        targetOffsetX = { if (forward) -it else it }
                    ) + fadeOut(animationSpec = tween(AiWizardTransitionDuration))
                )
            },
            label = "ai_wizard_step"
        ) { step ->
            when (step) {
                AiWordImportStep.TARGET_LANG -> AiLanguageStep(
                    title = "Which language",
                    highlight = "do you want to learn?",
                    subtitle = "We'll tailor your vocabulary to your chosen language.",
                    languages = state.availableLanguages,
                    selectedLanguage = state.selectedTargetLanguage,
                    onLanguageSelected = viewModel::selectTargetLanguage,
                    onContinue = { viewModel.nextStep() },
                    onCancel = handleDismiss,
                    spacing = spacing,
                    dimensions = dimensions
                )

                AiWordImportStep.NATIVE_LANG -> AiLanguageStep(
                    title = "What's your",
                    highlight = "native language?",
                    subtitle = "We'll use this as the base for translations.",
                    languages = state.availableLanguages.filter { it != state.selectedTargetLanguage },
                    selectedLanguage = state.selectedNativeLanguage,
                    onLanguageSelected = viewModel::selectNativeLanguage,
                    onContinue = { viewModel.nextStep() },
                    onCancel = handleDismiss,
                    spacing = spacing,
                    dimensions = dimensions
                )

                AiWordImportStep.LEVEL -> AiLevelStep(
                    selectedLevel = state.selectedLevel,
                    error = state.error,
                    onLevelSelected = viewModel::selectLevel,
                    onContinue = { viewModel.nextStep() },
                    spacing = spacing,
                    dimensions = dimensions
                )

                AiWordImportStep.TOPICS -> AiTopicsStep(
                    state = state,
                    onToggleTopic = viewModel::toggleTopic,
                    onGenerate = { viewModel.submit() },
                    spacing = spacing,
                    dimensions = dimensions
                )

                AiWordImportStep.PREVIEW -> AiWordPreviewStep(
                    state = state,
                    onToggleWord = viewModel::toggleWordSelection,
                    onImport = { viewModel.importSelected() },
                    spacing = spacing,
                    dimensions = dimensions
                )
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    text = "Discard suggestions?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Your AI-generated vocabulary list will be lost.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        handleDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep")
                }
            }
        )
    }
}

// ──────────────────────────────────────────────
// Shared wizard step header
// ──────────────────────────────────────────────

@Composable
private fun AiStepHeader(
    title: String,
    highlight: String,
    subtitle: String,
    spacing: AppSpacing
) {
    Spacer(modifier = Modifier.height(spacing.small))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = highlight,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = spacing.extraSmall2)
    )
    Spacer(modifier = Modifier.height(spacing.medium))
}

// ──────────────────────────────────────────────
// Step 1 & 2: Language selection
// ──────────────────────────────────────────────

@Composable
private fun AiLanguageStep(
    title: String,
    highlight: String,
    subtitle: String,
    languages: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
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
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.extraSmall2)
        ) {
            AiStepHeader(
                title = title,
                highlight = highlight,
                subtitle = subtitle,
                spacing = spacing
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
                maxItemsInEachRow = 2
            ) {
                languages.forEach { language ->
                    Box(modifier = Modifier.weight(1f)) {
                        LanguageGridCard(
                            language = language,
                            nativeName = languageNativeNames[language] ?: language.uppercase(),
                            flag = languageFlags[language],
                            selected = selectedLanguage == language,
                            onClick = { onLanguageSelected(language) },
                            spacing = spacing
                        )
                    }
                }
            }
        }

        // Bottom bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                enabled = selectedLanguage != null,
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
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(spacing.extraSmall2))
        }
    }
}

// ──────────────────────────────────────────────
// Step 3: Level selection
// ──────────────────────────────────────────────

@Composable
private fun AiLevelStep(
    selectedLevel: String?,
    error: String?,
    onLevelSelected: (String) -> Unit,
    onContinue: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    val scrollState = rememberScrollState()
    val levels = listOf(
        "beginner" to "I'm just starting out. I want to learn basic phrases and common everyday words.",
        "intermediate" to "I can hold basic conversations and want to expand my vocabulary.",
        "advanced" to "I'm fluent and looking to master nuances, idioms, and specialized topics."
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.extraSmall2)
        ) {
            AiStepHeader(
                title = "What's your",
                highlight = "current level?",
                subtitle = "We'll match the vocabulary difficulty to your skills.",
                spacing = spacing
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2)) {
                levels.forEach { (level, description) ->
                    val selected = selectedLevel == level
                    val icon = levelIcons[level]

                    val bgColor by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "level_bg"
                    )

                    Card(
                        onClick = { onLevelSelected(level) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(dimensions.cardCornerRadius),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selected) 2.dp else 0.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            icon?.let {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.15f
                                            )
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.size(spacing.small))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = level.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                        alpha = 0.75f
                                    )
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
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

            error?.let {
                Spacer(modifier = Modifier.height(spacing.extraSmall2))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Bottom bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                enabled = selectedLevel != null,
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
            Spacer(modifier = Modifier.height(spacing.medium))
        }
    }
}

// ──────────────────────────────────────────────
// Step 4: Topics
// ──────────────────────────────────────────────

@Composable
private fun AiTopicsStep(
    state: AiWordImportUiState,
    onToggleTopic: (String) -> Unit,
    onGenerate: () -> Unit,
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
                    .padding(horizontal = spacing.medium)
                    .padding(bottom = spacing.extraSmall2)
            ) {
                AiStepHeader(
                    title = "What topics",
                    highlight = "interest you?",
                    subtitle = "Pick any that appeal to you. We'll mix them to create a varied vocabulary pack.",
                    spacing = spacing
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall2),
                    maxItemsInEachRow = 2
                ) {
                    state.availableTopics.forEach { topic ->
                        val selected = state.selectedTopics.contains(topic)
                        val emoji = topicEmojis[topic] ?: "📖"

                        val bgColor by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "chip_bg"
                        )
                        val borderColor by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "chip_border"
                        )

                        Card(
                            onClick = { onToggleTopic(topic) },
                            enabled = !state.isLoading,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(dimensions.cardCornerRadius),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = borderColor
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (selected) 2.dp else 0.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = spacing.extraSmall,
                                        vertical = spacing.extraSmall
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall3)
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    autoSize = TextAutoSize.StepBased(
                                        maxFontSize = 14.sp,
                                        minFontSize = 12.sp
                                    )
                                )
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                state.error?.let {
                    Spacer(modifier = Modifier.height(spacing.extraSmall2))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Bottom CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isLoading) {
                    OnboardingLoadingCard(spacing = spacing, dimensions = dimensions)
                } else {
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(dimensions.cardCornerRadius)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Spacer(modifier = Modifier.size(spacing.extraSmall2))
                        Text("Generate Words", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(spacing.medium))
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent() } } }
            )
        }
    }
}

// ──────────────────────────────────────────────
// Step 5: Word Preview
// ──────────────────────────────────────────────

@Composable
private fun AiWordPreviewStep(
    state: AiWordImportUiState,
    onToggleWord: (Int) -> Unit,
    onImport: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions
) {
    val scrollState = rememberScrollState()
    val selectedCount = state.selectedWordIndices.size
    val totalCount = state.suggestedWords.size
    val allSelected = selectedCount == totalCount && totalCount > 0

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.extraSmall2)
        ) {
            Spacer(modifier = Modifier.height(spacing.small))

            // Header row with word count badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your personalized",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "vocabulary",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$totalCount words",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            horizontal = spacing.extraSmall,
                            vertical = spacing.extraSmall3
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.extraSmall2))

            // Select all / Deselect all row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (allSelected) "All selected" else "$selectedCount of $totalCount selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        if (allSelected) {
                            state.suggestedWords.indices.forEach { onToggleWord(it) }
                        } else {
                            state.suggestedWords.indices
                                .filter { it !in state.selectedWordIndices }
                                .forEach { onToggleWord(it) }
                        }
                    }
                ) {
                    Text(
                        text = if (allSelected) "Deselect all" else "Select all",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.extraSmall2))

            state.suggestedWords.forEachIndexed { index, word ->
                val isSelected = state.selectedWordIndices.contains(index)

                val bgColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "word_card_bg"
                )

                Card(
                    onClick = { onToggleWord(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.extraSmall2),
                    shape = RoundedCornerShape(dimensions.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 2.dp else 0.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = word.originalWord,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = word.translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.8f
                                )
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (word.description.isNotBlank()) {
                                Text(
                                    text = word.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                        alpha = 0.6f
                                    )
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            state.error?.let {
                Spacer(modifier = Modifier.height(spacing.extraSmall2))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Bottom action bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(spacing.small))
                if (state.isLoading) {
                    Row(
                        modifier = Modifier.padding(vertical = spacing.small),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(spacing.extraSmall2))
                        Text(
                            text = "Adding to your library…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Button(
                        onClick = onImport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp),
                        enabled = selectedCount > 0,
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
                        Text(
                            text = if (selectedCount > 0) "Add $selectedCount Words to Library"
                            else "Select words to add",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.small))
            }
        }
    }
}
