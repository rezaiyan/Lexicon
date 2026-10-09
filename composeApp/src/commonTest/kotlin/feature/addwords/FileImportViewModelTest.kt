package feature.addwords

import app.cash.turbine.test
import domain.word.add.usecase.ParseWordFileUseCase
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.SourceEffect
import feature.addwords.source.FileImportViewModel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class FileImportViewModelTest : ViewModelTestBase() {

    private fun createViewModel() = FileImportViewModel(ParseWordFileUseCase(), UnconfinedTestDispatcher())

    @Test
    fun `onFilePicked emits the parsed words and the skipped lines`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.onFilePicked("words.csv", "Hund,dog\nKatze\nMaus,mouse".encodeToByteArray())

            val ready = assertIs<SourceEffect.CandidatesReady>(awaitItem())
            assertEquals(listOf("Hund", "Maus"), ready.drafts.map { it.term })
            assertEquals(listOf(2), ready.rejected.map { it.lineNumber })
        }
        assertFalse(vm.currentState.isParsing)
    }

    @Test
    fun `onFilePicked with a cancelled picker does nothing`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.onFilePicked(null, null)
            expectNoEvents()
        }
    }

    @Test
    fun `onFilePicked with a binary file shows unsupported file`() {
        val vm = createViewModel()

        vm.onFilePicked("photo.png", byteArrayOf(0x89.toByte(), 0x50, 0, 0, 0x0D, 0x0A))

        assertEquals(AddWordsProblem.UnsupportedFile, vm.currentState.problem)
    }

    @Test
    fun `onFilePicked with an empty file shows empty file`() {
        val vm = createViewModel()

        vm.onFilePicked("words.txt", "  \n".encodeToByteArray())

        assertEquals(AddWordsProblem.EmptyFile, vm.currentState.problem)
    }

    @Test
    fun `onFilePicked with no valid line shows nothing found`() {
        val vm = createViewModel()

        vm.onFilePicked("words.txt", "just words\nwithout translations".encodeToByteArray())

        assertEquals(AddWordsProblem.NothingInFile, vm.currentState.problem)
    }

    @Test
    fun `onFilePicked with a huge file shows file too large`() {
        val vm = createViewModel()

        vm.onFilePicked("words.txt", ByteArray(ParseWordFileUseCase.MAX_FILE_BYTES + 1) { 'a'.code.toByte() })

        assertEquals(AddWordsProblem.FileTooLarge, vm.currentState.problem)
    }
}
