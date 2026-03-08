package presentation.ui.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.words.model.WordManagerEffect
import presentation.ui.LocalSnackbarHostState
import components.scaffold.TopBarColor
import components.scaffold.LexiconColumn
import domain.word.model.Word
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.fullscreen.FullScreenProperties
import overlay.fullscreen.showFullScreen
import presentation.util.shareContentAsFile
import feature.words.WordManagerViewModel
import theme.Theme
import utils.Language
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.deleting_words_please_wait
import lexicon.resources.generated.resources.error_prefix
import lexicon.resources.generated.resources.failed_to_update_word
import lexicon.resources.generated.resources.no_words_to_share
import lexicon.resources.generated.resources.share_title_format
import lexicon.resources.generated.resources.updating_words_please_wait
import lexicon.resources.generated.resources.word_deleted
import lexicon.resources.generated.resources.word_manager
import lexicon.resources.generated.resources.word_updated
import lexicon.resources.generated.resources.words_deleted
import lexicon.resources.generated.resources.words_language_updated

fun OverlayHost.showWordManagerSheet() {
    showFullScreen(
        tag = "word-manager",
        properties = FullScreenProperties(
            dismissOnBackPress = false,
        )
    ) { nav ->
        WordManagerContent(onDismiss = { nav.dismiss() })
    }
}

private sealed interface WordManagerPage {
    data object List : WordManagerPage
    data class Detail(val word: Word) : WordManagerPage
    data class Edit(val word: Word) : WordManagerPage
    data class DeleteConfirm(val count: Int) : WordManagerPage
    data class BatchEditLanguages(
        val count: Int,
        val initialSourceLanguage: Language,
        val initialTargetLanguage: Language
    ) : WordManagerPage
}

