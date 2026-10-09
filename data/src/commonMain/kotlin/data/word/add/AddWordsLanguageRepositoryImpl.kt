package data.word.add

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import core.common.Try
import data.core.database.LexiconQueries
import domain.word.add.model.LanguagePair
import domain.word.add.repository.IAddWordsLanguageRepository
import utils.Language

class AddWordsLanguageRepositoryImpl(
    private val queries: LexiconQueries,
) : IAddWordsLanguageRepository {

    override suspend fun lastUsed(): Try<LanguagePair?> = Try {
        queries.getAddWordsPreference().awaitAsOneOrNull()?.let { row ->
            pairOf(row.learningLanguage, row.nativeLanguage)
        }
    }

    override suspend fun saveLastUsed(languages: LanguagePair): Try<Unit> = Try {
        queries.saveAddWordsPreference(languages.learning.code, languages.native.code)
    }

    override suspend fun mostCommon(): Try<LanguagePair?> = Try {
        queries.getMostCommonLanguagePair().awaitAsOneOrNull()?.let { row ->
            pairOf(row.targetLanguage, row.sourceLanguage)
        }
    }

    /** Unknown codes map to null rather than silently becoming English. */
    private fun pairOf(learningCode: String, nativeCode: String): LanguagePair? {
        val learning = Language.entries.find { it.code == learningCode } ?: return null
        val native = Language.entries.find { it.code == nativeCode } ?: return null
        return LanguagePair.orNull(learning, native)
    }
}
