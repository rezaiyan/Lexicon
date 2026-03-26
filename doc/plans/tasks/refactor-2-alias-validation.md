# REFACTOR-2 — Extract Alias Validation from EditProfileViewModel

**Priority:** P2
**Status:** Open
**Wave:** 2 (small, isolated, zero risk)

## Problem

`EditProfileViewModel.saveProfile()` contains inline validation rules for the display alias:

```kotlin
// EditProfileViewModel.kt ~line 66–74
if (alias.isNotEmpty() && (alias.length < 2 || alias.length > 30)) {
    updateState { copy(errorMessage = "Username must be 2-30 characters") }
    return
}
if (alias.isNotEmpty() && !alias.matches("^[a-zA-Z0-9 _-]+$".toRegex())) {
    updateState { copy(errorMessage = "Only letters, numbers, spaces, underscores, and hyphens allowed") }
    return
}
```

These are **business rules** (not UI decisions) that belong in the domain layer so they can be:
- Tested independently of the ViewModel
- Reused if validation is needed elsewhere (e.g., server-side mirroring, future onboarding alias step)

**Files with the problem:**
- `feature/profile/src/commonMain/kotlin/feature/profile/EditProfileViewModel.kt`

## Files to Create

### Domain model
```
domain/src/commonMain/kotlin/domain/profile/model/AliasValidationResult.kt
```
```kotlin
sealed interface AliasValidationResult {
    data object Valid : AliasValidationResult
    data object TooShort : AliasValidationResult       // non-blank and < 2 chars
    data object TooLong : AliasValidationResult        // > 30 chars
    data object InvalidCharacters : AliasValidationResult
}
```

### Domain use case
```
domain/src/commonMain/kotlin/domain/profile/usecase/ValidateDisplayAliasUseCase.kt
```
Pure function — no repository dependencies, no `Try<T>` needed (cannot fail).
```kotlin
class ValidateDisplayAliasUseCase {
    operator fun invoke(alias: String): AliasValidationResult
}
```
Rules:
- Blank/empty → `Valid` (clearing alias is allowed)
- Length < 2 → `TooShort`
- Length > 30 → `TooLong`
- Does not match `^[a-zA-Z0-9 _-]+$` → `InvalidCharacters`
- Otherwise → `Valid`

### Tests
```
domain/src/commonTest/kotlin/domain/profile/ValidateDisplayAliasUseCaseTest.kt
feature/profile/src/commonTest/kotlin/feature/profile/EditProfileViewModelTest.kt
```

## Files to Modify

```
feature/profile/.../EditProfileViewModel.kt
```
- Inject `ValidateDisplayAliasUseCase` via constructor
- Replace the two `if`-blocks in `saveProfile()` with:
  ```kotlin
  when (validateDisplayAliasUseCase(alias)) {
      AliasValidationResult.TooShort -> { updateState { copy(errorMessage = "...") }; return }
      AliasValidationResult.TooLong -> { updateState { copy(errorMessage = "...") }; return }
      AliasValidationResult.InvalidCharacters -> { updateState { copy(errorMessage = "...") }; return }
      AliasValidationResult.Valid -> Unit
  }
  ```

```
composeApp/src/commonMain/kotlin/di/AppModule.kt
```
Add:
```kotlin
factoryOf(::ValidateDisplayAliasUseCase)
```

## Test Cases

### `ValidateDisplayAliasUseCaseTest`
- Empty string → `Valid` (clearing alias)
- Blank string (spaces only) → `Valid` (backend will treat as clear)
- 1 character → `TooShort`
- 2 characters → `Valid`
- 30 characters → `Valid`
- 31 characters → `TooLong`
- `"john_doe"` → `Valid`
- `"john doe"` → `Valid` (space allowed)
- `"john-doe"` → `Valid` (hyphen allowed)
- `"john@doe"` → `InvalidCharacters`
- `"abc!"` → `InvalidCharacters`
- Unicode: `"héllo"` → `InvalidCharacters`
- All digits: `"123"` → `Valid`

### `EditProfileViewModelTest`
- Entering 1-char alias and calling `saveProfile()` → state has `errorMessage`, no UseCase called upstream
- Entering invalid chars and calling `saveProfile()` → state has `errorMessage`
- Entering valid alias → `updateProfileUseCase` called, error cleared
- Empty alias → `updateProfileUseCase` called with `null` alias (clearing)

## Acceptance Criteria

- [ ] Inline `if`-blocks removed from `EditProfileViewModel.saveProfile()`
- [ ] `ValidateDisplayAliasUseCase` covers all branches in tests
- [ ] `EditProfileViewModelTest` uses a fake for `ValidateDisplayAliasUseCase`
- [ ] Error messages in ViewModel mapped from `AliasValidationResult` (not hardcoded strings in domain)
- [ ] `./gradlew composeApp:compileKotlinMetadata` passes
- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` passes
