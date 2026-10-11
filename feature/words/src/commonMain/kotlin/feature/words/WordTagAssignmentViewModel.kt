package feature.words

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import core.error.toUserMessage
import domain.tag.usecase.AssignWordTagsParams
import domain.tag.usecase.AssignWordTagsUseCase
import domain.tag.usecase.CreateTagUseCase
import domain.tag.usecase.GetTagsUseCase
import feature.words.model.WordTagAssignmentEffect
import feature.words.model.WordTagAssignmentState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class WordTagAssignmentViewModel(
    private val getTagsUseCase: GetTagsUseCase,
    private val assignWordTagsUseCase: AssignWordTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
) : BaseViewModel<WordTagAssignmentState, WordTagAssignmentEffect>() {

    override fun initialState() = WordTagAssignmentState()

    private var tagsJob: Job? = null

    fun initialize(wordId: Int, currentTagIds: List<Long>) {
        updateState { copy(wordId = wordId, selectedTagIds = currentTagIds.toSet(), isLoading = true) }
        // One tag subscription per ViewModel, however often a word is opened.
        tagsJob?.cancel()
        tagsJob = viewModelScope.launch {
            getTagsUseCase()
                .catch { error ->
                    updateState { copy(isLoading = false) }
                    emitEffect(WordTagAssignmentEffect.Error(error.toUserMessage()))
                }
                .collect { tags ->
                    updateState { copy(tags = tags, isLoading = false) }
                }
        }
    }

    fun toggleTag(tagId: Long) {
        updateState {
            val newIds = if (selectedTagIds.contains(tagId)) selectedTagIds - tagId else selectedTagIds + tagId
            copy(selectedTagIds = newIds)
        }
    }

    /** Creates a tag and selects it for the word; the tag list picks it up from the tags flow. */
    fun createTag(name: String) {
        viewModelScope.launch {
            createTagUseCase(name).fold(
                onSuccess = { tag ->
                    updateState { copy(selectedTagIds = selectedTagIds + tag.id) }
                    emitEffect(WordTagAssignmentEffect.TagCreated)
                },
                onFailure = { error ->
                    emitEffect(WordTagAssignmentEffect.Error(error.toUserMessage()))
                }
            )
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
                    emitEffect(WordTagAssignmentEffect.Error(error.toUserMessage()))
                }
            )
        }
    }
}
