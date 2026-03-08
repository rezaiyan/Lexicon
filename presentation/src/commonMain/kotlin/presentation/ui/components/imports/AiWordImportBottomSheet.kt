@file:OptIn(ExperimentalAnimationApi::class)

package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.LexiconDialogContent
import events.OnEvents
import org.koin.compose.viewmodel.koinViewModel
import feature.aiimport.AiWordImportViewModel
import feature.aiimport.model.AiWordImportStep
import feature.aiimport.model.AiWordImportUiState
import feature.onboarding.ui.components.LanguageGrid
import feature.onboarding.ui.components.LevelCards
import feature.onboarding.ui.components.OnboardingLoadingCard
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

private const val AiWizardTransitionDuration = 300
private const val AiWizardTotalSteps = 4

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
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
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

        // AI card — gradient featured option
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.none)
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(Theme.dimensions.iconSizeLarge)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                    )
                }
            }
        }

        // OR divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "or",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        // Manual card
        Card(
            onClick = onManual,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimensions.cardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(Theme.dimensions.borderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Theme.dimensions.iconSize)
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
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                )
            }
        }

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
    val state by viewModel.state()
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    val handleDismiss = {
        viewModel.reset()
        onDismiss()
    }

    OnEvents(viewModel.effects) { event ->
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

        // Nav row: back | pill segments (or AI Suggestions label) | close
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
                Spacer(modifier = Modifier.size(Theme.dimensions.touchTarget))
            }

            if (!isPreview) {
                // Animated pill segments
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.extraSmall2),
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall3)
                ) {
                    repeat(AiWizardTotalSteps) { index ->
                        val filled = index <= stepIndex
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
                                .clip(RoundedCornerShape(2.dp))
                                .background(segmentColor)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Theme.dimensions.iconSizeSmall)
                    )
                    Spacer(modifier = Modifier.size(spacing.extraSmall3))
                    Text(
                        text = "AI Suggestions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = {
                    if (isPreview) {
                        showDiscardConfirmation = true
                    } else {
                        handleDismiss()
                    }
                },
                enabled = !state.isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (state.isLoading)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface
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

    if (showDiscardConfirmation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .pointerInput(Unit) { /* consume touches */ },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(Theme.shapes.large),
                tonalElevation = 6.dp,
            ) {
                DiscardConfirmationContent(
                    onDiscard = {
                        showDiscardConfirmation = false
                        handleDismiss()
                    },
                    onKeep = { showDiscardConfirmation = false }
                )
            }
        }
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
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(spacing.extraSmall3))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(horizontal = spacing.md)
                .padding(bottom = spacing.xs)
        ) {
            AiStepHeader(
                title = title,
                highlight = highlight,
                subtitle = subtitle,
                spacing = spacing
            )

            LanguageGrid(
                languages = languages,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth),
                enabled = selectedLanguage != null,
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = spacing.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text("Continue", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.size(spacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth)
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(spacing.md))
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

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.md)
                .padding(bottom = spacing.xs)
        ) {
            AiStepHeader(
                title = "What's your",
                highlight = "current level?",
                subtitle = "We'll match the vocabulary difficulty to your skills.",
                spacing = spacing
            )

            LevelCards(
                selectedLevel = selectedLevel,
                onLevelSelected = onLevelSelected
            )

            error?.let {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = it,
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
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth),
                enabled = selectedLevel != null,
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = spacing.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text("Continue", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.size(spacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }
            Spacer(modifier = Modifier.height(spacing.md))
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
                    .padding(horizontal = spacing.md)
                    .padding(bottom = spacing.xs)
            ) {
                AiStepHeader(
                    title = "What topics",
                    highlight = "interest you?",
                    subtitle = "Pick any that appeal to you. We'll mix them to create a varied vocabulary pack.",
                    spacing = spacing
                )

                // 2-column grid of topic tiles
                val topics = state.availableTopics
                val rows = (topics.size + 1) / 2
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    repeat(rows) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            val firstIndex = rowIndex * 2
                            TopicTile(
                                topic = topics[firstIndex],
                                emoji = "",
                                selected = state.selectedTopics.contains(topics[firstIndex]),
                                enabled = !state.isLoading,
                                onClick = { onToggleTopic(topics[firstIndex]) },
                                modifier = Modifier.weight(1f)
                            )
                            val secondIndex = firstIndex + 1
                            if (secondIndex < topics.size) {
                                TopicTile(
                                    topic = topics[secondIndex],
                                    emoji = "",
                                    selected = state.selectedTopics.contains(topics[secondIndex]),
                                    enabled = !state.isLoading,
                                    onClick = { onToggleTopic(topics[secondIndex]) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                state.error?.let {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = it,
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
                    OnboardingLoadingCard()
                } else {
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = Theme.dimensions.contentMaxWidth),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = spacing.lg),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Spacer(modifier = Modifier.size(spacing.xs))
                        Text("Generate Words", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(spacing.md))
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                    }
            )
        }
    }
}

@Composable
private fun TopicTile(
    topic: String,
    emoji: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tile_bg_$topic"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tile_border_$topic"
    )

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) Theme.elevation.medium else Theme.elevation.none
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Check badge in top-right corner
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
                        contentDescription = null,
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
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = topic,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
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
                .padding(horizontal = spacing.md)
                .padding(bottom = spacing.xs)
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
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
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
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "word_card_bg_$index"
                )

                Card(
                    onClick = { onToggleWord(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.extraSmall2),
                    shape = RoundedCornerShape(Theme.shapes.large),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) Theme.elevation.medium else Theme.elevation.none
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
                                .size(26.dp)
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
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
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
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (word.description.isNotBlank()) {
                                Text(
                                    text = word.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = Theme.spacing.xxxs)
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
            shadowElevation = Theme.elevation.overlay,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(spacing.md))
                if (state.isLoading) {
                    Row(
                        modifier = Modifier.padding(vertical = spacing.small),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
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
                            .widthIn(max = Theme.dimensions.contentMaxWidth),
                        enabled = selectedCount > 0,
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = Theme.spacing.lg),
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
                        Spacer(modifier = Modifier.size(spacing.extraSmall2))
                        Text(
                            text = if (selectedCount > 0) "Add $selectedCount Words to Library"
                            else "Select words to add",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.md))
            }
        }
    }
}

@Composable
private fun DiscardConfirmationContent(
    onDiscard: () -> Unit,
    onKeep: () -> Unit,
) {
    LexiconDialogContent(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg)
            .padding(bottom = Theme.spacing.lg),
        title = "Discard suggestions?",
        message = "Your AI-generated vocabulary list will be lost.",
        primaryButton = ButtonState(
            text = "Discard",
            onClick = onDiscard,
            type = ButtonType.Error
        ),
        secondaryButton = ButtonState(
            text = "Keep",
            onClick = onKeep
        ),
    )
}
