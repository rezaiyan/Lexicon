package presentation.ui.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.model.WordManagerEffect
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.ActionIconConfig
import presentation.ui.components.TopBarColor
import presentation.ui.components.LexiconColumn
import presentation.util.shareContentAsFile
import presentation.viewmodel.WordManagerViewModel
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.batch_edit_languages
import lexicon.resources.generated.resources.deleting_words_please_wait
import lexicon.resources.generated.resources.error_prefix
import lexicon.resources.generated.resources.failed_to_update_word
import lexicon.resources.generated.resources.no_words_selected
import lexicon.resources.generated.resources.no_words_to_share
import lexicon.resources.generated.resources.share_title_format
import lexicon.resources.generated.resources.updating_words_please_wait
import lexicon.resources.generated.resources.word_deleted
import lexicon.resources.generated.resources.word_manager
import lexicon.resources.generated.resources.word_updated
import lexicon.resources.generated.resources.words_deleted
import lexicon.resources.generated.resources.words_language_updated

@Composable
fun WordManagerScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = koinViewModel<WordManagerViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val noWordsSelectedMessage = stringResource(Res.string.no_words_selected)
    val coroutineScope = rememberCoroutineScope()

    // Reset state when screen opens
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    // Handle Word Manager events (file sharing)
    val shareTitleFormat = stringResource(Res.string.share_title_format)
    val noWordsToShare = stringResource(Res.string.no_words_to_share)
    val wordDeleted = stringResource(Res.string.word_deleted)
    val wordsDeletedFormat = stringResource(Res.string.words_deleted)
    val wordUpdated = stringResource(Res.string.word_updated)
    val wordsLanguageUpdatedFormat = stringResource(Res.string.words_language_updated)
    val failedToUpdateWord = stringResource(Res.string.failed_to_update_word)
    val errorPrefix = stringResource(Res.string.error_prefix)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
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
        onNavigationClick = onNavigateBack,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.delete),
            onClick = {
                if (state.selectedCount > 0) {
                    viewModel.showDeleteConfirmation()
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(noWordsSelectedMessage)
                    }
                }
            },
            tint = MaterialTheme.colorScheme.error,
            size = 28.dp
        ),
        actionIcon2 = if (state.selectedCount > 0) ActionIconConfig(
            icon = Icons.Default.Language,
            contentDescription = stringResource(Res.string.batch_edit_languages),
            onClick = { viewModel.showBatchEditLanguages() },
            tint = MaterialTheme.colorScheme.primary,
            size = 28.dp
        ) else null,
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
                        onEdit = viewModel::startEditingWord,
                        onSelectAll = viewModel::selectAll,
                        onDeselectAll = viewModel::deselectAll,
                        onShareWords = viewModel::shareWords
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
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = stringResource(Res.string.updating_words_please_wait),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Deletion overlay - prevents interaction and hides list updates
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
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
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
    // Edit Word Dialog
    if (state.editingWord != null) {
        EditWordDialog(
            word = state.editingWord!!,
            onDismiss = viewModel::cancelEditing,
            onSave = viewModel::updateWord
        )
    }

    // Delete Confirmation Dialog
    if (state.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            isDeleting = state.isDeletingWords,
            count = state.selectedWordIds.size,
            onConfirm = viewModel::deleteSelectedWords,
            onDismiss = viewModel::hideDeleteConfirmation
        )
    }

    // Batch Edit Languages Dialog
    if (state.showBatchEditLanguages) {
        val selectedWords = state.words.filter { state.selectedWordIds.contains(it.id) }
        val mostCommonSource = selectedWords
            .groupingBy { it.sourceLanguage }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: utils.Language.ENGLISH
        val mostCommonTarget = selectedWords
            .groupingBy { it.targetLanguage }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: utils.Language.ENGLISH

        BatchEditLanguagesDialog(
            isUpdating = state.isBatchUpdatingLanguages,
            count = state.selectedWordIds.size,
            initialSourceLanguage = mostCommonSource,
            initialTargetLanguage = mostCommonTarget,
            onConfirm = { source, target ->
                viewModel.batchUpdateLanguages(source, target)
            },
            onDismiss = viewModel::hideBatchEditLanguages
        )
    }
}
