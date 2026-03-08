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
import components.scaffold.ActionIconConfig
import components.scaffold.TopBarColor
import components.scaffold.LexiconColumn
import overlay.LocalOverlayHost
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.showFullscreenBottomSheet
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.util.shareContentAsFile
import feature.words.WordManagerViewModel
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
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

private val WordManagerSheetProperties = BottomSheetProperties(
    dismissOnTouchOutside = false,
    dismissOnBackPress = false,
    sheetGesturesEnabled = false
)

fun OverlayHost.showWordManagerSheet() {
    showFullscreenBottomSheet(
        tag = "word-manager",
        properties = WordManagerSheetProperties
    ) { nav ->
        WordManagerContent(onDismiss = { nav.dismiss() })
    }
}

@Composable
internal fun WordManagerContent(
    onDismiss: () -> Unit
) {
    val viewModel = koinViewModel<WordManagerViewModel>()
    val state by viewModel.state()
    val snackbarHostState = LocalSnackbarHostState.current
    val overlayHost = LocalOverlayHost.current

    // Reset state when screen opens
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    // Handle Word Manager events
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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Crossfade(
                targetState = Triple(state.isLoading, state.errorMessage, state.words.isEmpty()),
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
                                onEdit = { w ->
                                    overlayHost.showSizeToFitBottomSheet(tag = "edit-word") { nav ->
                                        EditWordContent(
                                            word = w,
                                            onDismiss = { nav.dismiss() },
                                            onSave = { updatedWord ->
                                                viewModel.updateWord(updatedWord)
                                                nav.dismiss()
                                            }
                                        )
                                    }
                                },
                                onDelete = { w ->
                                    viewModel.toggleWordSelection(w.id)
                                    overlayHost.showSizeToFitBottomSheet(tag = "delete-confirmation") { nav ->
                                        DeleteConfirmationContent(
                                            count = 1,
                                            onConfirm = {
                                                viewModel.deleteSelectedWords()
                                                nav.dismiss()
                                            },
                                            onDismiss = { nav.dismiss() }
                                        )
                                    }
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
                        onDeleteSelected = {
                            if (state.selectedCount > 0) {
                                overlayHost.showSizeToFitBottomSheet(tag = "delete-confirmation") { nav ->
                                    DeleteConfirmationContent(
                                        count = state.selectedWordIds.size,
                                        onConfirm = {
                                            viewModel.deleteSelectedWords()
                                            nav.dismiss()
                                        },
                                        onDismiss = { nav.dismiss() }
                                    )
                                }
                            }
                        },
                        onBatchEditLanguages = {
                            if (state.selectedCount > 0) {
                                val selectedWords = state.words.filter { state.selectedWordIds.contains(it.id) }
                                val mostCommonSource = selectedWords
                                    .groupingBy { it.sourceLanguage }
                                    .eachCount()
                                    .maxByOrNull { it.value }?.key ?: utils.Language.ENGLISH
                                val mostCommonTarget = selectedWords
                                    .groupingBy { it.targetLanguage }
                                    .eachCount()
                                    .maxByOrNull { it.value }?.key ?: utils.Language.ENGLISH

                                overlayHost.showSizeToFitBottomSheet(tag = "batch-edit-languages") { nav ->
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
                        onExitSelectionMode = viewModel::exitSelectionMode
                    )
                }
            }

            // Batch language update overlay
            if (state.isBatchUpdatingLanguages) {
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
                            text = stringResource(Res.string.updating_words_please_wait),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Deletion overlay
            if (state.isDeletingWords) {
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
                            text = stringResource(Res.string.deleting_words_please_wait),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

}
