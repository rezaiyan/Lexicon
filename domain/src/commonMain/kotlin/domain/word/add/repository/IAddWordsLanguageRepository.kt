package domain.word.add.repository

import core.common.Try
import domain.word.add.model.LanguagePair

/** Local-only memory of which languages the user adds words in. */
interface IAddWordsLanguageRepository {
    /** Pair the user last confirmed when adding words, or null. */
    suspend fun lastUsed(): Try<LanguagePair?>
    suspend fun saveLastUsed(languages: LanguagePair): Try<Unit>

    /** Pair most words in the collection use, or null when the collection is empty. */
    suspend fun mostCommon(): Try<LanguagePair?>
}
