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

## Checklist

1. Interface in `:domain` — implementation in `:data`
2. Suspend methods return `Try<T>` — never throw
3. Stream methods return `Flow<T>`
4. Data source has interface in `:domain`, impl in `:data`
5. Mappers are extension functions: `Dto.toDomain()`, `Domain.toDto()`, `Entity.toDomain()`
6. No platform imports in repository interfaces
7. Registered in AppModule.kt with `bind<Interface>()`
