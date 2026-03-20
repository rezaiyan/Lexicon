package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import events.OnEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.aiimport.AiWordImportViewModel
import feature.aiimport.model.AiWordImportEffect
import feature.aiimport.model.AiWordImportStep
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_import_success
import lexicon.resources.generated.resources.ai_wizard_ai_suggestions
import lexicon.resources.generated.resources.ai_wizard_native_highlight
import lexicon.resources.generated.resources.ai_wizard_native_subtitle
import lexicon.resources.generated.resources.ai_wizard_native_title
import lexicon.resources.generated.resources.ai_wizard_target_highlight
import lexicon.resources.generated.resources.ai_wizard_target_subtitle
import lexicon.resources.generated.resources.ai_wizard_target_title
import lexicon.resources.generated.resources.content_description_back
import lexicon.resources.generated.resources.content_description_close
import overlay.LocalOverlayHost
import overlay.bottomsheet.showSizeToFitBottomSheet
import theme.Theme

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
    val motion = Theme.motion
    val overlayHost = LocalOverlayHost.current

    val handleDismiss = {
        viewModel.reset()
        onDismiss()
    }

    val importSuccessFormat = stringResource(Res.string.ai_import_success)

    OnEvents(viewModel.effects) { event ->
        when (event) {
            is AiWordImportEffect.ImportSuccess -> {
                onShowSnackBar(importSuccessFormat.replace("%1\$d", event.count.toString()))
                handleDismiss()
            }

            is AiWordImportEffect.Dismiss -> handleDismiss()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
            val stepIndex = state.step.ordinal
            val isPreview = state.step == AiWordImportStep.PREVIEW

            WizardNavigationBar(
                stepIndex = stepIndex,
                isPreview = isPreview,
                isFirstStep = state.step == AiWordImportStep.TARGET_LANG,
                isLoading = state.isLoading,
                onBack = { viewModel.previousStep() },
                onClose = {
                    if (isPreview || state.isLoading) {
                        overlayHost.showSizeToFitBottomSheet(tag = "discard-confirm") { nav ->
                            DiscardConfirmationContent(
                                onDiscard = {
                                    nav.dismiss()
                                    handleDismiss()
                                },
                                onKeep = { nav.dismiss() }
                            )
                        }
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
                            animationSpec = tween(motion.durationMedium),
                            initialOffsetX = { if (forward) it else -it }
                        ) + fadeIn(animationSpec = tween(motion.durationMedium)),
                        initialContentExit = slideOutHorizontally(
                            animationSpec = tween(motion.durationMedium),
                            targetOffsetX = { if (forward) -it else it }
                        ) + fadeOut(animationSpec = tween(motion.durationMedium))
                    )
                },
                label = "ai_wizard_step"
            ) { step ->
                when (step) {
                    AiWordImportStep.TARGET_LANG -> AiLanguageStep(
                        title = stringResource(Res.string.ai_wizard_target_title),
                        highlight = stringResource(Res.string.ai_wizard_target_highlight),
                        subtitle = stringResource(Res.string.ai_wizard_target_subtitle),
                        languages = state.availableLanguages,
                        selectedLanguage = state.selectedTargetLanguage,
                        onLanguageSelected = viewModel::selectTargetLanguage,
                        spacing = spacing,
                    )

                    AiWordImportStep.NATIVE_LANG -> AiLanguageStep(
                        title = stringResource(Res.string.ai_wizard_native_title),
                        highlight = stringResource(Res.string.ai_wizard_native_highlight),
                        subtitle = stringResource(Res.string.ai_wizard_native_subtitle),
                        languages = state.availableLanguages.filter { it != state.selectedTargetLanguage },
                        selectedLanguage = state.selectedNativeLanguage,
                        onLanguageSelected = viewModel::selectNativeLanguage,
                        spacing = spacing,
                    )

                    AiWordImportStep.LEVEL -> AiLevelStep(
                        selectedLevel = state.selectedLevel,
                        error = state.error,
                        onLevelSelected = viewModel::selectLevel,
                        onContinue = viewModel::nextStep,
                        spacing = spacing,
                        dimensions = dimensions
                    )

                    AiWordImportStep.TOPICS -> AiTopicsStep(
                        state = state,
                        onToggleTopic = viewModel::toggleTopic,
                        onGenerate = viewModel::submit,
                        spacing = spacing,
                        dimensions = dimensions
                    )

                    AiWordImportStep.PREVIEW -> AiWordPreviewStep(
                        state = state,
                        onToggleWord = viewModel::toggleWordSelection,
                        onImport = viewModel::importSelected,
                        spacing = spacing,
                        dimensions = dimensions
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
            .padding(horizontal = spacing.xs, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isFirstStep && !isPreview && !isLoading) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.content_description_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Spacer(modifier = Modifier.size(Theme.dimensions.touchTarget))
        }

        if (isLoading) {
            Spacer(modifier = Modifier.weight(1f))
        } else if (!isPreview) {
            StepProgressSegments(
                stepIndex = stepIndex,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.xs),
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
                Spacer(modifier = Modifier.size(spacing.xxs))
                Text(
                    text = stringResource(Res.string.ai_wizard_ai_suggestions),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.content_description_close),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StepProgressSegments(
    stepIndex: Int,
    modifier: Modifier = Modifier,
) {
    val motion = Theme.motion

    Row(
        modifier = modifier.semantics {
            contentDescription = "Step ${stepIndex + 1} of $AiWizardTotalSteps"
        },
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
    ) {
        repeat(AiWizardTotalSteps) { index ->
            val filled = index <= stepIndex
            val segmentColor by animateColorAsState(
                targetValue = if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(motion.durationMedium),
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
}
