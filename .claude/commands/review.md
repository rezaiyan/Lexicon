---
description: Review staged/recent changes against project conventions
allowed-tools: ["Bash", "Read", "Glob", "Grep"]
---

Review the current changes (staged + unstaged) against Lexicon's architecture and coding conventions.

## Steps

1. Run `git diff` and `git diff --cached` to see all changes
2. Check each changed file against these rules:

### Architecture Boundaries
- Domain module must have NO platform dependencies (no Android, iOS, Ktor, Room imports)
- Data flows: View → ViewModel → UseCase → Repository → DataSource
- New components must be registered in `composeApp/src/commonMain/kotlin/di/AppModule.kt`

### Kotlin Code Style
- No `!!` (non-null assertion) — use safe calls, Elvis, or requireNotNull with justification
- No try-catch for control flow — use Flow `catch` operator or Result/sealed types
- No unnecessary `runCatching` wrappers
- Use Kotlin Flow (not LiveData) for reactive state

### Patterns
- Each business operation should be a standalone use case class
- Repository interfaces in `domain`, implementations in `data`
- Platform-specific code uses `expect`/`actual` in `platforms` module

3. Report findings as:
   - **Violations**: Must-fix issues that break conventions
   - **Warnings**: Style issues or potential improvements
   - **Good**: Notable well-written code worth highlighting
