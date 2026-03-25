package feature.words.di

import feature.words.TagManagerViewModel
import feature.words.VocabularyViewModel
import feature.words.WordManagerViewModel
import feature.words.WordTagAssignmentViewModel
import domain.tag.usecase.BatchAssignTagsUseCase
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
            getTagsUseCase = get(),
            deleteWordsUseCase = get(),
            batchUpdateLanguagesUseCase = get(),
            batchAssignTagsUseCase = get(),
            updateWordUseCase = get(),
            exportWordsUseCase = get(),
            getFeatureAccessUseCase = get(),
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
