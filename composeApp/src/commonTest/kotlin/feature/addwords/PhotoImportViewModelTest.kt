package feature.addwords

import app.cash.turbine.test
import core.common.Try
import core.common.getOrThrow
import core.error.DomainError
import domain.subscription.usecase.RefreshFeatureAccessUseCase
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.usecase.ExtractWordsFromImageUseCase
import fakes.FakeAiRepository
import fakes.FakeAnalyticsTracker
import fakes.FakeImagePreparer
import fakes.FakeSubscriptionAccessRepository
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.SourceEffect
import feature.addwords.source.PhotoImportViewModel
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoImportViewModelTest : ViewModelTestBase() {

    private val languages = LanguagePair(Language.GERMAN, Language.ENGLISH)
    private val ai = FakeAiRepository()
    private val image = byteArrayOf(1, 2, 3)
    private val analytics = FakeAnalyticsTracker()
    private val preparer = FakeImagePreparer()

    private fun createViewModel() = PhotoImportViewModel(
        ExtractWordsFromImageUseCase(
            preparer,
            ai,
            RefreshFeatureAccessUseCase(FakeSubscriptionAccessRepository()),
        ),
        analytics,
    )

    @Test
    fun `extract without a photo does nothing`() {
        val vm = createViewModel()

        vm.extract(languages)

        assertNull(ai.lastImage)
    }

    @Test
    fun `extract sends the photo with the sheet languages and emits the words`() = runTest {
        ai.drafts = Try.success(listOf(WordDraft.of("Hund", "dog").getOrThrow()))
        val vm = createViewModel()
        vm.onPhotoPicked(image)

        vm.effects.test {
            vm.extract(languages)
            val ready = assertIs<SourceEffect.CandidatesReady>(awaitItem())
            assertEquals(listOf("Hund"), ready.drafts.map { it.term })
        }
        assertTrue(ai.lastImage.contentEquals(image))
        assertEquals(languages, ai.lastLanguages)
        assertFalse(vm.currentState.isExtracting)
    }

    @Test
    fun `extract with nothing recognized keeps the photo and says so`() {
        ai.drafts = Try.success(emptyList())
        val vm = createViewModel()
        vm.onPhotoPicked(image)

        vm.extract(languages)

        assertEquals(AddWordsProblem.NothingInPhoto, vm.currentState.problem)
        assertNotNull(vm.currentState.photo)
    }

    @Test
    fun `extract offline shows offline`() {
        ai.drafts = Try.failure(DomainError.Network.NoConnection)
        val vm = createViewModel()
        vm.onPhotoPicked(image)

        vm.extract(languages)

        assertEquals(AddWordsProblem.Offline, vm.currentState.problem)
    }

    @Test
    fun `extract refused for premium reports the lapse`() = runTest {
        ai.drafts = Try.failure(DomainError.Commerce.PremiumRequired)
        val vm = createViewModel()
        vm.onPhotoPicked(image)

        vm.effects.test {
            vm.extract(languages)
            assertEquals(SourceEffect.PremiumLapsed, awaitItem())
        }
        assertEquals(AddWordsProblem.PremiumRequired, vm.currentState.problem)
    }

    @Test
    fun `rotatePhoto turns a quarter clockwise and wraps after a full turn`() {
        val vm = createViewModel()
        vm.onPhotoPicked(image)

        val turns = (1..5).map { vm.rotatePhoto(); vm.currentState.quarterTurns }

        assertEquals(listOf(1, 2, 3, 0, 1), turns)
    }

    @Test
    fun `a new photo starts upright`() {
        val vm = createViewModel()
        vm.onPhotoPicked(image)
        vm.rotatePhoto()

        vm.onPhotoPicked(byteArrayOf(4, 5, 6))

        assertEquals(0, vm.currentState.quarterTurns)
    }

    @Test
    fun `extract uploads the photo turned as shown`() = runTest {
        ai.drafts = Try.success(listOf(WordDraft.of("Hund", "dog").getOrThrow()))
        val vm = createViewModel()
        vm.onPhotoPicked(image)
        vm.rotatePhoto()

        vm.effects.test {
            vm.extract(languages)
            awaitItem()
        }

        assertEquals(1, preparer.lastQuarterTurns)
    }

    @Test
    fun `extract refused for premium logs the failure`() {
        ai.drafts = Try.failure(DomainError.Commerce.PremiumRequired)
        val vm = createViewModel()
        vm.onPhotoPicked(image)

        vm.extract(languages)

        assertEquals(
            listOf<Pair<String, Map<String, Any>?>>(
                "import_failed" to mapOf("method" to "image", "step" to "extract", "error_type" to "PremiumRequired"),
            ),
            analytics.events,
        )
    }

    @Test
    fun `onPhotoPicked with a cancelled picker keeps the current photo`() {
        val vm = createViewModel()
        vm.onPhotoPicked(image)

        vm.onPhotoPicked(null)

        assertNotNull(vm.currentState.photo)
    }

    @Test
    fun `clearPhoto removes the photo`() {
        val vm = createViewModel()
        vm.onPhotoPicked(image)

        vm.clearPhoto()

        assertNull(vm.currentState.photo)
    }
}
