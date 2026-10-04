package feature.onboarding

import core.base.BaseViewModel
import domain.onboarding.model.SuggestedVocabulary
import feature.onboarding.model.VocabularyPreviewEffect
import feature.onboarding.model.VocabularyPreviewUiState

/** Starter words built from the onboarding answers: add them all, or start with an empty list. */
class VocabularyPreviewViewModel(
    words: List<SuggestedVocabulary>,
) : BaseViewModel<VocabularyPreviewUiState, VocabularyPreviewEffect>() {

    override fun initialState() = VocabularyPreviewUiState()

    init {
        val first = words.firstOrNull()
        updateState {
            copy(
                words = words,
                learningLanguage = first?.targetLanguage,
                nativeLanguage = first?.sourceLanguage,
            )
        }
    }

    fun addWords() {
        emitEffect(VocabularyPreviewEffect.AddWords(currentState.words))
    }

    fun startEmpty() {
        emitEffect(VocabularyPreviewEffect.StartEmpty)
    }
}
