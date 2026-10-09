package data.word.sync

import data.core.database.WordEntity
import data.word.remote.model.RemoteWord
import kotlin.test.Test
import kotlin.test.assertEquals

class WordConflictResolverTest {

    private val resolver = WordConflictResolver()

    private fun local(id: Long, term: String, translation: String, learning: String) = WordEntity(
        id = id,
        originalWord = term,
        translation = translation,
        description = "",
        sourceLanguage = "en",
        targetLanguage = learning,
        level = 0,
        easeFactor = 2.5,
        interval = 0,
        repetitions = 0,
        lastReviewDate = 0,
        nextReviewDate = 0,
        dateAdded = 1,
    )

    private fun remote(id: Long, term: String, translation: String, learning: String, level: Int = 0) = RemoteWord(
        id = id,
        originalWord = term,
        translation = translation,
        description = "",
        sourceLanguage = "en",
        targetLanguage = learning,
        level = level,
        easeFactor = 2.5f,
        interval = 0,
        repetitions = 0,
        lastReviewDate = 0,
        nextReviewDate = 0,
    )

    @Test
    fun `resolveConflicts keeps the same word in two learning languages as two words`() {
        val resolved = resolver.resolveConflicts(
            localWords = listOf(local(1, "chat", "cat", "fr"), local(2, "chat", "cat", "de")),
            remoteWords = listOf(remote(10, "chat", "cat", "fr", level = 2), remote(11, "chat", "cat", "de", level = 3)),
        )

        assertEquals(
            setOf(Triple(1, "fr", 2), Triple(2, "de", 3)),
            resolved.map { Triple(it.id, it.targetLanguage, it.level) }.toSet(),
        )
    }

    @Test
    fun `resolveConflicts matches a remote word to the local word with the same content ignoring case`() {
        val resolved = resolver.resolveConflicts(
            localWords = listOf(local(1, "Hund", "dog", "de")),
            remoteWords = listOf(remote(10, " hund", "Dog ", "de", level = 4)),
        )

        assertEquals(listOf(1 to 4), resolved.map { it.id to it.level })
    }
}
