package feature.addwords.source

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.common.fold
import core.common.onSuccess
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.GetSuggestionLevelUseCase
import domain.word.add.usecase.SuggestWordsUseCase
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.GuidanceDraft
import feature.addwords.model.SourceEffect
import feature.addwords.model.logImportEvent
import feature.addwords.model.logImportFailed
import feature.addwords.model.toProblem
import kotlinx.coroutines.launch

data class AiSuggestState(
    /** Preselected with the level last used (see [GetSuggestionLevelUseCase]); null only while loading. */
    val level: ProficiencyLevel? = null,
    val topics: Set<String> = emptySet(),
    val availableTopics: List<String> = DefaultTopics,
    val isGenerating: Boolean = false,
    val problem: AddWordsProblem? = null,
    /** What the learner describes in their own words, plus quick picks. */
    override val guidance: GuidanceDraft = GuidanceDraft(),
) : GuidedSourceState<AiSuggestState> {
    val canGenerate: Boolean get() = level != null && !isGenerating

    override val isBusy: Boolean get() = isGenerating

    override fun withGuidance(guidance: GuidanceDraft) = copy(guidance = guidance)

    companion object {
        val DefaultTopics = listOf(
            "Daily Life", "Travel", "Business", "Food", "Technology",
            "Sports", "Health", "Arts", "Nature", "Academic",
        )
    }
}

/**
 * Asks the AI for words at a level, on topics and/or described in the learner's own words, in
 * the sheet's language pair. Nothing is saved here.
 */
class AiSuggestViewModel(
    private val suggestWords: SuggestWordsUseCase,
    getSuggestionLevel: GetSuggestionLevelUseCase,
    private val analytics: IAnalyticsTracker,
) : GuidedSourceViewModel<AiSuggestState>(WordOrigin.AiSuggestion, analytics) {

    override fun initialState() = AiSuggestState()

    init {
        viewModelScope.launch {
            // A level the learner already picked while this loaded wins
            getSuggestionLevel(Unit).onSuccess { level -> updateState { copy(level = this.level ?: level) } }
        }
    }

    fun selectLevel(level: ProficiencyLevel) = updateState { copy(level = level, problem = null) }

    fun toggleTopic(topic: String) {
        val adding = topic !in currentState.topics
        updateState { copy(topics = if (adding) topics + topic else topics - topic) }
        // Topics are a fixed list, never free text, so the topic itself is safe to log.
        if (adding) analytics.logImportEvent("import_topic_entered", WordOrigin.AiSuggestion, "topic" to topic)
    }

    fun generate(languages: LanguagePair?) {
        val state = currentState
        val level = state.level ?: return
        if (!state.canGenerate || languages == null) return

        logGuidanceSent()
        updateState { copy(isGenerating = true, problem = null) }
        val params = SuggestWordsUseCase.Params(languages, level, state.topics.toList(), state.guidance.toGuidance())
        viewModelScope.launch {
            suggestWords(params).fold(
                onSuccess = { drafts ->
                    updateState { copy(isGenerating = false) }
                    emitEffect(SourceEffect.CandidatesReady(drafts))
                },
                onFailure = { error ->
                    updateState { copy(isGenerating = false) }
                    if (showInstructionProblem(error)) return@fold
                    val problem = error.toProblem(WordOrigin.AiSuggestion)
                    analytics.logImportFailed(WordOrigin.AiSuggestion, step = "extract", problem)
                    // Out of credits gets its own page (with the way to get more), not an inline error
                    if (problem == AddWordsProblem.OutOfCredits) {
                        emitEffect(SourceEffect.OutOfCredits)
                    } else {
                        updateState { copy(problem = problem) }
                    }
                },
            )
        }
    }

    fun dismissProblem() = updateState { copy(problem = null) }
}
