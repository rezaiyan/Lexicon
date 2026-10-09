# Import & Export

## Adding Words: one pipeline

Every way of adding words follows the same path:

```
Source → candidates (WordDraft) → review → AddWordsUseCase → local DB + upload queue
```

| Source | Producer | Review step |
|--------|----------|-------------|
| Manual entry | `ManualEntryViewModel` → `WordDraft.create` | none (saved one at a time) |
| Text / CSV file | `FileImportViewModel` → `ParseWordFileUseCase` | shared review |
| Photo (premium) | `PhotoImportViewModel` → `ExtractWordsFromImageUseCase` | shared review |
| AI suggestions (premium) | `AiSuggestViewModel` → `SuggestWordsUseCase` | shared review |
| Onboarding starter words | `AddStarterWordsUseCase` (from `AppNavigationViewModel`) | onboarding preview |

Code lives in:
- `domain/word/add/`: models (`WordDraft`, `LanguagePair`, `AddWordsCommand`, `WordFile`), `VocabularyTextParser`, and the use cases.
- `feature/import/.../feature/addwords/`: `AddWordsViewModel` (host) and the source VMs in `source/`.
- `presentation/ui/components/imports/`: `AddWordsSheet` (page router), `CandidateReviewPage`, and the per-source pages.

### Languages
- Each add uses an explicit `LanguagePair(learning, native)`. There is no global "settings language".
- `ResolveAddWordsLanguagesUseCase` picks the default pair: the last pair used (`AddWordsPreferenceEntity`), else the most common pair in the collection, else none. With none, the sheet asks for both languages first.
- The pair used is saved after every successful commit.

### Writing (`AddWordsUseCase`)
- It is the single write path. It validates drafts, builds `Word.newCard`, and calls `IWordRepository.addWords`.
- That stores the words locally in one transaction, links the tags, and enqueues each word in the upload queue.
- **Dedupe** is keyed on (term, translation, learning language), ignoring case and surrounding spaces. The same word in another learning language counts as new.
- `AddWordsOutcome(added, duplicates, addedTerms)` is returned. The result page previews `addedTerms`.
- **Upload**: the queue flushes in the background after each add and on every app start (`UploadPendingWordsUseCase` in `AppNavigationViewModel.onSessionVerified`). Words added offline reach the server on the next start.
- **Server ids**: `POST /words` returns the saved words. `WordLocalDataSource.completeUpload` moves each uploaded row (with its tags, pending review and queue entry) to its server id, so later edits and deletes address the right server word. If the server id is already stored locally as the same word, the local copy is dropped. If it is held by a different word, that word is moved to a free id first. Rows from before this change are moved when a pull returns their server copy (`ResolvedWords.localIdMoves`).
- **Analytics**: see "Add-words funnel" in `analytics-tracking-plan.md`.

### Errors
Domain errors (`DomainError.AddWords.*`, network errors) map to the `AddWordsProblem` enum and then to string resources in `AddWordsProblemText.kt`. No error string is sniffed from exception messages.

The AI endpoints (photo, suggestions) limit calls per user over a 5-minute window on the server. A 429 maps to `DomainError.Network.RateLimited` and shows `AddWordsProblem.RateLimited` ("try again in a few minutes"). The sheet shows no counter.

## File format (`VocabularyTextParser`)

```
term,translation[,note]
```

- **Delimiters**: comma, tab, semicolon or pipe, auto-detected. A line that lacks the file's delimiter is split by its own.
- **Quoting**: RFC-4180. Fields may contain the delimiter, escaped quotes and line breaks.
- **Ignored**: BOM, Windows line endings, `#` and `//` comments, blank lines, and a header row (`word,translation`, …).
- **Legacy** single-line `word,translation;word,translation` (the export format) is still read.
- **Extra columns** are folded into the note.
- **Limits**: file ≤ 1 MB; term/translation ≤ 200 chars; note ≤ 1000 chars.
- **Encoding**: any text file is accepted. `TextDecoder` reads UTF-16 when there is a byte order mark and UTF-8 otherwise; binary content is rejected.
- **Rejected lines** (missing translation, too long, malformed) are listed under "Skipped lines" in the review.

```
word,translation,note
hello,hola
"to go, to leave",irse,"with ""se"""
house	casa
# comment
```

## Photo
- `IImagePreparer` re-encodes the photo with smaller size and quality until it is ≤ 3 MB. Under 128 bytes means unreadable.
- `POST /ai/extract-words` (v2) sends the language pair and gets structured items back.
- The client only calls v2. The server keeps v1 `/ai/extract-vocabulary` for app versions that predate v2.
- Sideways photos are misread by the model, so the preview has a Rotate button; the upload is turned the same way (`quarterTurns`).

## Export Format (ExportWordsUseCase)

```
word1,translation1;word2,translation2,description2;word3,translation3
```

Entries are separated by semicolons, and each entry is `word,translation[,description]`. The description is omitted when empty. `VocabularyTextParser` reads this format back (legacy mode).
