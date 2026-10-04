package domain.onboarding.model

import utils.Language

/**
 * A generated word the user can add to their list.
 *
 * Follows the [domain.word.model.Word] convention: [originalWord] is in [targetLanguage] (the language
 * being learned) and [translation] is in [sourceLanguage] (the user's native language).
 */
data class SuggestedVocabulary(
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
)
