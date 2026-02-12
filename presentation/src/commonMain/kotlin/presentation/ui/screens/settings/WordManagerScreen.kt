@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalTime::class
)

package presentation.ui.screens.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import domain.word.model.Word
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.model.WordManagerEvent
import presentation.model.WordManagerScreenState
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.ActionIconConfig
import presentation.ui.components.BasicAlertDialog
import presentation.ui.components.ButtonType
import presentation.ui.components.TopBarColor
import presentation.ui.components.LexiconColumn
import presentation.util.shareContentAsFile
import presentation.viewmodel.WordManagerViewModel
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.cancel
import vokab.resources.generated.resources.delete
import vokab.resources.generated.resources.delete_words_message
import vokab.resources.generated.resources.delete_words_title
import vokab.resources.generated.resources.deleting
import vokab.resources.generated.resources.deleting_words
import vokab.resources.generated.resources.deleting_words_please_wait
import vokab.resources.generated.resources.description_optional
import vokab.resources.generated.resources.deselect_all
import vokab.resources.generated.resources.edit
import vokab.resources.generated.resources.edit_word
import vokab.resources.generated.resources.empty_library
import vokab.resources.generated.resources.error
import vokab.resources.generated.resources.error_prefix
import vokab.resources.generated.resources.failed_to_update_word
import vokab.resources.generated.resources.loading_words
import vokab.resources.generated.resources.no_results_found
import vokab.resources.generated.resources.no_words_selected
import vokab.resources.generated.resources.no_words_to_share
import vokab.resources.generated.resources.original_word
import vokab.resources.generated.resources.search_words
import vokab.resources.generated.resources.please_wait
import vokab.resources.generated.resources.save
import vokab.resources.generated.resources.select_all
import vokab.resources.generated.resources.share
import vokab.resources.generated.resources.share_title_format
import vokab.resources.generated.resources.start_by_importing
import vokab.resources.generated.resources.translation_label
import vokab.resources.generated.resources.word_deleted
import vokab.resources.generated.resources.word_manager
import vokab.resources.generated.resources.word_updated
import vokab.resources.generated.resources.words_deleted
import kotlin.time.ExperimentalTime

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
    val failedToUpdateWord = stringResource(Res.string.failed_to_update_word)
    val errorPrefix = stringResource(Res.string.error_prefix)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WordManagerEvent.WordsShared -> {
                    val pattern = "%1" + '$' + "d"
                    val title = shareTitleFormat.replace(pattern, event.count.toString())
                    val filename = "vokab_words_${event.count}_${event.timestamp}.txt"
                    shareContentAsFile(title, event.text, filename)
                }

                is WordManagerEvent.ShareFailed -> {
                    snackbarHostState.showSnackbar(noWordsToShare)
                }

                is WordManagerEvent.WordDeleted -> {
                    val message = if (event.count == 1) {
                        wordDeleted
                    } else {
                        val pattern = "%1" + '$' + "d"
                        wordsDeletedFormat.replace(pattern, event.count.toString())
                    }
                    snackbarHostState.showSnackbar(message)
                }

                is WordManagerEvent.WordUpdated -> {
                    snackbarHostState.showSnackbar(wordUpdated)
                }

                is WordManagerEvent.Error -> {
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
}

