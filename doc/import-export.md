# Import & Export

## Import Methods

### 1. Manual Text Input (ImportViewModel)
- User types word + translation + optional description
- Single word at a time
- Immediate insert + sync to remote

### 2. File Import (ImportViaFileUseCase)
- Accepts `.txt` files only
- Parses using ImportValidationService
- Delegates to ImportWordsUseCase

### 3. Image Import (ImportFromImageUseCase)
- Requires authentication (premium feature check via IsAiAvailableUseCase)
- Flow: capture/pick image → base64 encode → POST /ai/extract-vocabulary → parse response → import
- Max 3MB image, min 128 bytes
- Returns extracted text in CSV format

### 4. AI Word Generation (AiWordImportViewModel)
- Multi-step wizard: target lang → native lang → level → topics → preview
- POST /onboarding/preferences → receives SuggestedVocabulary list
- User selects which words to import
- ImportSuggestedVocabularyUseCase converts to Word objects

### 5. Onboarding Import (VocabularyPreviewViewModel)
- After onboarding, suggested words previewed
- User selects/deselects, then imports on auth complete

## Import Format (CSV)

### Parsing Rules (ImportValidationService)
```
word,translation[,description]
```

**Separators between entries**: newline (`\n`) or semicolon (`;`)
**Separators within entry**: comma (`,`)
**Comments**: lines starting with `#` are ignored
**Blank lines**: ignored

### Examples
```
hello,hola
goodbye,adiós,a farewell greeting
house,casa;car,coche
# This is a comment
dog,perro,a loyal animal
```

### Edge Cases
- Commas within description are preserved (only first 2 commas split)
- Whitespace is trimmed from each part
- Empty word or translation → entry skipped
- Special characters (accents, CJK, Arabic, emoji) fully supported

### Deduplication
On import, words are deduplicated against existing words using:
```kotlin
Word.isSameContent(other) // compares originalWord + translation (lowercase, trimmed)
```

### New Word Defaults
Imported words get:
- `level = 0`
- `nextReviewDate = now - 1000` (immediately due)
- `easeFactor = 2.5`
- `sourceLanguage` / `targetLanguage` from user's current language setting or explicit params

## Export Format (ExportWordsUseCase)

### Format
```
word1,translation1;word2,translation2,description2;word3,translation3
```

Semicolon-separated entries. Each entry is comma-delimited: `word,translation[,description]`

Description is omitted if empty.

### Round-Trip Compatibility
Export format is designed to be re-importable. Tests verify:
```kotlin
val exported = ExportWordsUseCase()(words)
val reimported = ImportWordsUseCase().execute(exported)
// reimported matches original words
```

## Import Flow in UI

```
StudyScreen → Import Button → ImportMethodSelectorContent
    │
    ├── "Manual" → ImportBottomSheet (fullscreen)
    │    ├── Text tab (single word input)
    │    ├── File tab (.txt upload)
    │    └── Image tab (camera/gallery → AI OCR)
    │
    └── "AI Generate" → AiWordImportBottomSheet
         └── Wizard: lang → lang → level → topics → preview → import
```

## Language Confirmation
When importing via file or image, if detected source/target languages differ from user's setting, a confirmation dialog appears before proceeding.
