package feature.aiimport.di

import feature.aiimport.AiWordImportViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun importModule() = module {
    viewModel {
        AiWordImportViewModel(
            submitPreferencesUseCase = get(),
            importSuggestedVocabularyUseCase = get(),
            analyticsTracker = get()
        )
    }
}
