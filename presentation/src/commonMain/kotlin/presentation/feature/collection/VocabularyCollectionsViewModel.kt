package presentation.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.collection.model.VocabularyCollection
import domain.collection.repository.ICollectionRepository
import domain.word.usecase.ImportVocabularyCollectionUseCase
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VocabularyCollectionsViewModel(
    private val collectionRepository: ICollectionRepository,
    private val importVocabularyCollectionUseCase: ImportVocabularyCollectionUseCase,
    private val importWordsUseCase: ImportWordsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState = _uiState.asStateFlow()

    private val loaderHandler = CollectionLoaderHandler(
        collectionRepository = collectionRepository,
        state = _uiState,
        scope = viewModelScope
    )

    private val importHandler = CollectionImportHandler(
        collectionRepository = collectionRepository,
        importVocabularyCollectionUseCase = importVocabularyCollectionUseCase,
        importWordsUseCase = importWordsUseCase,
        state = _uiState,
        scope = viewModelScope
    )

    init {
        loaderHandler.loadCollections()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun retry() {
        loaderHandler.loadCollections()
    }

    fun selectCollection(collection: VocabularyCollection) {
        importHandler.selectCollection(collection)
    }

    fun clearSelectedCollection() {
        _uiState.value = _uiState.value.copy(selectedCollection = null, downloadInfo = null)
    }

    fun confirmImport() {
        importHandler.confirmImport()
    }
}

data class CollectionsUiState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val isDownloading: Boolean = false,
    val importProgress: String = "",
    val downloadProgress: String = "",
    val collections: List<VocabularyCollection> = emptyList(),
    val groupedCollections: Map<String, Map<String, List<VocabularyCollection>>> = emptyMap(), // TargetLanguage -> OriginLanguage -> Collections
    val error: String? = null,
    val successMessage: String? = null,
    val selectedCollection: VocabularyCollection? = null,
    val downloadInfo: DownloadInfo? = null
)

data class DownloadInfo(
    val wordCount: Int,
    val content: String
)
