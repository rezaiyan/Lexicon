@file:OptIn(ExperimentalTime::class)

package data.word.sync

import data.core.database.WordEntity
import data.word.remote.model.RemoteWord
import kotlin.time.ExperimentalTime

interface IWordConflictResolver {
    fun resolveConflicts(
        localWords: List<WordEntity>,
        remoteWords: List<RemoteWord>
    ): List<WordEntity>
}

class WordConflictResolver : IWordConflictResolver {

    override fun resolveConflicts(
        localWords: List<WordEntity>,
        remoteWords: List<RemoteWord>
    ): List<WordEntity> {
        val localWordMapById = localWords.associateBy { it.id.toLong() }
        val localWordMapByContent = localWords.associateBy { entity ->
            WordContentKey(
                originalWord = entity.originalWord.trim().lowercase(),
                translation = entity.translation.trim().lowercase()
            )
        }

        val validRemoteWords = remoteWords.filter { it.id != null && it.id > 0 }
        val entitiesByContent = mutableMapOf<WordContentKey, WordEntity>()

        for (remote in validRemoteWords) {
            val contentKey = WordContentKey(
                originalWord = remote.originalWord.trim().lowercase(),
                translation = remote.translation.trim().lowercase()
            )

            val existingByContent = localWordMapByContent[contentKey]
            val existingById = remote.id?.let { localWordMapById[it] }
            val existingEntity = existingByContent ?: existingById

            val entityId = existingEntity?.id ?: (remote.id?.toInt() ?: 0)

            val entity = WordEntity(
                id = entityId,
                originalWord = remote.originalWord,
                translation = remote.translation,
                description = remote.description,
                sourceLanguage = remote.sourceLanguage,
                targetLanguage = remote.targetLanguage,
                level = remote.level,
                easeFactor = remote.easeFactor,
                interval = remote.interval,
                repetitions = remote.repetitions,
                lastReviewDate = remote.lastReviewDate,
                nextReviewDate = remote.nextReviewDate,
                dateAdded = remote.createdAt
                    ?: existingEntity?.dateAdded
                    ?: 0L
            )

            entitiesByContent[contentKey] = entity
        }

        return entitiesByContent.values.toList()
    }

    private data class WordContentKey(val originalWord: String, val translation: String)
}

