package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import components.sheet.StepProgressBar
import events.OnEvents
import expects.BackHandler
import feature.aiimport.AiWordImportViewModel
import feature.aiimport.model.AiWordImportEffect
import feature.aiimport.model.AiWordImportStep
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.content_description_back
import lexicon.resources.generated.resources.content_description_close
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

internal val AiWizardTotalSteps = AiWordImportStep.entries.size

private data class AiImportResult(val count: Int, val previewWords: List<String>)

/**
 * AI wizard: target language → native language → level → topics → (generating) → pick words → success.
 * Draws its own top bar (back · step progress · close) because the progress sits between the buttons.
 */
@Composable
fun AiWordImportBottomSheet(
    onDismiss: () -> Unit,
    onBackToChooser: (() -> Unit)? = null,
    onStartReview: (() -> Unit)? = null,
) {
    val viewModel = koinViewModel<AiWordImportViewModel>()
    val state by viewModel.state()
    val motion = Theme.motion
    var showDiscard by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AiImportResult?>(null) }
    // Words chosen on the preview, captured before import resets the suggestions
    var pendingPreviewWords by remember { mutableStateOf(emptyList<String>()) }

    val handleDismiss = {
        viewModel.reset()
        onDismiss()
    }

    OnEvents(viewModel.effects) { event ->
        when (event) {
            is AiWordImportEffect.ImportSuccess -> result = AiImportResult(event.count, pendingPreviewWords)
            is AiWordImportEffect.Dismiss -> handleDismiss()
        }
    }

    val isPreview = state.step == AiWordImportStep.PREVIEW
    val hasUnsavedWork = isPreview || state.isLoading
    val requestClose = { if (hasUnsavedWork) showDiscard = true else handleDismiss() }
    val goBack = {
        when {
            hasUnsavedWork -> showDiscard = true
            state.step == AiWordImportStep.TARGET_LANG -> {
                viewModel.reset()
                onBackToChooser?.invoke() ?: onDismiss()
            }
            else -> viewModel.previousStep()
        }
    }

    // Composed inside the outer pager's page, so it takes precedence over the pager's back handler
    BackHandler(enabled = result == null) { goBack() }

    if (showDiscard) {
        DiscardConfirmationDialog(
            onDiscard = {
                showDiscard = false
                handleDismiss()
            },
            onKeep = { showDiscard = false },
        )
    }

    val finished = result
    if (finished != null) {
        Column {
            WizardTopBar(stepIndex = null, showBack = false, onBack = {}, onClose = handleDismiss)
            ImportSuccessContent(
                count = finished.count,
                previewWords = finished.previewWords,
                onStartReview = onStartReview?.let { start -> { viewModel.reset(); start() } },
                onAddMore = {
                    viewModel.reset()
                    onBackToChooser?.invoke() ?: handleDismiss()
                },
                onDone = handleDismiss,
            )
        }
        return
    }

    Column {
        WizardTopBar(
            stepIndex = state.step.ordinal,
            showBack = !state.isLoading,
            onBack = goBack,
            onClose = requestClose,
        )

        AnimatedContent(
            targetState = state.step to state.isLoading,
            modifier = Modifier.weight(1f, fill = false),
            transitionSpec = {
                val forward = targetState.first.ordinal >= initialState.first.ordinal
                ContentTransform(
                    targetContentEnter = slideInHorizontally(tween(motion.durationMedium)) {
                        if (forward) it / 4 else -it / 4
                    } + fadeIn(tween(motion.durationMedium)),
                    initialContentExit = slideOutHorizontally(tween(motion.durationMedium)) {
                        if (forward) -it / 4 else it / 4
                    } + fadeOut(tween(motion.durationShort)),
                    sizeTransform = SizeTransform(clip = false),
                )
            },
            label = "ai_wizard_step",
        ) { (step, isLoading) ->
            when (step) {
                AiWordImportStep.TARGET_LANG -> AiTargetLanguageStep(
                    languages = state.availableLanguages,
                    selected = state.selectedTargetLanguage,
                    onSelected = viewModel::selectTargetLanguage,
                )

                AiWordImportStep.NATIVE_LANG -> AiNativeLanguageStep(
                    languages = state.availableLanguages.filter { it != state.selectedTargetLanguage },
                    selected = state.selectedNativeLanguage,
                    onSelected = viewModel::selectNativeLanguage,
                )

                AiWordImportStep.LEVEL -> AiLevelStep(
                    selectedLevel = state.selectedLevel,
                    error = state.error,
                    onLevelSelected = viewModel::selectLevel,
                    onContinue = viewModel::nextStep,
                )

                AiWordImportStep.TOPICS -> if (isLoading) {
                    AiGeneratingContent(state = state)
                } else {
                    AiTopicsStep(
                        topics = state.availableTopics,
                        selectedTopics = state.selectedTopics,
                        error = state.error,
                        onToggleTopic = viewModel::toggleTopic,
                        onGenerate = viewModel::submit,
                    )
                }

                AiWordImportStep.PREVIEW -> AiWordPreviewStep(
                    state = state,
                    onToggleWord = viewModel::toggleWordSelection,
                    onTagSelected = viewModel::selectTag,
                    onImport = {
                        pendingPreviewWords = state.selectedWordIndices.sorted()
                            .mapNotNull { state.suggestedWords.getOrNull(it)?.originalWord }
                        viewModel.importSelected()
                    },
                )
            }
        }
    }
}

@Composable
private fun WizardTopBar(
    stepIndex: Int?,
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.content_description_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(Modifier.size(Theme.dimensions.touchTarget))
        }

        if (stepIndex != null) {
            StepProgressBar(current = stepIndex + 1, total = AiWizardTotalSteps, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }

        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(Res.string.content_description_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
