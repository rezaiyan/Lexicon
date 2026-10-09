package domain.word.add.model

import utils.Language

/**
 * The two languages of a word, named the way the learner thinks about them.
 *
 * Maps to [domain.word.model.Word] as `targetLanguage = learning` and `sourceLanguage = native`.
 */
data class LanguagePair(val learning: Language, val native: Language) {
    init {
        require(learning != native) { "Learning and native language must differ" }
    }

    companion object {
        fun orNull(learning: Language, native: Language): LanguagePair? =
            if (learning != native) LanguagePair(learning, native) else null
    }
}
