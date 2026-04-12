# Plan: Word List Search, Sort & Filter by Tag

**Status:** PENDING
**Type:** Feature
**Worktree:** No
**Approved:** No

## Goal

Let users find and organize words without scrolling through hundreds. Three capabilities:
1. **Search** by word value or translation
2. **Sort** by date added, level, or alphabetical
3. **Filter** by tag

## Context

- `VocabularyViewModel` — `feature/words/src/.../VocabularyViewModel.kt` — check current state/filter support
- `IWordRepository.getWords()` — check if it supports filtering/sorting params
- Tags: `ITagRepository` exists with `observeTags()` and `getWordsByTagId()`
- Current word list UI — check `feature/words/src/.../ui/` for screen files
- `GetWordsUseCase` or `ObserveWordsUseCase` — check parameters

## Implementation Tasks

- [ ] **T1** Audit current `VocabularyViewModel` and word list screen
  - Understand existing state shape
  - Check if search/filter is partially implemented

- [ ] **T2** Add search + sort + filter state to `VocabularyViewModel`
  - `searchQuery: String`, `sortOrder: WordSortOrder`, `selectedTagId: Long?` to ViewModel state
  - `WordSortOrder` enum: `DATE_ADDED`, `ALPHABETICAL`, `LEVEL_ASC`, `LEVEL_DESC`
  - Derive `filteredWords` from full word list + current filter state

- [ ] **T3** Add event-sink methods: `onSearchQueryChanged(query: String)`, `onSortOrderChanged(order: WordSortOrder)`, `onTagFilterChanged(tagId: Long?)`

- [ ] **T4** Search bar composable — persistent top of word list, with clear button

- [ ] **T5** Sort bottom sheet — radio group of sort options, triggered by sort button in toolbar

- [ ] **T6** Tag filter chips — horizontal scrollable row of tag pills, tap to select/deselect

- [ ] **T7** Tests
  - `VocabularyViewModelTest`: verify search filters words by query
  - `VocabularyViewModelTest`: verify sort orders are applied correctly
  - `VocabularyViewModelTest`: verify tag filter reduces words to tagged subset

## Files to Modify

| File | Change |
|------|--------|
| `feature/words/src/.../VocabularyViewModel.kt` | Add filter/sort/search state + methods |
| `feature/words/src/.../ui/VocabularyScreen.kt` | Add search bar + sort button + tag chips |
| `composeApp/src/commonTest/.../VocabularyViewModelTest.kt` | New filter/sort tests |

## Done: 0 / Left: 7
