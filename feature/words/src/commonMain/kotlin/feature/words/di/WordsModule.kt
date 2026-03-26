package feature.words.di

import domain.word.usecase.FilterAndSortWordsUseCase
import feature.words.TagManagerViewModel
import feature.words.VocabularyViewModel
import feature.words.WordManagerViewModel
import feature.words.WordTagAssignmentViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun wordsModule() = module {
    singleOf(::FilterAndSortWordsUseCase)
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
            getTagsUseCase = get(),
            deleteWordsUseCase = get(),
            batchUpdateLanguagesUseCase = get(),
            batchAssignTagsUseCase = get(),
            updateWordUseCase = get(),
            exportWordsUseCase = get(),
            getFeatureAccessUseCase = get(),
            filterAndSortWordsUseCase = get(),
            classifyImportErrorUseCase = get(),
            analyticsTracker = get(),
        )
    }
    viewModel {
        TagManagerViewModel(
            getTagsUseCase = get(),
            createTagUseCase = get(),
            renameTagUseCase = get(),
            deleteTagUseCase = get(),
            setSkipTagSelectorUseCase = get(),
            getSkipTagSelectorUseCase = get(),
        )
    }
    viewModel {
        WordTagAssignmentViewModel(
            getTagsUseCase = get(),
            assignWordTagsUseCase = get(),
        )
    }
}
