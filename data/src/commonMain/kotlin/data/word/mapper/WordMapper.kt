@file:OptIn(ExperimentalTime::class)

package data.word.mapper

import data.core.database.WordEntity
import domain.word.model.Word
import kotlin.time.ExperimentalTime

/**
 * Mapper between data layer (WordEntity) and domain layer (Word)
 */
object WordMapper {

    fun toDomain(entity: WordEntity): Word {
        return Word(
            id = entity.id,
            originalWord = entity.originalWord,
            translation = entity.translation,
            description = entity.description,
            sourceLanguage = entity.sourceLanguage,
            targetLanguage = entity.targetLanguage,
            level = entity.level,
            easeFactor = entity.easeFactor,
            interval = entity.interval,
            repetitions = entity.repetitions,
            lastReviewDate = entity.lastReviewDate,
            nextReviewDate = entity.nextReviewDate,
            dateAdded = entity.dateAdded
        )
    }

    fun toEntity(domain: Word): WordEntity {
        return WordEntity(
            id = domain.id,
            originalWord = domain.originalWord,
            translation = domain.translation,
            description = domain.description,
            sourceLanguage = domain.sourceLanguage,
            targetLanguage = domain.targetLanguage,
            level = domain.level,
            easeFactor = domain.easeFactor,
            interval = domain.interval,
            repetitions = domain.repetitions,
            lastReviewDate = domain.lastReviewDate,
            nextReviewDate = domain.nextReviewDate,
            dateAdded = domain.dateAdded
        )
    }

    fun toDomainList(entities: List<WordEntity>): List<Word> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Word>): List<WordEntity> {
        return domains.map { toEntity(it) }
    }
}

