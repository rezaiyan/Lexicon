package feature.addwords

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import core.common.getOrNull
import domain.credits.usecase.ObserveCreditsUseCase
import domain.credits.usecase.RefreshCreditsUseCase
import domain.tag.usecase.CreateTagUseCase
import domain.tag.usecase.GetTagsUseCase
import domain.word.add.model.AddWordsCommand
import domain.word.add.model.WordDraft
import domain.word.add.model.WordOrigin
import domain.word.add.parser.RejectedLine
import domain.word.add.repository.IAddWordsLanguageRepository
import domain.word.add.usecase.AddWordsUseCase
import domain.word.add.usecase.ResolveAddWordsLanguagesUseCase
import feature.addwords.model.AddWordsEffect
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.AddWordsResult
import feature.addwords.model.AddWordsUiState
import feature.addwords.model.CandidateReview
import feature.addwords.model.logImportEvent
import feature.addwords.model.logImportFailed
import feature.addwords.model.toProblem
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import utils.Language

private const val PREVIEW_TERMS = 3

/**
 * Host of one add-words sheet. Owns the shared context (languages, tag, AI credits), the review of
 * candidates produced by any source, and the single commit through [AddWordsUseCase].
 */
@Suppress("TooManyFunctions") // event sink: one small public method per user action
class AddWordsViewModel(
    private val resolveLanguages: ResolveAddWordsLanguagesUseCase,
    private val languageRepository: IAddWordsLanguageRepository,
    private val addWords: AddWordsUseCase,
    private val getTags: GetTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val observeCredits: ObserveCreditsUseCase,
    private val refreshCredits: RefreshCreditsUseCase,
    private val analytics: IAnalyticsTracker,
) : BaseViewModel<AddWordsUiState, AddWordsEffect>() {

    override fun initialState() = AddWordsUiState()

    init {
        viewModelScope.launch {
            val resolved = resolveLanguages(Unit).getOrNull()
            updateState { copy(learning = resolved?.learning, native = resolved?.native, languagesLoaded = true) }
        }
        viewModelScope.launch {
            getTags().catch { emit(emptyList()) }.collect { tags -> updateState { copy(tags = tags) } }
        }
        viewModelScope.launch {
            observeCredits().catch { emit(null) }.collect { updateState { copy(credits = it) } }
        }
        // The cached balance may be stale (renewal, purchase, another device): re-read on open
        reloadCredits()
    }

    // --- Languages ---

    fun setLearningLanguage(language: Language) = updateState {
        copy(learning = language, native = native.takeUnless { it == language })
    }

    fun setNativeLanguage(language: Language) = updateState {
        copy(native = language, learning = learning.takeUnless { it == language })
    }

    fun swapLanguages() = updateState { copy(learning = native, native = learning) }

    // --- Tags ---

    fun selectTag(tagId: Long?) = updateState { copy(selectedTagId = tagId) }

    fun createTag(name: String) {
        viewModelScope.launch {
            createTagUseCase(name).fold(
                onSuccess = { tag -> updateState { copy(selectedTagId = tag.id, problem = null) } },
                onFailure = { updateState { copy(problem = AddWordsProblem.Generic) } },
            )
        }
    }

    // --- Review ---

    /** The user picked a way of adding words: top of the add-words funnel. */
    fun sourceOpened(origin: WordOrigin) = analytics.logImportEvent("import_started", origin)

    fun openReview(origin: WordOrigin, drafts: List<WordDraft>, rejected: List<RejectedLine> = emptyList()) {
        updateState { copy(review = CandidateReview.of(origin, drafts, rejected), problem = null, result = null) }
        emitEffect(AddWordsEffect.OpenReview)
        analytics.logImportEvent(
            "import_preview_shown", origin, "word_count" to drafts.size, "rejected_count" to rejected.size,
        )
    }

    fun toggleCandidate(id: Int) = updateReview { toggle(id) }

    fun setAllCandidatesSelected(selected: Boolean) = updateReview { setAllSelected(selected) }

    fun removeCandidate(id: Int) = updateReview { remove(id) }

    fun startEditingCandidate(id: Int) = updateReview { startEditing(id) }

    fun cancelEditingCandidate() = updateReview { cancelEditing() }

    fun saveCandidateEdit(term: String, translation: String, note: String) {
        val review = currentState.review ?: return
        review.saveEdit(term, translation, note).fold(
            onSuccess = { updated -> updateState { copy(review = updated, problem = null) } },
            onFailure = { error -> updateState { copy(problem = error.toProblem(review.origin)) } },
        )
    }

    fun discardReview() {
        val origin = currentState.review?.origin ?: return
        updateState { copy(review = null, problem = null) }
        analytics.logImportEvent("import_cancelled", origin, "at_step" to "review")
    }

    fun commitReview() {
        val state = currentState
        val review = state.review ?: return
        val languages = state.languages ?: return
        if (state.isCommitting || review.selectedCount == 0) return

        updateState { copy(isCommitting = true, problem = null) }
        viewModelScope.launch {
            addWords(AddWordsCommand(review.selectedDrafts, languages, state.tagIds, review.origin)).fold(
                onSuccess = { outcome ->
                    languageRepository.saveLastUsed(languages)
                    val terms = outcome.addedTerms.take(PREVIEW_TERMS)
                    updateState {
                        copy(
                            isCommitting = false,
                            review = null,
                            result = AddWordsResult(review.origin, outcome.added, outcome.duplicates, terms),
                        )
                    }
                    emitEffect(AddWordsEffect.ShowResult)
                    // Logged last so a tracker failure can't leave the sheet stuck mid-commit.
                    analytics.logImportEvent(
                        "import_confirmed", review.origin,
                        "word_count" to outcome.added, "duplicates" to outcome.duplicates,
                    )
                    if (outcome.added > 0) {
                        analytics.logWordsImported(count = outcome.added, method = review.origin.analyticsName)
                    }
                },
                // The review stays so nothing the user checked or edited is lost.
                onFailure = { error ->
                    val problem = error.toProblem(review.origin)
                    updateState { copy(isCommitting = false, problem = problem) }
                    analytics.logImportFailed(review.origin, step = "commit", problem)
                },
            )
        }
    }

    // --- Results from sources that commit themselves ---

    fun finishManualEntry(added: Int, recentTerms: List<String>) {
        val languages = currentState.languages
        if (added == 0 || languages == null) return
        viewModelScope.launch { languageRepository.saveLastUsed(languages) }
        updateState {
            copy(result = AddWordsResult(WordOrigin.Manual, added, duplicates = 0, recentTerms.take(PREVIEW_TERMS)))
        }
        emitEffect(AddWordsEffect.ShowResult)
    }

    fun dismissProblem() = updateState { copy(problem = null) }

    // --- Credits ---

    /**
     * Best effort: on failure the last known balance stays and the server still has the final say.
     * Spends need no call here: the AI repository re-reads the shared balance after every paid call.
     */
    private fun reloadCredits() {
        viewModelScope.launch { refreshCredits() }
    }

    private fun updateReview(change: CandidateReview.() -> CandidateReview) =
        updateState { copy(review = review?.change(), problem = null) }
}
