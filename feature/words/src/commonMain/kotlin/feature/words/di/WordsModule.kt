package feature.words.di

import feature.words.VocabularyViewModel
import feature.words.WordManagerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun wordsModule() = module {
    viewModel {
        VocabularyViewModel(
            getDueWordsUseCase = get(),
            getWordsByStageUseCase = get(),
            updateWordUseCase = get(),
            deleteWordUseCase = get(),
            analyticsTracker = get(),
        )
    }
    viewModel {
        WordManagerViewModel(
            getAllWordsUseCase = get(),
            deleteWordsUseCase = get(),
            batchUpdateLanguagesUseCase = get(),
            updateWordUseCase = get(),
            exportWordsUseCase = get(),
            getFeatureAccessUseCase = get(),
            analyticsTracker = get(),
        )
    }
}
