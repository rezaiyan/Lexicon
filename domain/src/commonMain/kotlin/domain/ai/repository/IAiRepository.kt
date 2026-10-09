package domain.ai.repository

import core.common.Try
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft

interface IAiRepository {
    /** Vocabulary read from an upload-ready image: terms in [languages] learning, translations in native. */
    suspend fun extractWords(image: ByteArray, languages: LanguagePair): Try<List<WordDraft>>

    /** AI-generated words for a level and topics, skipping words the user already has. */
    suspend fun suggestWords(
        languages: LanguagePair,
        level: ProficiencyLevel,
        topics: List<String>,
    ): Try<List<WordDraft>>
}

