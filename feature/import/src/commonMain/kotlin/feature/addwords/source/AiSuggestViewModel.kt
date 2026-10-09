package feature.addwords.source

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import core.error.DomainError
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.SuggestWordsUseCase
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.SourceEffect
import feature.addwords.model.toProblem
import kotlinx.coroutines.launch

data class AiSuggestState(
    val level: ProficiencyLevel? = null,
    val topics: Set<String> = emptySet(),
    val availableTopics: List<String> = DefaultTopics,
    val isGenerating: Boolean = false,
    val problem: AddWordsProblem? = null,
) {
    val canGenerate: Boolean get() = level != null && !isGenerating

    companion object {
        val DefaultTopics = listOf(
            "Daily Life", "Travel", "Business", "Food", "Technology",
            "Sports", "Health", "Arts", "Nature", "Academic",
        )
    }
}

/** Asks the AI for words at a level and on topics, in the sheet's language pair. Nothing is saved here. */
class AiSuggestViewModel(
    private val suggestWords: SuggestWordsUseCase,
    private val analytics: IAnalyticsTracker,
) : BaseViewModel<AiSuggestState, SourceEffect>() {

    override fun initialState() = AiSuggestState()

    fun selectLevel(level: ProficiencyLevel) = updateState { copy(level = level, problem = null) }

    fun toggleTopic(topic: String) {
        val adding = topic !in currentState.topics
        updateState { copy(topics = if (adding) topics + topic else topics - topic) }
        if (adding) analytics.logEvent("import_topic_entered", mapOf("topic" to topic))
    }

    fun generate(languages: LanguagePair?) {
        val state = currentState
        val level = state.level ?: return
        if (!state.canGenerate || languages == null) return

        updateState { copy(isGenerating = true, problem = null) }
        viewModelScope.launch {
            suggestWords(SuggestWordsUseCase.Params(languages, level, state.topics.toList())).fold(
                onSuccess = { drafts ->
                    updateState { copy(isGenerating = false) }
                    emitEffect(SourceEffect.CandidatesReady(drafts))
                    analytics.logEvent("import_preview_shown", mapOf("word_count" to drafts.size.toString()))
                },
                onFailure = { error ->
                    updateState { copy(isGenerating = false, problem = error.toProblem(WordOrigin.AiSuggestion)) }
                    analytics.logEvent("import_failed", mapOf("reason" to (error::class.simpleName ?: "unknown")))
                    if (error is DomainError.Commerce.PremiumRequired) emitEffect(SourceEffect.PremiumLapsed)
                },
            )
        }
    }

    fun dismissProblem() = updateState { copy(problem = null) }
}
