# REFACTOR-1 — Extract CSV & Error Logic from ImportViewModel

**Priority:** P1
**Status:** Open
**Wave:** 1 (highest impact, lowest risk)

## Problem

`ImportViewModel` contains three pieces of domain logic that belong in the domain layer:

1. **CSV parsing** — splits raw text into `ExtractedWordItem` objects using regex, filters comment
   lines, validates required fields. Currently a private `parseCsvToWordItems()` method.
2. **CSV generation** — converts a list of words back to CSV string, handles optional description
   column, escapes commas. Duplicated in both `addWord()` and `confirmImageImport()`.
3. **Error classification** — detects network vs. "no vocabulary" vs. generic errors by
   string-matching on `error.message`. Duplicated in both `addWord()` and `importImage()`.

**Files with the problem:**
- `presentation/src/commonMain/kotlin/presentation/ui/components/imports/ImportViewModel.kt`
  - `addWord()` lines ~167–224 — CSV generation + error classification
  - `parseCsvToWordItems()` lines ~499–515 — CSV parsing
  - `importImage()` lines ~253–316 — error classification (duplicated)
  - `confirmImageImport()` lines ~336–355 — CSV generation (duplicated)

## Files to Create

### Domain models
```
domain/src/commonMain/kotlin/domain/import/model/ParsedWord.kt
```
Plain data class: `word: String`, `translation: String`, `description: String`.

```
domain/src/commonMain/kotlin/domain/import/model/ErrorClassification.kt
```
Sealed class: `Network`, `NoVocabularyFound`, `Generic(message: String)`.

### Domain use cases
```
domain/src/commonMain/kotlin/domain/import/usecase/ParseCsvWordsUseCase.kt
```
`UseCase<String, List<ParsedWord>>` — splits on `[;\n]+`, strips blanks, filters `#` comment
lines, validates `word` and `translation` present, returns `Try<List<ParsedWord>>`.

```
domain/src/commonMain/kotlin/domain/import/usecase/FormatWordsToCsvUseCase.kt
```
`UseCase<List<ParsedWord>, String>` — joins to CSV string, omits description column when blank.

```
domain/src/commonMain/kotlin/domain/import/usecase/ClassifyImportErrorUseCase.kt
```
`UseCase<String, ErrorClassification>` — maps raw error message to `ErrorClassification`.
Matching rules (kept in one place):
- Contains "timeout", "connect", "network" → `Network`
- Contains "empty", "no words", "no vocabulary", "nothing" → `NoVocabularyFound`
- Otherwise → `Generic(message)`

### Tests
```
domain/src/commonTest/kotlin/domain/import/ParseCsvWordsUseCaseTest.kt
domain/src/commonTest/kotlin/domain/import/FormatWordsToCsvUseCaseTest.kt
domain/src/commonTest/kotlin/domain/import/ClassifyImportErrorUseCaseTest.kt
presentation/src/commonTest/kotlin/.../ImportViewModelTest.kt
```

## Files to Modify

```
presentation/.../ImportViewModel.kt
```
- `addWord()` — replace inline CSV line assembly with `FormatWordsToCsvUseCase`
- `addWord()` error handler — replace inline string matching with `ClassifyImportErrorUseCase`,
  map `ErrorClassification` to user-facing string
- `parseCsvToWordItems()` — delete; replace all call sites with `ParseCsvWordsUseCase`
- `importImage()` error handler — replace inline string matching with `ClassifyImportErrorUseCase`
- `confirmImageImport()` — replace inline CSV join with `FormatWordsToCsvUseCase`

```
composeApp/src/commonMain/kotlin/di/AppModule.kt
```
Add:
```kotlin
factoryOf(::ParseCsvWordsUseCase)
factoryOf(::FormatWordsToCsvUseCase)
factoryOf(::ClassifyImportErrorUseCase)
```

## Test Cases

### `ParseCsvWordsUseCaseTest`
- Happy path: `"apple,Apfel,a fruit\ncar,Auto"` → 2 items, description present on first only
- Semicolon delimiter: `"apple,Apfel;car,Auto"` → 2 items
- Mixed delimiters: semicolons + newlines in same string → all items parsed
- Blank lines filtered: extra `\n\n` between entries → still 2 items
- Comment lines filtered: `"#comment\napple,Apfel"` → 1 item
- Missing translation: `"apple"` → 0 items (invalid, skipped)
- Whitespace trimmed: `"  apple , Apfel "` → `word="apple"`, `translation="Apfel"`

### `FormatWordsToCsvUseCaseTest`
- With description: `ParsedWord("a","b","c")` → `"a,b,c"`
- Without description: `ParsedWord("a","b","")` → `"a,b"`
- Multiple words → newline-joined
- Empty list → empty string

### `ClassifyImportErrorUseCaseTest`
- "Connection timeout" → `Network`
- "network error" (case-insensitive) → `Network`
- "No vocabulary found in image" → `NoVocabularyFound`
- "empty response" → `NoVocabularyFound`
- "Server error 500" → `Generic("Server error 500")`
- Blank message → `Generic("")`

### `ImportViewModelTest`
- `addWord()` with blank word → no use case called, no state change
- `addWord()` success → `ParsedWord` formatted and appended via `FormatWordsToCsvUseCase`
- `importImage()` network failure → state shows network error message
- `confirmImageImport()` → words serialised via `FormatWordsToCsvUseCase` before import

## Acceptance Criteria

- [ ] `parseCsvToWordItems()` deleted from `ImportViewModel`
- [ ] Inline CSV generation removed from `addWord()` and `confirmImageImport()`
- [ ] Inline error string-matching removed from `addWord()` and `importImage()`
- [ ] All three use cases have 100% branch coverage in tests
- [ ] `ImportViewModel` tests use fakes for all three new use cases
- [ ] `./gradlew composeApp:compileKotlinMetadata` passes
- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` passes
