package feature.onboarding

import core.base.BaseViewModel
import domain.onboarding.model.SuggestedVocabulary
import feature.onboarding.model.VocabularyPreviewEffect
import feature.onboarding.model.VocabularyPreviewUiState
import utils.Language

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
                learningLanguage = first?.targetLanguage?.let(::languageOrNull),
                nativeLanguage = first?.sourceLanguage?.let(::languageOrNull),
            )
        }
    }

    fun addWords() {
        emitEffect(VocabularyPreviewEffect.AddWords(currentState.words))
    }

    fun startEmpty() {
        emitEffect(VocabularyPreviewEffect.StartEmpty)
    }

    /** The backend sends either a code ("de") or a display name ("German"). */
    private fun languageOrNull(value: String): Language? =
        Language.entries.firstOrNull {
            it.code.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true)
        }
}
