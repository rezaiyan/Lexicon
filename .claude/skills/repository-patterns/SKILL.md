---
name: repository-patterns
description: Create repositories following Lexicon's consistent Try<T>/Flow<T> contracts, interface-driven testability, and extension function mappers
argument-hint: "<repository-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
agent: test-writer
---

# Lexicon Repository Patterns

Use this skill when creating or modifying repositories and data sources.

## Two Contract Rules

Every repository method follows exactly one of two rules:

### Rule 1: Suspend -> always Try<T>

```kotlin
suspend fun updateWord(word: Word): Try<Word>
suspend fun deleteWord(id: Int): Try<Unit>
suspend fun insertWords(words: List<Word>): Try<Int>
suspend fun syncWithRemote(): Try<Unit>
```

**Never** return bare `Unit` or throw exceptions from suspend methods. The return type tells callers whether it can fail.

### Rule 2: Streaming -> always Flow<T>

```kotlin
fun getAllWords(): Flow<List<Word>>
fun getDueCards(): Flow<List<Word>>
fun getProgressStats(): Flow<ProgressStats>

// Multi-step operations: sealed progress inside Flow
fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress>
```

### Exception: Non-fallible reads

Reads that can't fail return `T?` directly — no wrapping needed:

```kotlin
suspend fun getWordById(id: Int): Word?
```

## Repository Interface (in :domain)

```kotlin
interface IWordRepository {
    // Reactive streams
    fun getAllWords(): Flow<List<Word>>
    fun getDueCards(): Flow<List<Word>>

    // One-shot operations — always Try<T>
    suspend fun updateWord(word: Word): Try<Word>
    suspend fun deleteWord(id: Int): Try<Unit>
    suspend fun insertWords(words: List<Word>): Try<Int>
    suspend fun syncWithRemote(): Try<Unit>

    // Non-fallible reads
    suspend fun getWordById(id: Int): Word?
}
```

## Data Source Interfaces (in :domain)

Every remote data source has an interface in `:domain` so repositories depend on abstractions:

```kotlin
// In :domain
interface IWordRemoteDataSource {
    suspend fun fetchWords(): List<WordDto>
    suspend fun addWords(words: List<WordDto>): List<WordDto>
    suspend fun updateWord(word: WordDto): WordDto
    suspend fun deleteWord(id: Int)
}
```

```kotlin
// In :data — implementation
class WordRemoteDataSourceImpl(
    private val client: HttpClient,
) : IWordRemoteDataSource {
    override suspend fun fetchWords(): List<WordDto> {
        return client.get("words").body()
    }
    // ...
}
```

## Mapper Pattern

Use extension functions for all DTO <-> Domain mapping. Consistent, discoverable, testable:

```kotlin
// In :data — mapper file (e.g., WordMapper.kt)
fun WordDto.toDomain(): Word = Word(
    id = id,
    originalWord = original,
    translatedWord = translated,
    bucket = bucket,
    lastReviewedAt = lastReviewed,
)

fun Word.toDto(): WordDto = WordDto(
    id = id,
    original = originalWord,
    translated = translatedWord,
    bucket = bucket,
    lastReviewed = lastReviewedAt,
)

fun WordEntity.toDomain(): Word = Word(...)
fun Word.toEntity(): WordEntity = WordEntity(...)
```

**Never** use top-level functions, in-class methods, or companion object methods for mapping.

## Repository Implementation (in :data)

```kotlin
class WordRepositoryImpl(
    private val localDataSource: WordDao,
    private val remoteDataSource: IWordRemoteDataSource,
) : IWordRepository {

    override fun getAllWords(): Flow<List<Word>> {
        return localDataSource.getAllWords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateWord(word: Word): Try<Word> = Try {
        val dto = remoteDataSource.updateWord(word.toDto())
        val entity = word.toEntity()
        localDataSource.update(entity)
        dto.toDomain()
    }

    override suspend fun syncWithRemote(): Try<Unit> = Try {
        val remote = remoteDataSource.fetchWords()
        val entities = remote.map { it.toDomain().toEntity() }
        localDataSource.upsertAll(entities)
    }
}
```

