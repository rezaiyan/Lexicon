---
name: usecase-patterns
description: Create use cases following Lexicon's UseCase/FlowUseCase base interfaces with Try<T> return types and stateless contracts
argument-hint: "<usecase-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
agent: test-writer
---

# Lexicon Use Case Patterns

Use this skill when creating or modifying use cases.

## Base Interfaces

Every use case implements one of two `fun interface` contracts:

```kotlin
// One-shot suspend operations — always return Try<R>
fun interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Try<R>
}

// Reactive stream operations — return Flow<R>
fun interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}

// Convenience aliases for parameterless use cases
typealias NoParamUseCase<R> = UseCase<Unit, R>
typealias NoParamFlowUseCase<R> = FlowUseCase<Unit, R>
```

## Return Type Rules

| Operation type | Return type | Example |
|---|---|---|
| One-shot that can fail | `Try<T>` | `UpdateWordUseCase : UseCase<Word, Word>` |
| One-shot returning Unit | `Try<Unit>` | `DeleteWordUseCase : UseCase<Int, Unit>` |
| One-shot returning Boolean | `Try<Boolean>` | `IsAuthenticatedUseCase : UseCase<Unit, Boolean>` |
| Reactive stream | `Flow<T>` | `GetDueWordsUseCase : FlowUseCase<Unit, List<Word>>` |
| Stream with errors | `Flow<T>` | errors handled in VM via `.catch {}` |

**Never** return bare `Unit`, `Boolean`, `String`, or `sealed class` from suspend use cases. Always wrap in `Try<T>`.

**Never** return `Flow<Try<T>>` — use `Flow<T>` and let the ViewModel handle errors via `.catch {}`.

## Examples

```kotlin
// Suspend use case with params
class ReviewWordUseCase(
    private val wordRepository: IWordRepository,
) : UseCase<ReviewWordUseCase.Params, Word> {

    data class Params(val word: Word, val quality: Int)

    override suspend fun invoke(params: Params): Try<Word> {
        return wordRepository.updateWord(params.word.copy(
            bucket = calculateBucket(params.word, params.quality)
        ))
    }
}

// Suspend use case without params
class SyncWordsUseCase(
    private val wordRepository: IWordRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke(params: Unit): Try<Unit> {
        return wordRepository.syncWithRemote()
    }
}

// Flow use case
class GetDueWordsUseCase(
    private val wordRepository: IWordRepository,
) : NoParamFlowUseCase<List<Word>> {

    override fun invoke(params: Unit): Flow<List<Word>> {
        return wordRepository.getDueCards()
    }
}
```

## Stateless Contract

Use cases must be **stateless** — no mutable fields, no `var`, no caching. If state is needed, it belongs in the repository or a dedicated state holder.

```kotlin
// WRONG — stateful use case
class ScheduleNotificationsUseCase(...) {
    private var hasScheduled = false  // violation!
    ...
}

// RIGHT — stateless, repository owns the state
class ScheduleNotificationsUseCase(
    private val notificationRepository: INotificationRepository,
) : NoParamUseCase<Unit> {
    override suspend fun invoke(params: Unit): Try<Unit> {
        return notificationRepository.scheduleIfNeeded()
    }
}
```

## Integration with BaseViewModel

Use cases return `Try<T>`, which integrates with `BaseViewModel.reduce`:

```kotlin
// In ViewModel
fun reviewWord(word: Word, quality: Int) {
    viewModelScope.launch {
        reviewWordUseCase(ReviewWordUseCase.Params(word, quality)).reduce(
            onSuccess = { copy(reviewedCount = reviewedCount + 1) },
            onFailure = { copy(error = it.message) }
        )
    }
}
```

## DI Registration

```kotlin
factoryOf(::ReviewWordUseCase)  // factory — new instance per injection
```

Use `factory` (not `single`) so use cases are lightweight and don't accumulate state.

## Checklist

1. Implements `UseCase<P, R>` or `FlowUseCase<P, R>`
2. Suspend use cases return `Try<T>` — never bare types
3. Flow use cases return `Flow<T>` — never `Flow<Try<T>>`
4. Stateless — no mutable fields
5. Single responsibility — one business operation per class
6. Dependencies injected via constructor
7. Registered in AppModule.kt via `factoryOf(::UseCaseName)`
