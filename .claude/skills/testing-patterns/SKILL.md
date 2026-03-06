---
name: testing-patterns
description: Write tests following Lexicon's test pyramid — ViewModel tests with Turbine, repository tests with fakes, DataSource tests with MockEngine
argument-hint: "<test-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash"]
agent: test-writer
---

# Lexicon Testing Patterns

Use this skill when writing or reviewing tests.

## Test Pyramid

```
  ViewModel Tests (Turbine)        <- state transitions, effects, event sink
  Repository Tests (fake DS)       <- mapping, local+remote coordination
  DataSource Tests (MockEngine)    <- HTTP serialization, error mapping
  Domain / Use Case Tests          <- business logic in isolation
  Instrumented / Integration       <- Room DB, SRS regression (keep)
```

## Stack

- `kotlin-test` for assertions
- `kotlinx-coroutines-test` for `runTest`, `TestDispatcher`
- `Turbine` for Flow/StateFlow testing
- Ktor `MockEngine` for HTTP client testing
- Manual fakes (not mocking libraries) for all dependencies

## Shared Test Utilities (:core:testing)

Reusable fakes and builders live in `:core:testing`:

```kotlin
// Fake repository
class FakeWordRepository : IWordRepository {
    private val words = MutableStateFlow<List<Word>>(emptyList())
    fun emit(words: List<Word>) { this.words.value = words }

    override fun getAllWords() = words.asStateFlow()
    override suspend fun updateWord(w: Word) = Try.success(w)
    // ... all interface methods
}

// Test data builder
fun testWord(
    id: Int = 1,
    original: String = "hello",
    translated: String = "hola",
    bucket: Int = 0,
) = Word(id = id, originalWord = original, translatedWord = translated, bucket = bucket)
```

## ViewModel Tests

Test state transitions and effects using Turbine:

```kotlin
class StudyViewModelTest {

    private val fakeRepo = FakeWordRepository()
    private val reviewUseCase = ReviewWordUseCase(fakeRepo)
    private lateinit var vm: StudyViewModel

    @Test
    fun `review word updates progress`() = runTest {
        vm = StudyViewModel(fakeRepo, reviewUseCase)

        // Test state
        vm.state.test {
            val initial = awaitItem()
            assertEquals(UiState.Loading, initial)

            fakeRepo.emit(listOf(testWord()))
            val loaded = awaitItem()
            assertTrue(loaded is UiState.Success)
        }

        // Test effects
        vm.effects.test {
            vm.reviewWord(testWord(), quality = 4)
            val effect = awaitItem()
            assertTrue(effect is StudyEffect.ShowSnackbar)
        }
    }
}
```

### Key rules for VM tests:
- Use `runTest` for coroutine context
- Test `state` and `effects` separately via Turbine `.test {}`
- Call public methods (event sink) to trigger state changes
- Assert on `UiState` transitions: Loading -> Success/Error
- Never access private VM fields

## Repository Tests

Test with in-memory Room + fake remote data source:

```kotlin
class WordRepositoryTest {

    private val fakeRemote = FakeWordRemoteDataSource()
    private val fakeLocal = FakeWordDao()
    private lateinit var repo: WordRepositoryImpl

    @BeforeTest
    fun setup() {
        repo = WordRepositoryImpl(fakeLocal, fakeRemote)
    }

    @Test
    fun `sync fetches remote and stores locally`() = runTest {
        fakeRemote.seed(WordDto(id = 1, original = "hello"))

        val result = repo.syncWithRemote()

        assertTrue(result.isSuccess)
        assertEquals(1, fakeLocal.getAllWords().first().size)
    }

    @Test
    fun `updateWord returns failure on network error`() = runTest {
        fakeRemote.shouldFail = true

        val result = repo.updateWord(testWord())

        assertTrue(result.isFailure)
    }
}
```

## DataSource Tests (Ktor MockEngine)

```kotlin
class WordRemoteDataSourceTest {

    @Test
    fun `fetchWords deserializes response`() = runTest {
        val engine = MockEngine { request ->
            respond(
                content = """[{"id":1,"original":"hello","translated":"hola"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
        val dataSource = WordRemoteDataSourceImpl(client)

        val words = dataSource.fetchWords()

        assertEquals(1, words.size)
        assertEquals("hello", words[0].original)
    }
}
```

## Use Case Tests

Test business logic with fake repositories:

```kotlin
class ReviewWordUseCaseTest {

    private val fakeRepo = FakeWordRepository()
    private val useCase = ReviewWordUseCase(fakeRepo)

    @Test
    fun `high quality moves word to next bucket`() = runTest {
        val word = testWord(bucket = 2)

        val result = useCase(ReviewWordUseCase.Params(word, quality = 4))

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().bucket)
    }
}
```

## Conventions

- Test class name: `{ClassName}Test`
- Test function name: `` fun `descriptive name with backticks`() ``
- Use `runTest` for all coroutine tests
- No `!!` in tests
- No try-catch — use assertions
- Fakes over mocks — manual fakes for full control
- Tests go in `composeApp/src/commonTest/kotlin/` (shared) or `composeApp/src/androidTest/kotlin/` (Android)

## Checklist

1. Uses `runTest` for coroutines
2. Uses Turbine `.test {}` for Flow assertions
3. Uses manual fakes, not mocking libraries
4. Tests state transitions (Loading -> Success, Loading -> Error)
5. Tests effects/events separately from state
6. Tests error cases, not just happy path
7. No `!!`, no try-catch for control flow
8. Test file in correct source set
