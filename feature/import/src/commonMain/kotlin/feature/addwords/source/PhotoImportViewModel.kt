package feature.addwords.source

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import core.error.DomainError
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.ExtractWordsFromImageUseCase
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.SourceEffect
import feature.addwords.model.toProblem
import kotlinx.coroutines.launch

/** A picked photo. Identity equality on purpose: comparing image bytes on every recomposition is wasteful. */
class PickedPhoto(val bytes: ByteArray)

data class PhotoImportState(
    val photo: PickedPhoto? = null,
    val isExtracting: Boolean = false,
    val problem: AddWordsProblem? = null,
) {
    val canExtract: Boolean get() = photo != null && !isExtracting
}

/** Picks a photo and asks the AI for the words in it. Nothing is saved here. */
class PhotoImportViewModel(
    private val extractWords: ExtractWordsFromImageUseCase,
) : BaseViewModel<PhotoImportState, SourceEffect>() {

    override fun initialState() = PhotoImportState()

    /** [bytes] is null when the user cancelled the camera or gallery. */
    fun onPhotoPicked(bytes: ByteArray?) {
        if (bytes == null || currentState.isExtracting) return
        updateState { copy(photo = PickedPhoto(bytes), problem = null) }
    }

    fun clearPhoto() {
        if (currentState.isExtracting) return
        updateState { copy(photo = null, problem = null) }
    }

    fun extract(languages: LanguagePair?) {
        val state = currentState
        val photo = state.photo ?: return
        if (!state.canExtract || languages == null) return

        updateState { copy(isExtracting = true, problem = null) }
        viewModelScope.launch {
            extractWords(ExtractWordsFromImageUseCase.Params(photo.bytes, languages)).fold(
                onSuccess = { drafts ->
                    updateState { copy(isExtracting = false) }
                    emitEffect(SourceEffect.CandidatesReady(drafts))
                },
                onFailure = { error ->
                    updateState { copy(isExtracting = false, problem = error.toProblem(WordOrigin.Photo)) }
                    if (error is DomainError.Commerce.PremiumRequired) emitEffect(SourceEffect.PremiumLapsed)
                },
            )
        }
    }

    fun dismissProblem() = updateState { copy(problem = null) }
}
