package feature.onboarding.model

import domain.onboarding.model.SuggestedVocabulary

sealed interface VocabularyPreviewEffect {
    data class ProceedWithSelection(val words: List<SuggestedVocabulary>) : VocabularyPreviewEffect
    data object SkipVocabulary : VocabularyPreviewEffect
}
