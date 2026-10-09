package fakes

import core.common.Try
import domain.word.add.model.LanguagePair
import domain.word.add.repository.IAddWordsLanguageRepository

class FakeAddWordsLanguageRepository(
    var lastUsed: LanguagePair? = null,
    var mostCommon: LanguagePair? = null,
) : IAddWordsLanguageRepository {
    override suspend fun lastUsed(): Try<LanguagePair?> = Try.success(lastUsed)

    override suspend fun saveLastUsed(languages: LanguagePair): Try<Unit> {
        lastUsed = languages
        return Try.success(Unit)
    }

    override suspend fun mostCommon(): Try<LanguagePair?> = Try.success(mostCommon)
}