## Testability

With interfaces for data sources, tests can swap in fakes:

```kotlin
class FakeWordRemoteDataSource : IWordRemoteDataSource {
    private val words = mutableListOf<WordDto>()

    fun seed(vararg dto: WordDto) { words.addAll(dto) }

    override suspend fun fetchWords() = words.toList()
    override suspend fun addWords(words: List<WordDto>) = words.also { this.words.addAll(it) }
    override suspend fun updateWord(word: WordDto) = word.also { /* update in list */ }
    override suspend fun deleteWord(id: Int) { words.removeAll { it.id == id } }
}
```

## DI Registration

```kotlin
// Data source
singleOf(::WordRemoteDataSourceImpl) { bind<IWordRemoteDataSource>() }

// Repository
singleOf(::WordRepositoryImpl) { bind<IWordRepository>() }
```

## Batch Operations

**Anti-pattern — N sequential HTTP requests:**
```kotlin
// NEVER do this — partial failure leaves state inconsistent
wordIds.forEach { wordId ->
    remoteDataSource.assignTags(wordId, tagIds).getOrThrow() // fails on word #3? 1–2 are done, 3–N are not
}
```

**Correct pattern — single batch endpoint:**
```kotlin
// UseCase
override suspend fun invoke(params: BatchAssignTagsParams): Try<Int> = Try {
    tagRepository.batchAssignWordTags(
        wordIds = params.wordIds.map { it.toLong() },
        tagIds = params.tagIds
    ).getOrThrow()
    params.wordIds.size
}

// Repository
override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try {
    remoteDataSource.batchUpdateWordTags(wordIds, tagIds).getOrThrow()  // single HTTP request
    localDataSource.batchSetWordTags(wordIds, tagIds)                    // single SQLDelight transaction
}

// Local data source — wrap entire batch in ONE transaction
override suspend fun batchSetWordTags(wordIds: List<Long>, tagIds: List<Long>) {
    queries.transaction {                         // ← one transaction wraps all words
        wordIds.forEach { wordId ->
            queries.deleteWordTagsForWord(wordId)
            tagIds.forEach { tagId -> queries.insertWordTag(wordId, tagId) }
        }
    }
}
```

**Backend rule — never save() a parent entity just to modify a join table:**

If you only need to modify a join table (e.g., `word_tags`), use native `@Modifying @Query` directly on that table. Calling `wordRepository.save(word)` bumps `@Version` and causes `OptimisticLockingFailureException` under concurrent load.

```kotlin
// NEVER: loads Word entity, bumps @Version — breaks under concurrent sync
fun updateWordTags(wordId: Long, tagIds: List<Long>) {
    val word = wordRepository.findById(wordId).orElseThrow()
    word.tags = tagRepository.findAllById(tagIds).toMutableSet()
    wordRepository.save(word) // ← @Version bump — unnecessary, dangerous
}

// CORRECT: native SQL on join table, no @Version involved
@Modifying
@Query("DELETE FROM word_tags WHERE word_id IN :wordIds AND ...", nativeQuery = true)
fun deleteWordTagsByWordIdsAndUserId(wordIds: List<Long>, userId: Long)

@Modifying
@Query("INSERT INTO word_tags (word_id, tag_id) SELECT ... ON CONFLICT DO NOTHING", nativeQuery = true)
fun insertWordTagsByWordIdsAndUserId(wordIds: List<Long>, tagId: Long, userId: Long)
```

## Checklist

1. Interface in `:domain` — implementation in `:data`
2. Suspend methods return `Try<T>` — never throw
3. Stream methods return `Flow<T>`
4. Data source has interface in `:domain`, impl in `:data`
5. Mappers are extension functions: `Dto.toDomain()`, `Domain.toDto()`, `Entity.toDomain()`
6. No platform imports in repository interfaces
7. Registered in AppModule.kt with `bind<Interface>()`
8. Batch mutations use a single dedicated endpoint — never loop N sequential HTTP requests
9. Join table modifications use native `@Modifying @Query` — never `save()` the parent entity just for join table changes
