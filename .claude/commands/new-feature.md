---
description: Scaffold a new feature following Clean Architecture with BaseViewModel, UseCase<P,R>, and Try<T> contracts
argument-hint: "<feature-name>"
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep", "Bash", "Agent"]
---

Scaffold the files for a new feature named "$ARGUMENTS" following Lexicon's target architecture patterns.

## Instructions

1. **Enter plan mode first** — analyze where the feature fits in the existing architecture
2. Determine what layers are needed (all features need at minimum a use case):

   - **Domain**: Use case implementing `UseCase<P, R>` or `FlowUseCase<P, R>`, domain models, repository interface (suspend -> `Try<T>`, stream -> `Flow<T>`)
   - **Data**: Repository implementation, data source interface (in domain) + implementation (in data), extension function mappers (`Dto.toDomain()`)
   - **Presentation**: ViewModel extending `BaseViewModel<State, Effect>` with event sink pattern, Screen composable using `viewModel.state()` + `OnEvents`
   - **DI**: Koin registration in AppModule.kt

3. Follow target patterns (not legacy):
   - Look at recently migrated features for reference, or use skills directly
   - ViewModel: `BaseViewModel<State, Effect>`, single data class state, event sink methods
   - Use cases: `UseCase<P, R>` returning `Try<T>`, or `FlowUseCase<P, R>` returning `Flow<T>`
   - Repository: interface in `domain/`, impl in `data/`, suspend -> `Try<T>`
   - Data source: interface in `domain/`, impl in `data/`
   - Screen: `viewModel.state()`, `OnEvents(viewModel.effects)`, `LexiconColumn`, content composable with data + lambdas

4. Create the scaffolded files with TODO comments for business logic
5. Register all new components in AppModule.kt:
   ```kotlin
   singleOf(::FeatureRemoteDataSourceImpl) { bind<IFeatureRemoteDataSource>() }
   singleOf(::FeatureRepositoryImpl) { bind<IFeatureRepository>() }
   factoryOf(::GetFeatureUseCase)
   viewModelOf(::FeatureViewModel)
   ```
6. Delegate to `test-writer` agent for tests (ViewModel + UseCase at minimum)

**Important**: Always ask for confirmation before creating files. Present the plan first.
