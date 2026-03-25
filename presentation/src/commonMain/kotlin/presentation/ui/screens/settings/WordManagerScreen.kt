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
import overlay.LocalOverlayHost
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
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
import lexicon.resources.generated.resources.tagging_words_please_wait
import lexicon.resources.generated.resources.updating_words_please_wait
import lexicon.resources.generated.resources.word_deleted
import lexicon.resources.generated.resources.word_manager
import lexicon.resources.generated.resources.word_updated
import lexicon.resources.generated.resources.words_deleted
import lexicon.resources.generated.resources.words_language_updated
import lexicon.resources.generated.resources.words_tagged

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

private sealed interface WordDetailPage {
    data class Detail(val word: Word) : WordDetailPage
    data class Edit(val word: Word) : WordDetailPage
    data class DeleteConfirm(val word: Word) : WordDetailPage
    data class TagAssignment(val word: Word) : WordDetailPage
}

@Composable
internal fun WordManagerContent(
    onDismiss: () -> Unit
) {
    val viewModel = koinViewModel<WordManagerViewModel>()
    val state by viewModel.state()
    val snackbarHostState = LocalSnackbarHostState.current
    val overlayHost = LocalOverlayHost.current

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
    val wordsTaggedFormat = stringResource(Res.string.words_tagged)

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

                is WordManagerEffect.WordsTagged -> {
                    val pattern = "%1" + '$' + "d"
                    val message = wordsTaggedFormat.replace(pattern, event.count.toString())
                    snackbarHostState.showSnackbar(message)
                }

                is WordManagerEffect.Error -> {
                    val raw = event.message
                    val isNetwork = raw.contains("timeout", ignoreCase = true) ||
                        raw.contains("connect", ignoreCase = true) ||
                        raw.contains("network", ignoreCase = true) ||
                        raw.contains("internet", ignoreCase = true)

                    val errorMsg = when {
                        isNetwork -> "You're offline -- changes will sync when reconnected."
                        raw.isEmpty() -> failedToUpdateWord
                        else -> raw
                    }
                    snackbarHostState.showSnackbar("$errorPrefix $errorMsg")
                }
            }
        }
    }

    LexiconColumn(
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
                            overlayHost.showWordDetailSheet(
                                word = word,
                                onUpdateWord = viewModel::updateWord,
                                onDeleteWord = { w ->
                                    viewModel.toggleWordSelection(w.id)
                                    viewModel.deleteSelectedWords()
                                }
                            )
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
                        onFilterTagChange = viewModel::setFilterTagId,
                        onDeleteSelected = {
                            if (state.selectedCount > 0) {
                                overlayHost.showSizeToFitBottomSheet(
                                    tag = "delete-confirm"
                                ) { nav ->
                                    DeleteConfirmationContent(
                                        count = state.selectedWordIds.size,
                                        onConfirm = {
                                            viewModel.deleteSelectedWords()
                                            nav.dismiss()
                                        },
                                        onDismiss = {
                                            nav.dismiss()
                                            viewModel.exitSelectionMode()
                                        }
                                    )
                                }
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

                                overlayHost.showSizeToFitBottomSheet(
                                    tag = "batch-edit-languages"
                                ) { nav ->
                                    BatchEditLanguagesContent(
                                        count = selectedWords.size,
                                        initialSourceLanguage = mostCommonSource,
                                        initialTargetLanguage = mostCommonTarget,
                                        onConfirm = { source, target ->
                                            viewModel.batchUpdateLanguages(source, target)
                                            nav.dismiss()
                                        },
                                        onDismiss = { nav.dismiss() }
                                    )
                                }
                            }
                        },
                        onBatchAssignTags = {
                            if (state.selectedCount > 0) {
                                overlayHost.showSizeToFitBottomSheet(
                                    tag = "batch-assign-tags"
                                ) { nav ->
                                    BatchTagAssignmentContent(
                                        count = state.selectedCount,
                                        tags = state.tags,
                                        onConfirm = { tagIds ->
                                            viewModel.batchAssignTags(tagIds)
                                            nav.dismiss()
                                        },
                                        onDismiss = { nav.dismiss() }
                                    )
                                }
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

            if (state.isBatchAssigningTags) {
                ProgressOverlay(
                    message = stringResource(Res.string.tagging_words_please_wait)
                )
            }
        }
    }
}

private fun OverlayHost.showWordDetailSheet(
    word: Word,
    onUpdateWord: (Word) -> Unit,
    onDeleteWord: (Word) -> Unit
) {
    showSizeToFitBottomSheet(tag = "word-detail") { sheetNav ->
        val wordManagerViewModel = koinViewModel<WordManagerViewModel>()
        val liveState by wordManagerViewModel.state()

        val pages = rememberBottomSheetPageNavigator<WordDetailPage>(WordDetailPage.Detail(word))

        BottomSheetPages(navigator = pages, label = "wordDetailPages") { page ->
            when (page) {
                is WordDetailPage.Detail -> {
                    val liveWord = liveState.words.find { it.id == page.word.id } ?: page.word
                    val liveTags = liveState.tags
                    WordDetailSheetContent(
                        word = liveWord,
                        tags = liveTags,
                        onEdit = { w -> pages.navigateTo(WordDetailPage.Edit(w)) },
                        onDelete = { w -> pages.navigateTo(WordDetailPage.DeleteConfirm(w)) },
                        onAssignTags = { w -> pages.navigateTo(WordDetailPage.TagAssignment(w)) }
                    )
                }

                is WordDetailPage.Edit -> EditWordContent(
                    word = page.word,
                    onSave = { updatedWord ->
                        onUpdateWord(updatedWord)
                        sheetNav.dismiss()
                    },
                    onDismiss = { pages.navigateBack() }
                )

                is WordDetailPage.DeleteConfirm -> DeleteConfirmationContent(
                    count = 1,
                    onConfirm = {
                        onDeleteWord(page.word)
                        sheetNav.dismiss()
                    },
                    onDismiss = { pages.navigateBack() }
                )

                is WordDetailPage.TagAssignment -> TagAssignmentSheetContent(
                    word = page.word,
                    onDismiss = { pages.navigateBack() }
                )
            }
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
