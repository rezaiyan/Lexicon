package feature.words

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.tag.usecase.AssignWordTagsParams
import domain.tag.usecase.AssignWordTagsUseCase
import domain.tag.usecase.GetTagsUseCase
import feature.words.model.WordTagAssignmentEffect
import feature.words.model.WordTagAssignmentState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class WordTagAssignmentViewModel(
    private val getTagsUseCase: GetTagsUseCase,
    private val assignWordTagsUseCase: AssignWordTagsUseCase,
) : BaseViewModel<WordTagAssignmentState, WordTagAssignmentEffect>() {

    override fun initialState() = WordTagAssignmentState()

    fun initialize(wordId: Int, currentTagIds: List<Long>) {
        updateState { copy(wordId = wordId, selectedTagIds = currentTagIds.toSet(), isLoading = true) }
        viewModelScope.launch {
            getTagsUseCase()
                .catch { error ->
                    updateState { copy(isLoading = false) }
                    emitEffect(WordTagAssignmentEffect.Error(error.message ?: "Failed to load tags"))
                }
                .collect { tags ->
                    updateState { copy(tags = tags, isLoading = false) }
                }
        }
    }

    fun toggleTag(tagId: Long) {
        updateState {
            val updated = if (selectedTagIds.contains(tagId)) {
                selectedTagIds - tagId
            } else {
                selectedTagIds + tagId
            }
            copy(selectedTagIds = updated)
        }
    }

    fun save() {
        val state = currentState
        viewModelScope.launch {
            updateState { copy(isSaving = true) }
            assignWordTagsUseCase(
                AssignWordTagsParams(
                    wordId = state.wordId.toLong(),
                    tagIds = state.selectedTagIds.toList()
                )
            ).fold(
                onSuccess = {
                    updateState { copy(isSaving = false) }
                    emitEffect(WordTagAssignmentEffect.TagsAssigned)
                },
                onFailure = { error ->
                    updateState { copy(isSaving = false) }
                    emitEffect(WordTagAssignmentEffect.Error(error.message ?: "Failed to save tags"))
                }
            )
        }
    }
}
