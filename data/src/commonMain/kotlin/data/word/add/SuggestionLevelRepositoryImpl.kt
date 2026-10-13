package data.word.add

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import core.common.Try
import data.core.database.LexiconQueries
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.repository.ISuggestionLevelRepository

class SuggestionLevelRepositoryImpl(
    private val queries: LexiconQueries,
) : ISuggestionLevelRepository {

    /** An unknown stored name (a renamed level) reads as "never chosen", not as an error. */
    override suspend fun lastUsed(): Try<ProficiencyLevel?> = Try {
        queries.getSuggestionLevel().awaitAsOneOrNull()?.let { name ->
            ProficiencyLevel.entries.find { it.name == name }
        }
    }

    override suspend fun saveLastUsed(level: ProficiencyLevel): Try<Unit> = Try {
        queries.saveSuggestionLevel(level.name)
    }
}
