package feature.addwords.source

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.Try
import core.common.flatMap
import core.common.fold
import core.common.map
import domain.word.add.model.AddWordsCommand
import domain.word.add.model.AddWordsOutcome
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.AddWordsUseCase
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.toProblem
import kotlinx.coroutines.launch

private const val MAX_RECENT_WORDS = 5

data class AddedWord(val term: String, val translation: String)

data class ManualEntryState(
    val term: String = "",
    val translation: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val problem: AddWordsProblem? = null,
    /** True when the last attempt matched a word the user already has. */
    val alreadyAdded: Boolean = false,
    val addedCount: Int = 0,
    /** Words added in this sheet, newest first. */
    val recent: List<AddedWord> = emptyList(),
) {
    val canAdd: Boolean get() = term.isNotBlank() && translation.isNotBlank() && !isSaving
}

sealed interface ManualEntryEffect {
    /** A word was saved; the form is cleared and ready for the next one. */
    data object WordAdded : ManualEntryEffect
}

/** Type one word at a time. Each word is saved immediately (no review step). */
class ManualEntryViewModel(
    private val addWords: AddWordsUseCase,
    private val analytics: IAnalyticsTracker,
) : BaseViewModel<ManualEntryState, ManualEntryEffect>() {

    override fun initialState() = ManualEntryState()

    fun setTerm(value: String) = updateState { copy(term = value, problem = null, alreadyAdded = false) }

    fun setTranslation(value: String) = updateState { copy(translation = value, problem = null, alreadyAdded = false) }

    fun setNote(value: String) = updateState { copy(note = value) }

    /** Starts a new batch once its words were reported, so they are not counted twice. */
    fun clearSession() = updateState { ManualEntryState() }

    fun add(languages: LanguagePair?, tagIds: Set<Long>) {
        val state = currentState
        if (!state.canAdd || languages == null) return

        updateState { copy(isSaving = true, problem = null, alreadyAdded = false) }
        viewModelScope.launch {
            WordDraft.of(state.term, state.translation, state.note)
                .flatMap { draft -> save(draft, languages, tagIds).map { draft to it } }
                .fold(
                    onSuccess = { (draft, outcome) -> onSaved(draft, outcome) },
                    onFailure = { error ->
                        updateState { copy(isSaving = false, problem = error.toProblem(WordOrigin.Manual)) }
                    },
                )
        }
    }

    private suspend fun save(draft: WordDraft, languages: LanguagePair, tagIds: Set<Long>): Try<AddWordsOutcome> =
        addWords(AddWordsCommand(listOf(draft), languages, tagIds, WordOrigin.Manual))

    private fun onSaved(draft: WordDraft, outcome: AddWordsOutcome) {
        if (outcome.added == 0) {
            // Keep what was typed so the user can change it.
            updateState { copy(isSaving = false, alreadyAdded = true) }
            return
        }
        analytics.logWordsImported(count = outcome.added, method = WordOrigin.Manual.analyticsName)
        updateState {
            ManualEntryState(
                addedCount = addedCount + outcome.added,
                recent = (listOf(AddedWord(draft.term, draft.translation)) + recent).take(MAX_RECENT_WORDS),
            )
        }
        emitEffect(ManualEntryEffect.WordAdded)
    }
}
