package data.tag.local

import app.cash.turbine.test
import domain.tag.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagLocalDataSourceContractTest {

    private class FakeTagLocalDataSource : ITagLocalDataSource {
        private val _tags = MutableStateFlow<List<Tag>>(emptyList())
        val wordTagMap = mutableMapOf<Long, MutableList<Long>>()

        override fun getTags(): Flow<List<Tag>> = _tags

        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> =
            _tags.map { emptyMap() }

        override fun getDueTags(): Flow<List<Tag>> = _tags

        override suspend fun insertOrReplaceTag(
            id: Long,
            name: String,
            createdAt: Long,
            updatedAt: Long
        ) {
            val updated = _tags.value.toMutableList()
            updated.removeAll { it.id == id }
            updated.add(Tag(id = id, name = name, wordCount = 0L, createdAt = createdAt, updatedAt = updatedAt))
            _tags.value = updated
        }

        override suspend fun deleteTag(id: Long) {
            _tags.value = _tags.value.filter { it.id != id }
            wordTagMap.values.forEach { it.remove(id) }
        }

        override suspend fun replaceAllTags(tags: List<Tag>) {
            _tags.value = tags.toList()
        }

        override suspend fun setWordTags(wordId: Long, tagIds: List<Long>) {
            wordTagMap[wordId] = tagIds.toMutableList()
        }

        override suspend fun batchSetWordTags(wordIds: List<Long>, tagIds: List<Long>) {
            wordIds.forEach { wordId -> wordTagMap[wordId] = tagIds.toMutableList() }
        }

        override suspend fun getTagIdsForWord(wordId: Long): List<Long> =
            wordTagMap[wordId] ?: emptyList()
    }

    private fun makeTag(
        id: Long = 1L,
        name: String = "Tag",
        createdAt: Long = 1000L,
        updatedAt: Long = 2000L
    ) = Tag(id = id, name = name, wordCount = 0L, createdAt = createdAt, updatedAt = updatedAt)

    private val dataSource = FakeTagLocalDataSource()

    // -------------------------------------------------------------------------
    // insertOrReplaceTag
    // -------------------------------------------------------------------------

    @Test
    fun `insertOrReplaceTag adds tag to list`() = runTest {
        dataSource.insertOrReplaceTag(id = 1L, name = "Spanish", createdAt = 1000L, updatedAt = 2000L)

        dataSource.getTags().test {
            val tags = awaitItem()
            assertEquals(1, tags.size)
            assertEquals(1L, tags.first().id)
            assertEquals("Spanish", tags.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insertOrReplaceTag replaces existing tag with same id`() = runTest {
        dataSource.insertOrReplaceTag(id = 1L, name = "Original", createdAt = 1000L, updatedAt = 2000L)
        dataSource.insertOrReplaceTag(id = 1L, name = "Updated", createdAt = 1000L, updatedAt = 3000L)

        dataSource.getTags().test {
            val tags = awaitItem()
            assertEquals(1, tags.size)
            assertEquals("Updated", tags.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // deleteTag
    // -------------------------------------------------------------------------

    @Test
    fun `deleteTag removes tag from list`() = runTest {
        val freshDataSource = FakeTagLocalDataSource()
        freshDataSource.insertOrReplaceTag(id = 5L, name = "ToDelete", createdAt = 1000L, updatedAt = 2000L)
        freshDataSource.insertOrReplaceTag(id = 6L, name = "ToKeep", createdAt = 1000L, updatedAt = 2000L)

        freshDataSource.deleteTag(5L)

        freshDataSource.getTags().test {
            val tags = awaitItem()
            assertEquals(1, tags.size)
            assertEquals(6L, tags.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // replaceAllTags
    // -------------------------------------------------------------------------

    @Test
    fun `replaceAllTags clears and inserts new list`() = runTest {
        val freshDataSource = FakeTagLocalDataSource()
        freshDataSource.insertOrReplaceTag(id = 1L, name = "OldTag", createdAt = 1000L, updatedAt = 2000L)

        val newTags = listOf(
            makeTag(id = 10L, name = "NewTag1"),
            makeTag(id = 11L, name = "NewTag2")
        )
        freshDataSource.replaceAllTags(newTags)

        freshDataSource.getTags().test {
            val tags = awaitItem()
            assertEquals(2, tags.size)
            assertEquals("NewTag1", tags[0].name)
            assertEquals("NewTag2", tags[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // setWordTags
    // -------------------------------------------------------------------------

    @Test
    fun `setWordTags stores tagIds for wordId`() = runTest {
        val freshDataSource = FakeTagLocalDataSource()
        freshDataSource.setWordTags(wordId = 42L, tagIds = listOf(1L, 2L, 3L))

        val result = freshDataSource.getTagIdsForWord(42L)

        assertEquals(3, result.size)
        assertTrue(result.containsAll(listOf(1L, 2L, 3L)))
    }

    @Test
    fun `setWordTags replaces existing tags for word`() = runTest {
        val freshDataSource = FakeTagLocalDataSource()
        freshDataSource.setWordTags(wordId = 42L, tagIds = listOf(1L, 2L))
        freshDataSource.setWordTags(wordId = 42L, tagIds = listOf(3L, 4L, 5L))

        val result = freshDataSource.getTagIdsForWord(42L)

        assertEquals(3, result.size)
        assertTrue(result.containsAll(listOf(3L, 4L, 5L)))
    }

    // -------------------------------------------------------------------------
    // batchSetWordTags
    // -------------------------------------------------------------------------

    @Test
    fun `batchSetWordTags assigns same tags to multiple words`() = runTest {
        val freshDataSource = FakeTagLocalDataSource()
        freshDataSource.batchSetWordTags(
            wordIds = listOf(10L, 20L, 30L),
            tagIds = listOf(100L, 200L)
        )

        val result10 = freshDataSource.getTagIdsForWord(10L)
        val result20 = freshDataSource.getTagIdsForWord(20L)
        val result30 = freshDataSource.getTagIdsForWord(30L)

        assertEquals(listOf(100L, 200L), result10)
        assertEquals(listOf(100L, 200L), result20)
        assertEquals(listOf(100L, 200L), result30)
    }

    // -------------------------------------------------------------------------
    // getTagIdsForWord
    // -------------------------------------------------------------------------

    @Test
    fun `getTagIdsForWord returns empty list when no tags assigned`() = runTest {
        val freshDataSource = FakeTagLocalDataSource()

        val result = freshDataSource.getTagIdsForWord(wordId = 999L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTagIdsForWord returns assigned tag ids`() = runTest {
        val freshDataSource = FakeTagLocalDataSource()
        freshDataSource.setWordTags(wordId = 7L, tagIds = listOf(11L, 22L))

        val result = freshDataSource.getTagIdsForWord(wordId = 7L)

        assertEquals(2, result.size)
        assertEquals(11L, result[0])
        assertEquals(22L, result[1])
    }

    // -------------------------------------------------------------------------
    // getTags flow
    // -------------------------------------------------------------------------

    @Test
    fun `getTags flow emits current tag list`() = runTest {
        val freshDataSource = FakeTagLocalDataSource()
        freshDataSource.insertOrReplaceTag(id = 1L, name = "Alpha", createdAt = 1000L, updatedAt = 2000L)
        freshDataSource.insertOrReplaceTag(id = 2L, name = "Beta", createdAt = 1000L, updatedAt = 2000L)

        freshDataSource.getTags().test {
            val tags = awaitItem()
            assertEquals(2, tags.size)
            val names = tags.map { it.name }
            assertTrue(names.contains("Alpha"))
            assertTrue(names.contains("Beta"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
