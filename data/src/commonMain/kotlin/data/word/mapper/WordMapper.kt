package data.word.mapper

import data.core.database.WordEntity
import data.core.database.WordEntityData
import domain.word.model.Word
import utils.Language
fun WordEntity.toDomain(fallbackLanguage: Language = Language.ENGLISH, tagIds: List<Long> = emptyList()): Word {
    return Word(
        id = id.toInt(),
        originalWord = originalWord,
        translation = translation,
        description = description,
        sourceLanguage = resolveLanguage(sourceLanguage, fallbackLanguage),
        targetLanguage = resolveLanguage(targetLanguage, fallbackLanguage),
        level = level.toInt(),
        easeFactor = easeFactor.toFloat(),
        interval = interval.toInt(),
        repetitions = repetitions.toInt(),
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate,
        dateAdded = dateAdded,
        tagIds = tagIds
    )
}

fun WordEntityData.toDomain(fallbackLanguage: Language = Language.ENGLISH): Word {
    return Word(
        id = id,
        originalWord = originalWord,
        translation = translation,
        description = description,
        sourceLanguage = resolveLanguage(sourceLanguage, fallbackLanguage),
        targetLanguage = resolveLanguage(targetLanguage, fallbackLanguage),
        level = level,
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate,
        dateAdded = dateAdded,
        tagIds = tagIds
    )
}

fun Word.toEntityData(): WordEntityData {
    return WordEntityData(
        id = id,
        originalWord = originalWord,
        translation = translation,
        description = description,
        sourceLanguage = sourceLanguage.code,
        targetLanguage = targetLanguage.code,
        level = level,
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate,
        dateAdded = dateAdded
    )
}

fun List<WordEntity>.toDomainList(fallbackLanguage: Language = Language.ENGLISH): List<Word> {
    return map { it.toDomain(fallbackLanguage) }
}

fun List<Word>.toEntityDataList(): List<WordEntityData> {
    return map { it.toEntityData() }
}

private fun resolveLanguage(code: String, fallback: Language): Language {
    if (code.isBlank()) return fallback
    return Language.fromCode(Language.toCode(code))
}
