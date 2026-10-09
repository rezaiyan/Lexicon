package fakes

import core.common.Try
import domain.ai.repository.IAiRepository
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft

class FakeAiRepository : IAiRepository {
    var drafts: Try<List<WordDraft>> = Try.success(emptyList())
    var lastImage: ByteArray? = null
    var lastLanguages: LanguagePair? = null

    override suspend fun extractWords(image: ByteArray, languages: LanguagePair): Try<List<WordDraft>> {
        lastImage = image
        lastLanguages = languages
        return drafts
    }

    var suggestions: Try<List<WordDraft>> = Try.success(emptyList())
    var lastSuggestionRequest: Triple<LanguagePair, ProficiencyLevel, List<String>>? = null

    override suspend fun suggestWords(
        languages: LanguagePair,
        level: ProficiencyLevel,
        topics: List<String>,
    ): Try<List<WordDraft>> {
        lastSuggestionRequest = Triple(languages, level, topics)
        return suggestions
    }

}