@Composable
private fun WordListContent(
    state: WordManagerScreenState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEdit: (Word) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onShareWords: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            searchQuery = state.searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onClearSearch = onClearSearch
        )

        // Words list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
        ) {
            // Sticky selection bar header
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        SelectionBar(
                            selectedCount = state.selectedCount,
                            totalCount = state.filteredWords.size,
                            isUserSubscribed = state.isUserSubscribed,
                            onSelectAll = onSelectAll,
                            onDeselectAll = onDeselectAll,
                            onShareWords = onShareWords
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Show filtered results count if searching
            if (state.searchQuery.isNotBlank() && state.filteredWords.size < state.words.size) {
                item {
                    Text(
                        text = "${state.filteredWords.size} of ${state.words.size} words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = Theme.spacing.cardPadding,
                            vertical = Theme.spacing.extraSmall2
                        )
                    )
                }
            }

            // Word items with animation
            items(state.filteredWords, key = { it.id }) { word ->
                WordCard(
                    word = word,
                    isSelected = state.selectedWordIds.contains(word.id),
                    onToggleSelection = { onToggleSelection(word.id) },
                    onEdit = { onEdit(word) },
                    modifier = Modifier.animateItem()
                )
            }

            // Empty search results
            if (state.searchQuery.isNotBlank() && state.filteredWords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Theme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(Res.string.no_results_found),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Theme.spacing.cardPadding,
                    vertical = Theme.spacing.extraSmall
                ),
            placeholder = {
                Text(
                    text = stringResource(Res.string.search_words),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(Res.string.search_words),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(Res.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    totalCount: Int,
    isUserSubscribed: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onShareWords: () -> Unit
) {
    val hasSelection = selectedCount > 0
    val allSelected = selectedCount == totalCount && totalCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Theme.spacing.cardPadding,
                vertical = Theme.spacing.extraSmall
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Count info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
        ) {
            Icon(
                imageVector = if (allSelected) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                contentDescription = null,
                tint = if (hasSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
            )
            Text(
                text = if (hasSelection) {
                    "$selectedCount / $totalCount selected"
                } else {
                    "$totalCount items"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (hasSelection) FontWeight.Bold else FontWeight.Medium,
                color = if (hasSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        // Right side: Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share button (subscribed users only)
            if (isUserSubscribed) {
                IconButton(
                    onClick = onShareWords,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = stringResource(Res.string.share),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Select/Deselect All button
            if (totalCount > 0) {
                TextButton(
                    onClick = {
                        if (allSelected) onDeselectAll() else onSelectAll()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (hasSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = if (allSelected) {
                            stringResource(Res.string.deselect_all)
                        } else {
                            stringResource(Res.string.select_all)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatDateAdded(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val timeZone = TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(timeZone)

    return "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}/${localDateTime.year}"
}

@Composable
private fun WordCard(
    word: Word,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surface

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        onClick = onToggleSelection
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Checkbox + Word content
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)
            ) {
                // Checkbox
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null, // Handled by card click
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Word content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3)
                ) {
                    Text(
                        text = word.originalWord,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (word.description.isNotBlank()) {
                        Text(
                            text = word.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (word.dateAdded > 0L) {
                        Text(
                            text = formatDateAdded(word.dateAdded),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right side: Edit button
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(Theme.dimensions.iconSizeHuge)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Theme.dimensions.iconSize)
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacingLarge)
        ) {
            CircularProgressIndicator()
            Text(
                stringResource(Res.string.loading_words),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacingLarge)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                stringResource(Res.string.error),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyLibraryView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacingLarge)
        ) {
            Text(
                "📚",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                stringResource(Res.string.empty_library),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(Res.string.start_by_importing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EditWordDialog(
    word: Word,
    onDismiss: () -> Unit,
    onSave: (Word) -> Unit
) {
    var originalWord by remember { mutableStateOf(word.originalWord) }
    var translation by remember { mutableStateOf(word.translation) }
    var description by remember { mutableStateOf(word.description) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.Edit,
        title = stringResource(Res.string.edit_word),
        primaryButtonText = stringResource(Res.string.save),
        primaryButtonOnClick = {
            if (originalWord.isNotBlank() && translation.isNotBlank()) {
                val updatedWord = word.copy(
                    originalWord = originalWord.trim(),
                    translation = translation.trim(),
                    description = description.trim()
                )
                onSave(updatedWord)
            }
        },
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss,
        content = {
            Column(
                modifier = Modifier.padding(top = Theme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardPadding)
            ) {
                OutlinedTextField(
                    value = originalWord,
                    onValueChange = { originalWord = it },
                    label = { Text(stringResource(Res.string.original_word)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text(stringResource(Res.string.translation_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    isDeleting: Boolean,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        icon = if (isDeleting) null else Icons.Default.Warning,
        iconTint = if (isDeleting) null else MaterialTheme.colorScheme.error,
        title = if (isDeleting)
            stringResource(Res.string.deleting_words)
        else
            stringResource(Res.string.delete_words_title),
        primaryButtonText = if (isDeleting)
            stringResource(Res.string.deleting)
        else
            stringResource(Res.string.delete),
        primaryButtonOnClick = onConfirm,
        primaryButtonType = ButtonType.Error,
        secondaryButtonText = if (isDeleting) null else stringResource(Res.string.cancel),
        secondaryButtonOnClick = if (isDeleting) null else onDismiss,
        content = {
            Column(
                modifier = Modifier.padding(top = Theme.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.small))
                    Text(
                        "deleting_words_message, count",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
                        Text(
                            stringResource(Res.string.please_wait),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        stringResource(Res.string.delete_words_message, count),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}
