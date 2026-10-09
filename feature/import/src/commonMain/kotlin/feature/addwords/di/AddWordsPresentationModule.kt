package feature.addwords.di

import feature.addwords.AddWordsViewModel
import feature.addwords.source.AiSuggestViewModel
import feature.addwords.source.FileImportViewModel
import feature.addwords.source.ManualEntryViewModel
import feature.addwords.source.PhotoImportViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** ViewModels of the add-words sheet. Resolve them against the sheet's own ViewModelStoreOwner. */
fun addWordsPresentationModule() = module {
    viewModelOf(::AddWordsViewModel)
    viewModelOf(::ManualEntryViewModel)
    viewModel { FileImportViewModel(get()) }
    viewModelOf(::PhotoImportViewModel)
    viewModelOf(::AiSuggestViewModel)
}
