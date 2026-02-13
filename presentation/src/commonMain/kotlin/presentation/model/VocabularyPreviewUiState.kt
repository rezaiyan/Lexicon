package presentation.model

import domain.onboarding.model.SuggestedVocabulary

data class VocabularyPreviewUiState(
    val words: List<SuggestedVocabulary> = emptyList(),
    val selectedIndices: Set<Int> = emptySet()
) {
    val selectedCount: Int get() = selectedIndices.size
}
