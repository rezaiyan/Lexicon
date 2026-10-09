package domain.word.add.model

/** Where a batch of words came from; used for analytics and to tune messages. */
enum class WordOrigin(val analyticsName: String) {
    Manual("manual"),
    File("file"),
    Photo("image"),
    AiSuggestion("ai"),
    Onboarding("onboarding"),
}

data class AddWordsCommand(
    val drafts: List<WordDraft>,
    val languages: LanguagePair,
    val tagIds: Set<Long>,
    val origin: WordOrigin,
)

/** Result of adding a batch. Duplicates are skipped, never treated as an error. */
data class AddWordsOutcome(
    val added: Int,
    val duplicates: Int,
    /** Terms actually stored, in input order (duplicates excluded). */
    val addedTerms: List<String> = emptyList(),
)
