---
description: Scaffold a new feature following Clean Architecture
argument-hint: "<feature-name>"
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
---

Scaffold the files for a new feature named "$ARGUMENTS" following Lexicon's Clean Architecture pattern.

## Instructions

1. **Enter plan mode first** — analyze where the feature fits in the existing architecture
2. Determine what layers are needed (all features need at minimum a use case):
   - **Domain**: Use case class(es) and any new domain models
   - **Data**: Repository interface (domain) + implementation (data), data sources if needed
   - **Presentation**: ViewModel, Screen composable, UI state class
   - **DI**: Koin registration in AppModule.kt

3. Follow existing patterns:
   - Look at a similar existing feature for reference (use Glob/Grep to find examples)
   - Use cases extend or follow the pattern in `domain/usecase/`
   - ViewModels follow the intent-based pattern where appropriate
   - Repository interfaces go in `domain/repository/`
   - Repository implementations go in `data/repository/`

4. Create the scaffolded files with TODO comments for business logic
5. Register all new components in AppModule.kt
6. Suggest what tests should be written

**Important**: Always ask for confirmation before creating files. Present the plan first.
