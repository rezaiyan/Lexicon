package feature.onboarding.model

import domain.onboarding.model.SuggestedVocabulary

sealed interface VocabularyPreviewEffect {
    data class AddWords(val words: List<SuggestedVocabulary>) : VocabularyPreviewEffect
    data object StartEmpty : VocabularyPreviewEffect
}
