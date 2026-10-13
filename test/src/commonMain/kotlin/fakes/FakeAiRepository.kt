package fakes

import core.common.Try
import domain.ai.model.AiGuidance
import domain.ai.repository.IAiRepository
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft

class FakeAiRepository : IAiRepository {
    var drafts: Try<List<WordDraft>> = Try.success(emptyList())
    var lastImage: ByteArray? = null
    var lastLanguages: LanguagePair? = null
    var lastExtractGuidance: AiGuidance? = null

    override suspend fun extractWords(
        image: ByteArray,
        languages: LanguagePair,
        guidance: AiGuidance,
    ): Try<List<WordDraft>> {
        lastImage = image
        lastLanguages = languages
        lastExtractGuidance = guidance
        return drafts
    }

    var suggestions: Try<List<WordDraft>> = Try.success(emptyList())
    var lastSuggestionRequest: Triple<LanguagePair, ProficiencyLevel, List<String>>? = null
    var lastSuggestionGuidance: AiGuidance? = null

    override suspend fun suggestWords(
        languages: LanguagePair,
        level: ProficiencyLevel,
        topics: List<String>,
        guidance: AiGuidance,
    ): Try<List<WordDraft>> {
        lastSuggestionRequest = Triple(languages, level, topics)
        lastSuggestionGuidance = guidance
        return suggestions
    }
}
