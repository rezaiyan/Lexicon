---
description: SOLID, YAGNI, KISS, DRY principles applied to the Lexicon KMP codebase with concrete examples
---

# Design Principles

Principles applied to Lexicon's Clean Architecture. Concrete examples over abstract definitions.

---

## SOLID

### S — Single Responsibility

One reason to change per class. In Lexicon: one UseCase per action, one ViewModel per screen.

```kotlin
// ✅ Focused use case
class GetWordsUseCase : UseCase<Unit, List<Word>> {
    override suspend fun invoke(params: Unit): Try<List<Word>> = ...
}

// ❌ Swiss-army use case — multiple responsibilities
class WordUseCase {
    suspend fun getWords(): Try<List<Word>> = ...
    suspend fun addWord(word: Word): Try<Unit> = ...
    suspend fun deleteWord(id: WordId): Try<Unit> = ...
    fun formatWord(word: Word): String = ...  // formatting ≠ data access
}
```

**Rule:** If naming a class requires "And" (e.g. `FetchAndFormatWordsUseCase`), split it.

### O — Open/Closed

Extend via new use cases and implementations — don't modify existing ones.

```kotlin
// ✅ New behavior = new UseCase, no changes to existing
class GetFilteredWordsUseCase(
    private val getWords: GetWordsUseCase,
    private val filter: WordFilter
) : UseCase<WordFilter.Params, List<Word>> { ... }

// ❌ Adding feature by modifying existing use case
class GetWordsUseCase {
    suspend fun invoke(filter: WordFilter? = null): Try<List<Word>> = ...  // growing params
}
```

### L — Liskov Substitution

Repository implementations must honor the domain contract exactly. Implementations must not narrow the contract.

```kotlin
// ✅ Implementation honors the contract
class WordRepositoryImpl(private val dataSource: WordDataSource) : WordRepository {
    override suspend fun getWords(): Try<List<Word>> =
        dataSource.getAll().map { it.map(WordEntity::toDomain) }
}

// ❌ Implementation adds preconditions not in the interface
class WordRepositoryImpl : WordRepository {
    override suspend fun getWords(): Try<List<Word>> {
        check(isInitialized) { "Must call init() first" }  // caller doesn't know this
        ...
    }
}
```

### I — Interface Segregation

Split large repository interfaces. Clients should not depend on methods they don't use.

```kotlin
// ✅ Narrow interfaces
interface WordReader { suspend fun getWords(): Try<List<Word>> }
interface WordWriter { suspend fun saveWord(word: Word): Try<Unit> }

// ❌ Fat repository — forces all clients to depend on everything
interface WordRepository {
    suspend fun getWords(): Try<List<Word>>
    suspend fun saveWord(word: Word): Try<Unit>
    suspend fun deleteWord(id: WordId): Try<Unit>
    suspend fun importWords(csv: String): Try<Int>
    fun observeWords(): Flow<List<Word>>
    fun observeStudyStats(): Flow<StudyStats>
}
```

### D — Dependency Inversion

`domain` defines interfaces. `data` implements. `presentation` consumes through interfaces. **Never import `data` from `presentation`.**

```kotlin
// ✅ ViewModel depends on domain interface
class WordListViewModel(
    private val getWords: GetWordsUseCase  // domain interface
) : BaseViewModel<...>() { ... }

// ❌ ViewModel depends on concrete implementation
class WordListViewModel(
    private val repository: WordRepositoryImpl  // data layer
) : BaseViewModel<...>() { ... }
```

---

## YAGNI — You Aren't Gonna Need It

**Don't add what isn't required by a current, real use case.**

```kotlin
// ✅ Only what's needed now
data class WordFilter(val query: String)

// ❌ Speculative future params
data class WordFilter(
    val query: String,
    val sortOrder: SortOrder = SortOrder.ASCENDING,   // not used yet
    val maxResults: Int = Int.MAX_VALUE,               // not used yet
    val language: String? = null                       // not used yet
)
```

**Check before adding:** Search `Grep`/CodeGraph — is this called anywhere? If not, remove it.

**Common YAGNI violations:** abstraction layers for hypothetical reuse, optional parameters "just in case", base classes with one subclass, generic versions of specific solutions.

---

## KISS — Keep It Simple

Simplest solution that satisfies the requirement. If you need a comment to explain the logic, simplify the logic.

```kotlin
// ✅ Simple — intention obvious
fun Word.matchesQuery(query: String): Boolean =
    value.contains(query, ignoreCase = true)

// ❌ Complex — optimizing for unmeasured performance
fun Word.matchesQuery(query: String): Boolean {
    val normalizedQuery = query.lowercase().trim()
    val normalizedValue = value.lowercase()
    return when {
        normalizedQuery.isEmpty() -> true
        normalizedQuery.length > normalizedValue.length -> false
        else -> normalizedValue.indexOf(normalizedQuery) >= 0
    }
}
```

**Prefer:** sequential over reactive when there's no concurrency benefit, `data class` over inheritance chains, direct function calls over event buses, `when` over polymorphism for 2-3 variants.

---

## DRY — Don't Repeat Yourself

Every piece of knowledge has one authoritative home. But **don't over-DRY** — similarity isn't always duplication.

### Where to Put Shared Code

| Duplicate | Canonical Home |
|-----------|---------------|
| Domain model constructors/validators | `@JvmInline value class` or domain factory |
| Test fakes | `:core:testing` module |
| Compose theme tokens | `Theme.*` in `:design-system` |
| Error mapping (Entity → DomainError) | Mapper extension in `data` layer |
| Test coroutine setup | `TestCoroutineRule` in `:core:testing` |

```kotlin
// ✅ Single canonical mapper
fun WordEntity.toDomain(): Word = Word(
    id = WordId(id), value = value, translation = translation
)

// ❌ Mapping logic copy-pasted in every use case
```

### When NOT to DRY

Two pieces of code that look the same but represent different concepts should stay separate. Coupling them creates the wrong abstraction.

```kotlin
// ✅ Separate — different lifecycle, different intent
data class StudySessionState(val currentWord: Word, val progress: Int)
data class ReviewSessionState(val currentWord: Word, val progress: Int)

// ❌ Merged to "eliminate duplication" — wrong abstraction
data class SessionState(val mode: SessionMode, val currentWord: Word, val progress: Int)
```


**Tensions:**
- DRY vs YAGNI → extract only on the third occurrence (rule of three)
- KISS vs SRP → split when responsibilities diverge, not just to be "correct"
- OCP vs YAGNI → don't design for extension until extension is actually needed
