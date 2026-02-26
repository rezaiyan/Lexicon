@file:OptIn(ExperimentalTime::class)

package data.word.mapper

import data.core.database.WordEntity
import data.core.database.WordEntityData
import domain.word.model.Word
import utils.Language
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Mapper between data layer (SQLDelight WordEntity) and domain layer (Word)
 */
object WordMapper {

    fun toDomain(entity: WordEntity, fallbackLanguage: Language = Language.ENGLISH): Word {
        return Word(
            id = entity.id.toInt(),
            originalWord = entity.originalWord,
            translation = entity.translation,
            description = entity.description,
            sourceLanguage = resolveLanguage(entity.sourceLanguage, fallbackLanguage),
            targetLanguage = resolveLanguage(entity.targetLanguage, fallbackLanguage),
            level = entity.level.toInt(),
            easeFactor = entity.easeFactor.toFloat(),
            interval = entity.interval.toInt(),
            repetitions = entity.repetitions.toInt(),
            lastReviewDate = entity.lastReviewDate,
            nextReviewDate = entity.nextReviewDate,
            dateAdded = entity.dateAdded
        )
    }

    fun toEntityData(domain: Word): WordEntityData {
        return WordEntityData(
            id = domain.id,
            originalWord = domain.originalWord,
            translation = domain.translation,
            description = domain.description,
            sourceLanguage = domain.sourceLanguage.code,
            targetLanguage = domain.targetLanguage.code,
            level = domain.level,
            easeFactor = domain.easeFactor,
            interval = domain.interval,
            repetitions = domain.repetitions,
            lastReviewDate = domain.lastReviewDate,
            nextReviewDate = domain.nextReviewDate,
            dateAdded = domain.dateAdded
        )
    }

    fun toDomain(entity: WordEntityData, fallbackLanguage: Language = Language.ENGLISH): Word {
        return Word(
            id = entity.id,
            originalWord = entity.originalWord,
            translation = entity.translation,
            description = entity.description,
            sourceLanguage = resolveLanguage(entity.sourceLanguage, fallbackLanguage),
            targetLanguage = resolveLanguage(entity.targetLanguage, fallbackLanguage),
            level = entity.level,
            easeFactor = entity.easeFactor,
            interval = entity.interval,
            repetitions = entity.repetitions,
            lastReviewDate = entity.lastReviewDate,
            nextReviewDate = entity.nextReviewDate,
            dateAdded = entity.dateAdded
        )
    }

    fun toDomainList(entities: List<WordEntity>, fallbackLanguage: Language = Language.ENGLISH): List<Word> {
        return entities.map { toDomain(it, fallbackLanguage) }
    }

    fun toEntityDataList(domains: List<Word>): List<WordEntityData> {
        return domains.map { toEntityData(it) }
    }

    private fun resolveLanguage(code: String, fallback: Language): Language {
        if (code.isBlank()) return fallback
        return Language.fromCode(Language.toCode(code))
    }
}
