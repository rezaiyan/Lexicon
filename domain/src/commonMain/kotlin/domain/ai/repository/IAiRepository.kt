package domain.ai.repository

import core.common.Try
import domain.ai.model.AiGuidance
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft

interface IAiRepository {
    /**
     * Vocabulary read from an upload-ready image: terms in [languages] learning, translations in native.
     * [guidance] narrows what is read; guided calls cost more and may be refused.
     */
    suspend fun extractWords(
        image: ByteArray,
        languages: LanguagePair,
        guidance: AiGuidance = AiGuidance(),
    ): Try<List<WordDraft>>

    /** AI-generated words for a level and topics, skipping words the user already has. */
    suspend fun suggestWords(
        languages: LanguagePair,
        level: ProficiencyLevel,
        topics: List<String>,
        guidance: AiGuidance = AiGuidance(),
    ): Try<List<WordDraft>>
}

