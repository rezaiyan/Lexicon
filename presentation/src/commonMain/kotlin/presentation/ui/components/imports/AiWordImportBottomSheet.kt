@file:OptIn(ExperimentalAnimationApi::class)

package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import events.OnEvents
import org.koin.compose.viewmodel.koinViewModel
import feature.aiimport.AiWordImportViewModel
import feature.aiimport.model.AiWordImportStep
import theme.Theme

private const val AiWizardTransitionDuration = 300
private const val AiWizardTotalSteps = 4

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
        modifier = Modifier.fillMaxSize()
    ) {
        val stepIndex = state.step.ordinal
        val isPreview = state.step == AiWordImportStep.PREVIEW

        WizardNavigationBar(
            stepIndex = stepIndex,
            isPreview = isPreview,
            isFirstStep = state.step == AiWordImportStep.TARGET_LANG,
            isLoading = state.isLoading,
            onBack = { viewModel.previousStep() },
            onClose = {
                if (isPreview) {
                    showDiscardConfirmation = true
                } else {
                    handleDismiss()
                }
            },
        )

        AnimatedContent(
            targetState = state.step,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(spacing.md),
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

@Composable
private fun WizardNavigationBar(
    stepIndex: Int,
    isPreview: Boolean,
    isFirstStep: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val spacing = Theme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.extraSmall2, vertical = spacing.extraSmall2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isFirstStep && !isPreview && !isLoading) {
            IconButton(onClick = onBack) {
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
            StepProgressSegments(
                stepIndex = stepIndex,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.extraSmall2),
            )
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
            onClick = onClose,
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = if (isLoading)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StepProgressSegments(
    stepIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3)
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
}
