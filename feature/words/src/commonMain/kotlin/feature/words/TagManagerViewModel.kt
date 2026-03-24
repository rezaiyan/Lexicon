package feature.words

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.tag.usecase.CreateTagUseCase
import domain.tag.usecase.DeleteTagUseCase
import domain.tag.usecase.GetTagsUseCase
import domain.tag.usecase.RenameTagUseCase
import domain.tag.usecase.RenameTagParams
import feature.words.model.TagManagerEffect
import feature.words.model.TagManagerState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TagManagerViewModel(
    private val getTagsUseCase: GetTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val renameTagUseCase: RenameTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
) : BaseViewModel<TagManagerState, TagManagerEffect>() {

    override fun initialState() = TagManagerState()

    init {
        startObservingTags()
    }

    private fun startObservingTags() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            getTagsUseCase()
                .catch { error ->
                    updateState { copy(isLoading = false, errorMessage = error.message) }
                }
                .collect { tags ->
                    updateState { copy(tags = tags, isLoading = false, errorMessage = null) }
                }
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            createTagUseCase(name).fold(
                onSuccess = { tag -> emitEffect(TagManagerEffect.TagCreated(tag)) },
                onFailure = { error -> emitEffect(TagManagerEffect.Error(error.message ?: "Failed to create tag")) }
            )
        }
    }

    fun renameTag(id: Long, name: String) {
        viewModelScope.launch {
            renameTagUseCase(RenameTagParams(id = id, name = name)).fold(
                onSuccess = { tag -> emitEffect(TagManagerEffect.TagRenamed(tag)) },
                onFailure = { error -> emitEffect(TagManagerEffect.Error(error.message ?: "Failed to rename tag")) }
            )
        }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch {
            deleteTagUseCase(id).fold(
                onSuccess = { emitEffect(TagManagerEffect.TagDeleted(id)) },
                onFailure = { error -> emitEffect(TagManagerEffect.Error(error.message ?: "Failed to delete tag")) }
            )
        }
    }
}
