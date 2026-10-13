package presentation.ui.components.imports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import domain.onboarding.model.ProficiencyLevel
import feature.addwords.model.GuidanceDraft
import feature.addwords.source.AiSuggestViewModel
import feature.addwords.source.GuidedSourceViewModel
import feature.addwords.source.PhotoImportViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_guidance_placeholder_photo
import lexicon.resources.generated.resources.ai_guidance_placeholder_suggest
import lexicon.resources.generated.resources.ai_guidance_subtitle_photo
import lexicon.resources.generated.resources.ai_guidance_title_photo
import org.jetbrains.compose.resources.stringResource

/** Guidance under a photo: collapsed by default, quick picks first since most photos need no words. */
@Composable
internal fun PhotoGuidancePanel(
    photo: PhotoImportViewModel,
    guidance: GuidanceDraft,
    surcharge: Int,
    enabled: Boolean,
    onContinueWithout: () -> Unit,
) {
    val actions = rememberGuidanceActions(photo)
    AiGuidanceComposer(
        guidance = guidance,
        placeholder = stringResource(Res.string.ai_guidance_placeholder_photo),
        starters = photoStarters(),
        quickPicks = photoQuickPicks(guidance.focus, actions),
        surcharge = surcharge,
        enabled = enabled,
        actions = actions,
        onContinueWithout = onContinueWithout,
        header = GuidanceHeader(
            title = stringResource(Res.string.ai_guidance_title_photo),
            subtitle = stringResource(Res.string.ai_guidance_subtitle_photo),
        ),
        ownWordsFirst = false,
    )
}

/**
 * Guidance on the topics step: open, leading with the learner's own description, then [topics] as
 * the other way to say what to learn. The [level] sits with the other options (preselected with the
 * last one used), not on a page of its own.
 */
@Composable
internal fun SuggestGuidancePanel(
    ai: AiSuggestViewModel,
    guidance: GuidanceDraft,
    level: ProficiencyLevel?,
    surcharge: Int,
    enabled: Boolean,
    onContinueWithout: () -> Unit,
    topics: @Composable () -> Unit,
) {
    val actions = rememberGuidanceActions(ai)
    AiGuidanceComposer(
        guidance = guidance,
        placeholder = stringResource(Res.string.ai_guidance_placeholder_suggest),
        starters = suggestStarters(),
        quickPicks = suggestQuickPicks(guidance.focus, actions, level, ai::selectLevel),
        surcharge = surcharge,
        enabled = enabled,
        actions = actions,
        onContinueWithout = onContinueWithout,
        afterOwnWords = topics,
    )
}

/** The guidance callbacks of [viewModel], built once so chips and the composer don't recompose for them. */
@Composable
private fun rememberGuidanceActions(viewModel: GuidedSourceViewModel<*>): GuidanceActions = remember(viewModel) {
    GuidanceActions(
        onTextChange = viewModel::onInstructionChanged,
        onStarter = viewModel::applyStarter,
        onClear = viewModel::clearInstruction,
        onToggleWordType = viewModel::toggleWordType,
        onToggleRegister = viewModel::toggleRegister,
        onToggleGrammarNotes = viewModel::toggleGrammarNotes,
        onToggleMarkedOnly = viewModel::toggleMarkedOnly,
        onToggleSkipNames = viewModel::toggleSkipNames,
    )
}
