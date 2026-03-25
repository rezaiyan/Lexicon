package data.tag.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import domain.tag.model.Tag
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TagLocalDataSource(
    private val queries: LexiconQueries
) : ITagLocalDataSource {

    override fun getTags(): Flow<List<Tag>> {
        return queries.getAllTagsWithWordCount().asFlow().mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    Tag(
                        id = row.id,
                        name = row.name,
                        wordCount = row.wordCount,
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt,
                    )
                }
            }
    }

    override fun getDueTags(): Flow<List<Tag>> {
        return combine(
            queries.countWords().asFlow().mapToOneOrNull(Dispatchers.Default),
            queries.countWordTags().asFlow().mapToOneOrNull(Dispatchers.Default)
        ) { _, _ -> }
            .map {
                val currentTime = Clock.System.now().toEpochMilliseconds()
                queries.getDueTagWordCounts(currentTime).awaitAsList().map { row ->
                    Tag(
                        id = row.id,
                        name = row.name,
                        wordCount = row.wordCount,
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt,
                    )
                }
            }
    }

    override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> {
        return queries.getTagWordCountsByLevel().asFlow().mapToList(Dispatchers.Default)
            .map { rows ->
                rows.groupBy { it.level.toInt() }
                    .mapValues { (_, levelRows) ->
                        levelRows.map { row ->
                            Tag(
                                id = row.id,
                                name = row.name,
                                wordCount = row.wordCount,
                                createdAt = row.createdAt,
                                updatedAt = row.updatedAt,
                            )
                        }
                    }
            }
    }

    override suspend fun insertOrReplaceTag(id: Long, name: String, createdAt: Long, updatedAt: Long) {
        queries.insertOrReplaceTag(id, name, createdAt, updatedAt)
    }

    override suspend fun deleteTag(id: Long) {
        queries.transaction {
            queries.deleteWordTagsForTag(id)
            queries.deleteTag(id)
        }
    }

    override suspend fun replaceAllTags(tags: List<Tag>) {
        queries.transaction {
            queries.deleteAllTags()
            tags.forEach { tag ->
                queries.insertOrReplaceTag(tag.id, tag.name, tag.createdAt, tag.updatedAt)
            }
        }
    }

    override suspend fun setWordTags(wordId: Long, tagIds: List<Long>) {
        queries.transaction {
            queries.deleteWordTagsForWord(wordId)
            tagIds.forEach { tagId ->
                queries.insertWordTag(wordId, tagId)
            }
        }
    }

    override suspend fun batchSetWordTags(wordIds: List<Long>, tagIds: List<Long>) {
        queries.transaction {
            wordIds.forEach { wordId ->
                queries.deleteWordTagsForWord(wordId)
                tagIds.forEach { tagId ->
                    queries.insertWordTag(wordId, tagId)
                }
            }
        }
    }

    override suspend fun getTagIdsForWord(wordId: Long): List<Long> {
        return queries.getTagIdsForWord(wordId).executeAsList()
    }
}
