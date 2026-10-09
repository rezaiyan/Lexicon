package feature.addwords.source

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import core.error.DomainError
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.ExtractWordsFromImageUseCase
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.SourceEffect
import feature.addwords.model.logImportFailed
import feature.addwords.model.toProblem
import kotlinx.coroutines.launch

private const val QUARTER_TURNS_PER_TURN = 4

/** A picked photo. Identity equality on purpose: comparing image bytes on every recomposition is wasteful. */
class PickedPhoto(val bytes: ByteArray)

data class PhotoImportState(
    val photo: PickedPhoto? = null,
    val isExtracting: Boolean = false,
    val problem: AddWordsProblem? = null,
    /** Clockwise 90° turns the user applied; the photo is uploaded turned the same way. */
    val quarterTurns: Int = 0,
) {
    val canExtract: Boolean get() = photo != null && !isExtracting
}

/** Picks a photo and asks the AI for the words in it. Nothing is saved here. */
class PhotoImportViewModel(
    private val extractWords: ExtractWordsFromImageUseCase,
    private val analytics: IAnalyticsTracker,
) : BaseViewModel<PhotoImportState, SourceEffect>() {

    override fun initialState() = PhotoImportState()

    /** [bytes] is null when the user cancelled the camera or gallery. */
    fun onPhotoPicked(bytes: ByteArray?) {
        if (bytes == null || currentState.isExtracting) return
        updateState { copy(photo = PickedPhoto(bytes), problem = null, quarterTurns = 0) }
    }

    /** Sideways photos are misread by the vision model, so the user can turn the photo upright first. */
    fun rotatePhoto() {
        if (currentState.photo == null || currentState.isExtracting) return
        updateState { copy(quarterTurns = (quarterTurns + 1) % QUARTER_TURNS_PER_TURN, problem = null) }
    }

    fun clearPhoto() {
        if (currentState.isExtracting) return
        updateState { copy(photo = null, problem = null, quarterTurns = 0) }
    }

    fun extract(languages: LanguagePair?) {
        val state = currentState
        val photo = state.photo ?: return
        if (!state.canExtract || languages == null) return

        updateState { copy(isExtracting = true, problem = null) }
        viewModelScope.launch {
            extractWords(ExtractWordsFromImageUseCase.Params(photo.bytes, languages, state.quarterTurns)).fold(
                onSuccess = { drafts ->
                    updateState { copy(isExtracting = false) }
                    emitEffect(SourceEffect.CandidatesReady(drafts))
                },
                onFailure = { error ->
                    val problem = error.toProblem(WordOrigin.Photo)
                    updateState { copy(isExtracting = false, problem = problem) }
                    analytics.logImportFailed(WordOrigin.Photo, step = "extract", problem)
                    if (error is DomainError.Commerce.PremiumRequired) emitEffect(SourceEffect.PremiumLapsed)
                },
            )
        }
    }

    fun dismissProblem() = updateState { copy(problem = null) }
}
