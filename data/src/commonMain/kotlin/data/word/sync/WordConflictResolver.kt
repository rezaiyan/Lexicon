package data.word.sync

import data.core.database.WordEntity
import data.core.database.WordEntityData
import data.word.remote.model.RemoteWord

/**
 * Remote words to store, all under their server ids, and the local words that must first move to those
 * ids (local id → server id): words added on this device before uploads re-keyed them.
 */
data class ResolvedWords(
    val entities: List<WordEntityData>,
    val localIdMoves: Map<Int, Int> = emptyMap(),
)

interface IWordConflictResolver {
    fun resolveConflicts(
        localWords: List<WordEntity>,
        remoteWords: List<RemoteWord>
    ): ResolvedWords
}

class WordConflictResolver : IWordConflictResolver {

    override fun resolveConflicts(
        localWords: List<WordEntity>,
        remoteWords: List<RemoteWord>
    ): ResolvedWords {
        val localWordMapById = localWords.associateBy { it.id }
        val localWordMapByContent = localWords.associateBy { entity ->
            WordContentKey(
                originalWord = entity.originalWord.trim().lowercase(),
                translation = entity.translation.trim().lowercase(),
                learningLanguage = entity.targetLanguage,
            )
        }

        val validRemoteWords = remoteWords.filter { it.id != null && it.id > 0 }
        val entitiesByContent = mutableMapOf<WordContentKey, WordEntityData>()
        val localIdMoves = mutableMapOf<Int, Int>()

        for (remote in validRemoteWords) {
            val contentKey = WordContentKey(
                originalWord = remote.originalWord.trim().lowercase(),
                translation = remote.translation.trim().lowercase(),
                learningLanguage = remote.targetLanguage,
            )

            val existingByContent = localWordMapByContent[contentKey]
            val existingById = remote.id?.let { localWordMapById[it] }
            val existingEntity = existingByContent ?: existingById

            // validRemoteWords only holds words with a server id.
            val entityId = remote.id?.toInt() ?: 0
            existingByContent?.id?.toInt()?.takeIf { it != entityId }?.let { localIdMoves[it] = entityId }

            val entity = WordEntityData(
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
                    ?: 0L,
                tagIds = remote.tagIds
            )

            entitiesByContent[contentKey] = entity
        }

        return ResolvedWords(entitiesByContent.values.toList(), localIdMoves)
    }

    /** Same identity as adding words: the same word in another learning language is a different word. */
    private data class WordContentKey(val originalWord: String, val translation: String, val learningLanguage: String)
}
