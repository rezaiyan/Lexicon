---
description: TDD workflow for Lexicon KMP — kotlin-test, Turbine, MockEngine, coroutines-test, fake-over-mock pattern
---

# TDD for Lexicon KMP

**Test pyramid:** ViewModel (Turbine) → UseCase → Repository (fakes) → DataSource (MockEngine)

**Global TDD workflow:** `~/.claude/rules/testing.md` — this file covers Kotlin/KMP-specific tooling.

---

## Test Structure

```
composeApp/src/commonTest/kotlin/    ← shared (all platforms)
composeApp/src/androidTest/kotlin/   ← Android-only (JUnit 4)
core/testing/src/commonMain/kotlin/  ← shared fakes, rules, helpers
```

All new test files: `commonTest` by default unless the feature is platform-specific.

---

## Fakes Over Mocks

**Prefer hand-written fakes in `:core:testing`.** Mocks are brittle — they test implementation, not behavior.

```kotlin
// ✅ Fake — controls behavior, survives refactors
class FakeWordRepository : WordRepository {
    private val words = mutableListOf<Word>()
    var shouldFail = false

    override suspend fun getWords(): Try<List<Word>> =
        if (shouldFail) Try.failure(DomainError.Unknown) else Try.success(words.toList())

    override suspend fun saveWord(word: Word): Try<Unit> {
        words.add(word)
        return Try.success(Unit)
    }
}

// ❌ Mock — fragile, tests implementation details
val repo = mockk<WordRepository>()
every { repo.getWords() } returns Try.success(listOf(word))
verify { repo.getWords() }  // tests that getWords was called, not the outcome
```

---

## ViewModel Tests with Turbine

Test state transitions, not internal implementation.

```kotlin
@Test
fun `loading then success state transition`() = runTest {
    val repo = FakeWordRepository(words = listOf(word1, word2))
    val viewModel = WordListViewModel(GetWordsUseCase(repo))

    viewModel.effects.test {
        viewModel.loadWords()

        // state assertions
        assertEquals(true, viewModel.state.value.isLoading)
        // after loading completes
        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(2, viewModel.state.value.words.size)

        cancelAndIgnoreRemainingEvents()
    }
}

// Testing effects (one-time events)
@Test
fun `error triggers error effect`() = runTest {
    val repo = FakeWordRepository().apply { shouldFail = true }
    val viewModel = WordListViewModel(GetWordsUseCase(repo))

    viewModel.effects.test {
        viewModel.loadWords()
        val effect = awaitItem()
        assertIs<WordListEffect.ShowError>(effect)
    }
}
```

---

## DataSource Tests with MockEngine

```kotlin
@Test
fun `fetchWords returns mapped words on 200`() = runTest {
    val engine = MockEngine { request ->
        respond(
            content = """[{"id":"1","value":"apple","translation":"Apfel"}]""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
    val dataSource = WordRemoteDataSource(client)

    val result = dataSource.fetchWords()

    assertEquals(1, result.getOrThrow().size)
    assertEquals("apple", result.getOrThrow().first().value)
}
```

---

## UseCase Tests

UseCase tests use fakes, verify orchestration logic:

```kotlin
@Test
fun `invoke maps repository failure to DomainError`() = runTest {
    val repo = FakeWordRepository().apply { shouldFail = true }
    val useCase = GetWordsUseCase(repo)

    val result = useCase(Unit)

    assertTrue(result.isFailure)
    assertIs<DomainError.Unknown>(result.failureOrThrow())
}
```

---

## Coroutine Test Rules

```kotlin
// ✅ runTest — replaces runBlocking in tests, auto-advances virtual time
@Test
fun test() = runTest {
    val viewModel = MyViewModel(testScheduler = testScheduler)
    ...
}

// ✅ TestCoroutineScheduler for time control
@Test
fun `debounce waits before emitting`() = runTest {
    val flow = MutableStateFlow("")
    val debounced = flow.debounce(300)

    flow.value = "a"
    advanceTimeBy(100)
    // not emitted yet
    advanceTimeBy(300)
    // emitted now
}

// ✅ Inject TestDispatcher for deterministic ordering
class MyViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
)
// In test:
val viewModel = MyViewModel(dispatcher = UnconfinedTestDispatcher())
```

---

## Naming Convention

`fun \`[subject] [condition] [expected outcome]\``

```kotlin
fun `getWords when repository fails returns failure Try`()
fun `loadWords when called sets loading state to true`()
fun `saveWord when word already exists returns duplicate error`()
```

---

## Anti-Patterns

| ❌ Never | ✅ Instead |
|---------|-----------|
| `verify { mock.method() }` | Assert the state/output |
| `runBlocking` in tests | `runTest` |
| `Thread.sleep(500)` | `advanceTimeBy` / `awaitItem` |
| Testing `private` methods | Test through `public` API |
| One test file testing multiple classes | One test file per class |
| Shared mutable state between tests | Fresh instance in each `@Test` |
| `mockk` for internal collaborators | Hand-written fake in `:core:testing` |
