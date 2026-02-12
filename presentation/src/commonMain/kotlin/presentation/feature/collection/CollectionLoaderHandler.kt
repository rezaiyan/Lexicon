package presentation.feature.collection

import data.collection.remote.CollectionRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Handler for loading vocabulary collections
 * Manages fetching and grouping collections by language
 */
internal class CollectionLoaderHandler(
    private val collectionRemoteDataSource: CollectionRemoteDataSource,
    private val state: MutableStateFlow<CollectionsUiState>,
    private val scope: CoroutineScope
) {
    
    fun loadCollections() {
        scope.launch {
            state.value = state.value.copy(isLoading = true)
            
            val result = collectionRemoteDataSource.getAvailableCollections()
            
            result.fold(
                onSuccess = { collections ->
                    val grouped = collections.groupBy { it.targetLanguage }
                        .mapValues { (_, collections) ->
                            collections.groupBy { it.originLanguage }
                        }
                    state.value = state.value.copy(
                        isLoading = false,
                        collections = collections,
                        groupedCollections = grouped
                    )
                },
                onFailure = { error ->
                    state.value = state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load collections"
                    )
                }
            )
        }
    }
}



