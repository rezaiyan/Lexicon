package feature.addwords.source

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.word.add.model.WordFile
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.ParseWordFileUseCase
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.SourceEffect
import feature.addwords.model.toProblem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FileImportState(
    val isParsing: Boolean = false,
    val problem: AddWordsProblem? = null,
)

/** Reads a picked text file into candidates for review. Nothing is saved here. */
class FileImportViewModel(
    private val parseWordFile: ParseWordFileUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel<FileImportState, SourceEffect>() {

    override fun initialState() = FileImportState()

    /** [name] and [bytes] are null when the user cancelled the picker. */
    fun onFilePicked(name: String?, bytes: ByteArray?) {
        if (bytes == null || currentState.isParsing) return

        updateState { copy(isParsing = true, problem = null) }
        viewModelScope.launch {
            withContext(dispatcher) { parseWordFile(WordFile(name, bytes)) }.fold(
                onSuccess = { report ->
                    updateState { copy(isParsing = false) }
                    emitEffect(SourceEffect.CandidatesReady(report.drafts, report.rejected))
                },
                onFailure = { error ->
                    updateState { copy(isParsing = false, problem = error.toProblem(WordOrigin.File)) }
                },
            )
        }
    }

    fun dismissProblem() = updateState { copy(problem = null) }
}
