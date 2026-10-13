package feature.addwords.source

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.common.fold
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.ExtractWordsFromImageUseCase
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.GuidanceDraft
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
    /** Quick picks and the learner's own words for this photo. */
    override val guidance: GuidanceDraft = GuidanceDraft(),
) : GuidedSourceState<PhotoImportState> {
    val canExtract: Boolean get() = photo != null && !isExtracting

    override val isBusy: Boolean get() = isExtracting

    override fun withGuidance(guidance: GuidanceDraft) = copy(guidance = guidance)
}

/** Picks a photo and asks the AI for the words in it. Nothing is saved here. */
class PhotoImportViewModel(
    private val extractWords: ExtractWordsFromImageUseCase,
    private val analytics: IAnalyticsTracker,
) : GuidedSourceViewModel<PhotoImportState>(WordOrigin.Photo, analytics) {

    override fun initialState() = PhotoImportState()

    /** [bytes] is null when the user cancelled the camera or gallery. Guidance belongs to one photo. */
    fun onPhotoPicked(bytes: ByteArray?) {
        if (bytes == null || currentState.isExtracting) return
        updateState { PhotoImportState(photo = PickedPhoto(bytes)) }
    }

    /** Sideways photos are misread by the vision model, so the user can turn the photo upright first. */
    fun rotatePhoto() {
        if (currentState.photo == null || currentState.isExtracting) return
        updateState { copy(quarterTurns = (quarterTurns + 1) % QUARTER_TURNS_PER_TURN, problem = null) }
    }

    fun clearPhoto() {
        if (currentState.isExtracting) return
        updateState { PhotoImportState() }
    }

    fun extract(languages: LanguagePair?) {
        val state = currentState
        val photo = state.photo ?: return
        if (!state.canExtract || languages == null) return

        logGuidanceSent()
        updateState { copy(isExtracting = true, problem = null) }
        val params = ExtractWordsFromImageUseCase.Params(
            photo.bytes, languages, state.quarterTurns, state.guidance.toGuidance(),
        )
        viewModelScope.launch {
            extractWords(params).fold(
                onSuccess = { drafts ->
                    updateState { copy(isExtracting = false) }
                    emitEffect(SourceEffect.CandidatesReady(drafts))
                },
                onFailure = { error ->
                    updateState { copy(isExtracting = false) }
                    if (showInstructionProblem(error)) return@fold
                    val problem = error.toProblem(WordOrigin.Photo)
                    analytics.logImportFailed(WordOrigin.Photo, step = "extract", problem)
                    // Out of credits gets its own page (with the way to get more), not an inline error
                    if (problem == AddWordsProblem.OutOfCredits) {
                        emitEffect(SourceEffect.OutOfCredits)
                    } else {
                        updateState { copy(problem = problem) }
                    }
                },
            )
        }
    }

    fun dismissProblem() = updateState { copy(problem = null) }
}
