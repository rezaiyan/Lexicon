package presentation.feature.collection

import data.collection.remote.CollectionRemoteDataSource
import data.collection.remote.model.VocabularyCollection
import domain.word.usecase.ImportVocabularyCollectionUseCase
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Handler for downloading and importing vocabulary collections
 * Manages collection download, parsing, and import process
 */
internal class CollectionImportHandler(
    private val collectionRemoteDataSource: CollectionRemoteDataSource,
    private val importVocabularyCollectionUseCase: ImportVocabularyCollectionUseCase,
    private val importWordsUseCase: ImportWordsUseCase,
    private val state: MutableStateFlow<CollectionsUiState>,
    private val scope: CoroutineScope
) {
    
    fun selectCollection(collection: VocabularyCollection) {
        scope.launch {
            state.value = state.value.copy(isDownloading = true, downloadProgress = "Downloading...")
            
            val downloadResult = collectionRemoteDataSource.downloadCollection(
                collection.targetLanguage,
                collection.originLanguage,
                collection.fileName
            )
            
            state.value = state.value.copy(isDownloading = false)
            
            if (downloadResult.isFailure) {
                state.value = state.value.copy(
                    error = downloadResult.exceptionOrNull()?.message ?: "Failed to download collection"
                )
                return@launch
            }
            
            val content = downloadResult.getOrNull()!!.content
            
            state.value = state.value.copy(downloadProgress = "Parsing...")
            val parseResult = importVocabularyCollectionUseCase.parseCollection(
                content,
                collection.targetLanguage,
                collection.originLanguage
            )
            
            if (parseResult.isFailure) {
                state.value = state.value.copy(
                    error = parseResult.exceptionOrNull()?.message ?: "Failed to parse collection"
                )
                return@launch
            }
            
            val totalWords = parseResult.getOrNull()!!.size
            
            state.value = state.value.copy(
                selectedCollection = collection,
                downloadInfo = DownloadInfo(
                    wordCount = totalWords,
                    content = content
                )
            )
        }
    }
    
    fun confirmImport() {
        val collection = state.value.selectedCollection
        val downloadInfo = state.value.downloadInfo
        
        if (collection == null || downloadInfo == null) return
        
        state.value = state.value.copy(
            selectedCollection = null,
            downloadInfo = null,
            isImporting = true,
            importProgress = ""
        )
        
        scope.launch {
            state.value = state.value.copy(importProgress = "Checking for duplicates...")
            importWordsUseCase(
                text = downloadInfo.content
            ).collect { importResult ->
                when (importResult) {
                    is ImportWordsUseCase.ImportResult.Success -> {
                        state.value = state.value.copy(
                            isImporting = false,
                            importProgress = "",
                            successMessage = "Successfully imported ${importResult.count} words from ${collection.title}!"
                        )
                    }
                    is ImportWordsUseCase.ImportResult.Error -> {
                        state.value = state.value.copy(
                            isImporting = false,
                            error = importResult.message
                        )
                    }
                }
            }
        }
    }
}

