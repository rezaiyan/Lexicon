package feature.onboarding.model

import domain.onboarding.model.SuggestedVocabulary
import utils.Language

data class VocabularyPreviewUiState(
    val words: List<SuggestedVocabulary> = emptyList(),
    /** Language the words are in. */
    val learningLanguage: Language? = null,
    /** Language the translations are in. */
    val nativeLanguage: Language? = null,
)
