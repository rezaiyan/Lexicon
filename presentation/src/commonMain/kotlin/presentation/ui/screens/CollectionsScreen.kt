@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import data.collection.remote.model.VocabularyCollection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.collection.CollectionsUiState
import presentation.feature.collection.VocabularyCollectionsViewModel
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.BasicAlertDialog
import presentation.ui.components.DialogProgressState
import presentation.ui.components.LexiconColumn
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.cancel
import vokab.resources.generated.resources.collections
import vokab.resources.generated.resources.collections_dismiss
import vokab.resources.generated.resources.collections_downloading
import vokab.resources.generated.resources.collections_empty
import vokab.resources.generated.resources.collections_empty_message
import vokab.resources.generated.resources.collections_error
import vokab.resources.generated.resources.collections_import
import vokab.resources.generated.resources.collections_import_confirm_proceed
import vokab.resources.generated.resources.collections_import_confirm_title
import vokab.resources.generated.resources.collections_import_confirm_word_count
import vokab.resources.generated.resources.collections_importing
import vokab.resources.generated.resources.retry

@Composable
fun CollectionsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = koinViewModel<VocabularyCollectionsViewModel>()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current

    // Show snackbar for errors
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Show snackbar for success messages
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    // Show confirmation dialog
    state.selectedCollection?.let { collection ->
        state.downloadInfo?.let { downloadInfo ->
            ImportConfirmationDialog(
                collection = collection,
                wordCount = downloadInfo.wordCount,
                onConfirm = { viewModel.confirmImport() },
                onDismiss = { viewModel.clearSelectedCollection() }
            )
        }
    }

    LexiconColumn(
        showNavigationIcon = true,
        title = stringResource(Res.string.collections),
        onNavigationClick = onNavigateBack,
        scrollable = false
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Theme.spacing.cardPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null && state.collections.isEmpty() -> {
                ErrorState(
                    error = state.error!!,
                    onRetry = { viewModel.retry() },
                    onDismiss = { viewModel.clearError() }
                )
            }

            state.collections.isEmpty() -> {
                EmptyState()
            }

            else -> {
                CollectionsContent(
                    state = state,
                    onImportClick = { collection ->
                        viewModel.selectCollection(collection)
                    }
                )
            }
        }

        // Download progress dialog
        if (state.isDownloading) {
            DownloadProgressDialog(
                progress = state.downloadProgress
            )
        }

        // Import progress dialog
        if (state.isImporting) {
            ImportProgressDialog(
                progress = state.importProgress
            )
        }
    }
}

@Composable
private fun CollectionsContent(
    state: CollectionsUiState,
    onImportClick: (VocabularyCollection) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)
    ) {
        // Group by target language, then by origin language
        state.groupedCollections.forEach { (targetLanguage, originLanguages) ->
            item {
                // Target language header (e.g., "Learn English")
                Text(
                    text = targetLanguage,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        top = Theme.spacing.cardPadding,
                        bottom = Theme.spacing.extraSmall2
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Show origin languages under each target language
            originLanguages.forEach { (_, collections) ->
                items(collections, key = { it.path }) { collection ->
                    CollectionCard(
                        collection = collection,
                        onClick = { onImportClick(collection) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(
    collection: VocabularyCollection,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSize),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(Theme.spacing.extraSmall))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${collection.targetLanguage} → ${collection.originLanguage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = stringResource(Res.string.collections_import),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardPadding)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.collections_empty),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.collections_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardPadding)
        ) {
            Text(
                text = stringResource(Res.string.collections_error),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardPadding)
            ) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(Res.string.retry))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.collections_dismiss))
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressDialog(progress: String) {
    BasicAlertDialog(
        onDismissRequest = { },
        title = stringResource(Res.string.collections_downloading),
        progressState = DialogProgressState.Circular,
        message = progress
    )
}

@Composable
private fun ImportProgressDialog(progress: String) {
    BasicAlertDialog(
        onDismissRequest = { },
        title = stringResource(Res.string.collections_importing),
        progressState = DialogProgressState.Circular,
        message = progress
    )
}

@Composable
private fun ImportConfirmationDialog(
    collection: VocabularyCollection,
    wordCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.collections_import_confirm_title),
        primaryButtonText = stringResource(Res.string.collections_import),
        primaryButtonOnClick = onConfirm,
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss,
        content = {
            Column(
                modifier = Modifier.padding(top = Theme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
            ) {
                Text(
                    text = stringResource(Res.string.collections_import_confirm_word_count, wordCount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.collections_import_confirm_proceed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