@Composable
internal fun WordManagerContent(
    onDismiss: () -> Unit
) {
    val viewModel = koinViewModel<WordManagerViewModel>()
    val state by viewModel.state()
    val snackbarHostState = LocalSnackbarHostState.current
    val pages = rememberBottomSheetPageNavigator<WordManagerPage>(WordManagerPage.List)

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    // Handle Word Manager effects
    val shareTitleFormat = stringResource(Res.string.share_title_format)
    val noWordsToShare = stringResource(Res.string.no_words_to_share)
    val wordDeleted = stringResource(Res.string.word_deleted)
    val wordsDeletedFormat = stringResource(Res.string.words_deleted)
    val wordUpdated = stringResource(Res.string.word_updated)
    val wordsLanguageUpdatedFormat = stringResource(Res.string.words_language_updated)
    val failedToUpdateWord = stringResource(Res.string.failed_to_update_word)
    val errorPrefix = stringResource(Res.string.error_prefix)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { event ->
            when (event) {
                is WordManagerEffect.WordsShared -> {
                    val pattern = "%1" + '$' + "d"
                    val title = shareTitleFormat.replace(pattern, event.count.toString())
                    val filename = "vokab_words_${event.count}_${event.timestamp}.txt"
                    shareContentAsFile(title, event.text, filename)
                }

                is WordManagerEffect.ShareFailed -> {
                    snackbarHostState.showSnackbar(noWordsToShare)
                }

                is WordManagerEffect.WordDeleted -> {
                    val message = if (event.count == 1) {
                        wordDeleted
                    } else {
                        val pattern = "%1" + '$' + "d"
                        wordsDeletedFormat.replace(pattern, event.count.toString())
                    }
                    snackbarHostState.showSnackbar(message)
                }

                is WordManagerEffect.WordUpdated -> {
                    snackbarHostState.showSnackbar(wordUpdated)
                }

                is WordManagerEffect.WordsLanguageUpdated -> {
                    val pattern = "%1" + '$' + "d"
                    val message = wordsLanguageUpdatedFormat.replace(
                        pattern, event.count.toString()
                    )
                    snackbarHostState.showSnackbar(message)
                }

                is WordManagerEffect.Error -> {
                    val errorMsg = event.message.ifEmpty {
                        failedToUpdateWord
                    }
                    snackbarHostState.showSnackbar("$errorPrefix $errorMsg")
                }
            }
        }
    }

    BottomSheetPages(navigator = pages, label = "wordManagerPages") { page ->
        when (page) {
            WordManagerPage.List -> LexiconColumn(
                title = stringResource(Res.string.word_manager),
                showNavigationIcon = true,
                navigationIcon = Icons.Default.Close,
                onNavigationClick = {
                    if (state.isSelectionMode) {
                        viewModel.exitSelectionMode()
                    } else {
                        onDismiss()
                    }
                },
                scrollable = false,
                topBarColor = TopBarColor.Background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = Triple(
                            state.isLoading,
                            state.errorMessage,
                            state.words.isEmpty()
                        ),
                        label = "contentCrossfade"
                    ) { (loading, error, empty) ->
                        when {
                            loading -> LoadingView()
                            error != null -> ErrorView(message = error)
                            empty -> EmptyLibraryView()
                            else -> WordListContent(
                                state = state,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onClearSearch = viewModel::clearSearch,
                                onToggleSelection = viewModel::toggleWordSelection,
                                onOpenDetail = { word ->
                                    pages.navigateTo(WordManagerPage.Detail(word))
                                },
                                onEnterSelectionMode = { wordId ->
                                    viewModel.enterSelectionMode()
                                    viewModel.toggleWordSelection(wordId)
                                },
                                onSelectAll = viewModel::selectAll,
                                onShareWords = viewModel::shareWords,
                                onSortOptionChange = viewModel::setSortOption,
                                onFilterLanguageChange = viewModel::setFilterLanguage,
                                onFilterLearningStageChange = viewModel::setFilterLearningStage,
                                onDeleteSelected = {
                                    if (state.selectedCount > 0) {
                                        pages.navigateTo(
                                            WordManagerPage.DeleteConfirm(
                                                count = state.selectedWordIds.size
                                            )
                                        )
                                    }
                                },
                                onBatchEditLanguages = {
                                    if (state.selectedCount > 0) {
                                        val selectedWords = state.words.filter {
                                            state.selectedWordIds.contains(it.id)
                                        }
                                        val mostCommonSource = selectedWords
                                            .groupingBy { it.sourceLanguage }
                                            .eachCount()
                                            .maxByOrNull { it.value }?.key
                                            ?: Language.ENGLISH
                                        val mostCommonTarget = selectedWords
                                            .groupingBy { it.targetLanguage }
                                            .eachCount()
                                            .maxByOrNull { it.value }?.key
                                            ?: Language.ENGLISH

                                        pages.navigateTo(
                                            WordManagerPage.BatchEditLanguages(
                                                count = selectedWords.size,
                                                initialSourceLanguage = mostCommonSource,
                                                initialTargetLanguage = mostCommonTarget
                                            )
                                        )
                                    }
                                },
                                onExitSelectionMode = viewModel::exitSelectionMode
                            )
                        }
                    }

                    if (state.isBatchUpdatingLanguages) {
                        ProgressOverlay(
                            message = stringResource(Res.string.updating_words_please_wait)
                        )
                    }

                    if (state.isDeletingWords) {
                        ProgressOverlay(
                            message = stringResource(Res.string.deleting_words_please_wait)
                        )
                    }
                }
            }

            is WordManagerPage.Detail -> WordDetailSheetContent(
                word = page.word,
                onEdit = { word -> pages.navigateTo(WordManagerPage.Edit(word)) },
                onDelete = { word ->
                    viewModel.toggleWordSelection(word.id)
                    pages.navigateTo(WordManagerPage.DeleteConfirm(count = 1))
                }
            )

            is WordManagerPage.Edit -> EditWordContent(
                word = page.word,
                onSave = { updatedWord ->
                    viewModel.updateWord(updatedWord)
                    pages.navigateBack()
                    pages.navigateBack()
                },
                onDismiss = { pages.navigateBack() }
            )

            is WordManagerPage.DeleteConfirm -> DeleteConfirmationContent(
                count = page.count,
                onConfirm = {
                    viewModel.deleteSelectedWords()
                    while (pages.canNavigateBack) pages.navigateBack()
                },
                onDismiss = { pages.navigateBack() }
            )

            is WordManagerPage.BatchEditLanguages -> BatchEditLanguagesContent(
                count = page.count,
                initialSourceLanguage = page.initialSourceLanguage,
                initialTargetLanguage = page.initialTargetLanguage,
                onConfirm = { source, target ->
                    viewModel.batchUpdateLanguages(source, target)
                    pages.navigateBack()
                },
                onDismiss = { pages.navigateBack() }
            )
        }
    }
}

@Composable
private fun ProgressOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacingLarge)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(Theme.dimensions.touchTarget),
                strokeWidth = Theme.dimensions.borderWidthThick
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
