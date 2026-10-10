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
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import components.scaffold.ActionIconConfig
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.deselect_all
import lexicon.resources.generated.resources.select_all
import lexicon.resources.generated.resources.selected_format
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import events.OnEvents
import expects.BackHandler
import presentation.ui.components.LanguageSelectionContent
import lexicon.resources.generated.resources.offline_changes_will_sync
import lexicon.resources.generated.resources.translation_language_label
import lexicon.resources.generated.resources.word_language
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.words.model.WordManagerEffect
import presentation.ui.LocalSnackbarHostState
import components.scaffold.TopBarColor
import components.scaffold.LexiconColumn
import domain.word.model.ImportErrorClassification
import domain.word.model.Word
import domain.word.usecase.ClassifyImportErrorUseCase
import overlay.LocalOverlayHost
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
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
import lexicon.resources.generated.resources.words_tab
import lexicon.resources.generated.resources.word_updated
import lexicon.resources.generated.resources.words_deleted
import lexicon.resources.generated.resources.words_language_updated
import lexicon.resources.generated.resources.words_tagged

private sealed interface WordDetailPage {
    data object Detail : WordDetailPage
    data object Edit : WordDetailPage
    data object PickLearningLanguage : WordDetailPage
    data object PickNativeLanguage : WordDetailPage
    data object DeleteConfirm : WordDetailPage
    data object TagAssignment : WordDetailPage
}

@Composable
fun WordManagerScreen() {
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
    val offlineChangesSync = stringResource(Res.string.offline_changes_will_sync)
    val classifyError = remember { ClassifyImportErrorUseCase() }

    OnEvents(viewModel.effects) { event ->
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
                    val errorMsg = when (classifyError(raw)) {
                        ImportErrorClassification.NetworkError -> offlineChangesSync
                        else -> raw.ifEmpty { failedToUpdateWord }
                    }
                    snackbarHostState.showSnackbar("$errorPrefix $errorMsg")
                }
            }
    }

    BackHandler(enabled = state.isSelectionMode, onBack = viewModel::exitSelectionMode)

    val allSelected = state.filteredWords.isNotEmpty() && state.selectedCount == state.filteredWords.size
    LexiconColumn(
        title = if (state.isSelectionMode) {
            stringResource(Res.string.selected_format, state.selectedCount)
        } else {
            stringResource(Res.string.words_tab)
        },
        showNavigationIcon = state.isSelectionMode,
        navigationIcon = Icons.Default.Close,
        navigationIconContentDescription = stringResource(Res.string.cancel),
        onNavigationClick = viewModel::exitSelectionMode,
        actionIcon1 = if (state.isSelectionMode) {
            ActionIconConfig(
                icon = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                contentDescription = stringResource(
                    if (allSelected) Res.string.deselect_all else Res.string.select_all
                ),
                onClick = viewModel::selectAll,
            )
        } else {
            null
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
                    error != null -> ErrorView(message = error, classification = state.errorClassification)
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
                                onDeleteWord = { w -> viewModel.deleteWord(w.id) }
                            )
                        },
                        onDragSelectStart = viewModel::startDragSelection,
                        onDragSelectTo = viewModel::dragSelectionTo,
                        onDragSelectEnd = viewModel::endDragSelection,
                        onShareWords = viewModel::shareWords,
                        onSortOptionChange = viewModel::setSortOption,
                        onFilterLanguageChange = viewModel::setFilterLanguage,
                        onFilterLearningStageChange = viewModel::setFilterLearningStage,
                        onFilterTagChange = viewModel::setFilterTagId,
                        onClearFilters = viewModel::clearFilters,
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
                                        onDismiss = { nav.dismiss() },
                                        onClose = { nav.dismiss() },
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
                                        onClose = { nav.dismiss() },
                                    )
                                }
                            }
                        },
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
        // Reads the word from the live list so the detail page reflects a save straight away.
        val liveWord = liveState.words.find { it.id == word.id } ?: word
        // Edit draft sits above the pages so it survives the language picker pages.
        var draft by remember { mutableStateOf(liveWord) }

        val pages = rememberBottomSheetPageNavigator<WordDetailPage>(WordDetailPage.Detail)

        BottomSheetPages(navigator = pages, onClose = { sheetNav.dismiss() }, label = "wordDetailPages") { page ->
            when (page) {
                WordDetailPage.Detail -> WordDetailSheetContent(
                    word = liveWord,
                    tags = liveState.tags,
                    onEdit = {
                        draft = liveWord
                        pages.navigateTo(WordDetailPage.Edit)
                    },
                    onDelete = { pages.navigateTo(WordDetailPage.DeleteConfirm) },
                    onAssignTags = { pages.navigateTo(WordDetailPage.TagAssignment) }
                )

                WordDetailPage.Edit -> EditWordContent(
                    original = liveWord,
                    draft = draft,
                    onDraftChange = { draft = it },
                    onPickLearningLanguage = { pages.navigateTo(WordDetailPage.PickLearningLanguage) },
                    onPickNativeLanguage = { pages.navigateTo(WordDetailPage.PickNativeLanguage) },
                    onSave = { updatedWord ->
                        onUpdateWord(updatedWord)
                        pages.navigateBack()
                    },
                    onDismiss = { pages.navigateBack() }
                )

                WordDetailPage.PickLearningLanguage -> DraftLanguagePicker(
                    current = draft.targetLanguage,
                    title = stringResource(Res.string.word_language),
                    onPicked = { draft = draft.copy(targetLanguage = it) },
                    onDone = { pages.navigateBack() },
                )

                WordDetailPage.PickNativeLanguage -> DraftLanguagePicker(
                    current = draft.sourceLanguage,
                    title = stringResource(Res.string.translation_language_label),
                    onPicked = { draft = draft.copy(sourceLanguage = it) },
                    onDone = { pages.navigateBack() },
                )

                WordDetailPage.DeleteConfirm -> DeleteConfirmationContent(
                    count = 1,
                    onConfirm = {
                        onDeleteWord(liveWord)
                        sheetNav.dismiss()
                    },
                    onDismiss = { pages.navigateBack() }
                )

                WordDetailPage.TagAssignment -> TagAssignmentSheetContent(
                    word = liveWord,
                    onDismiss = { pages.navigateBack() }
                )
            }
        }
    }
}

@Composable
private fun DraftLanguagePicker(
    current: Language,
    title: String,
    onPicked: (Language) -> Unit,
    onDone: () -> Unit,
) {
    LanguageSelectionContent(
        currentLanguage = current,
        onLanguageSelected = {
            onPicked(it)
            onDone()
        },
        title = title,
    )
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
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)
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
