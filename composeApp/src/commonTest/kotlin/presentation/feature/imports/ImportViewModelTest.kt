package presentation.feature.imports

import core.common.Try
import domain.ai.repository.IAiRepository
import domain.ai.usecase.ImportFromImageUseCase
import domain.auth.manager.IUserManager
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.repository.IAuthRepository
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.word.service.IImportValidationService
import domain.word.usecase.GetSourceLanguageUseCase
import domain.word.usecase.ImportViaFileUseCase
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import presentation.ui.components.imports.ImportTabV2
import presentation.ui.components.imports.ImportViewModel
import presentation.ui.components.imports.PendingImportAction
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportViewModelTest : ViewModelTestBase() {

    private val settingsRepository = FakeSettingsRepo()
    private val wordRepository = FakeWordRepo()
    private val validationService = FakeValidationService()
    private val getCurrentLanguageUseCase = GetCurrentLanguageUseCase(settingsRepository)
    private val importWordsUseCase = ImportWordsUseCase(wordRepository, validationService, getCurrentLanguageUseCase)
    private val importViaFileUseCase = ImportViaFileUseCase(importWordsUseCase)
    private val aiRepository = FakeAiRepo()
    private val importFromImageUseCase =
        ImportFromImageUseCase(aiRepository, importWordsUseCase, getCurrentLanguageUseCase)
    private val userManager = FakeUserManager()
    private val authRepository = FakeAuthRepo()
    private val getFeatureAccessUseCase = GetFeatureAccessUseCase(authRepository)
    private val getSourceLanguageUseCase = GetSourceLanguageUseCase(wordRepository)

    private fun createViewModel() = ImportViewModel(
        getFeatureAccessUseCase = getFeatureAccessUseCase,
        importWordsUseCase = importWordsUseCase,
        importViaFileUseCase = importViaFileUseCase,
        importFromImageUseCase = importFromImageUseCase,
        userManager = userManager,
        getCurrentLanguageUseCase = getCurrentLanguageUseCase,
        getSourceLanguageUseCase = getSourceLanguageUseCase,
    )

    @Test
    fun `initial state has Text and File tabs`() = runTest {
        val vm = createViewModel()

        val state = vm.currentState
        assertEquals(2, state.tabs.size)
        assertIs<ImportTabV2.Text>(state.tabs[0])
        assertIs<ImportTabV2.File>(state.tabs[1])
    }

    @Test
    fun `selectTab updates selected tab`() = runTest {
        val vm = createViewModel()

        vm.selectTab(ImportTabV2.File())

        assertIs<ImportTabV2.File>(vm.currentState.selectedTab)
    }

    @Test
    fun `updateWord updates text input state`() = runTest {
        val vm = createViewModel()

        vm.updateWord("hello")

        assertEquals("hello", vm.currentState.textInputState.word)
    }

    @Test
    fun `updateTranslation updates text input state`() = runTest {
        val vm = createViewModel()

        vm.updateTranslation("hola")

        assertEquals("hola", vm.currentState.textInputState.translation)
    }

    @Test
    fun `updateDescription updates text input state`() = runTest {
        val vm = createViewModel()

        vm.updateDescription("greeting")

        assertEquals("greeting", vm.currentState.textInputState.description)
    }

    @Test
    fun `updateWord clears error message`() = runTest {
        val vm = createViewModel()

        vm.updateWord("test")

        assertNull(vm.currentState.textInputState.errorMessage)
    }

    @Test
    fun `addWord does nothing when word is blank`() = runTest {
        val vm = createViewModel()
        vm.updateWord("")
        vm.updateTranslation("hola")

        vm.addWord()

        assertEquals(0, vm.currentState.textInputState.wordsAddedCount)
    }

    @Test
    fun `addWord does nothing when translation is blank`() = runTest {
        val vm = createViewModel()
        vm.updateWord("hello")
        vm.updateTranslation("")

        vm.addWord()

        assertEquals(0, vm.currentState.textInputState.wordsAddedCount)
    }

    @Test
    fun `addWord disables input while processing`() = runTest {
        wordRepository.insertGate = CompletableDeferred()
        val vm = createViewModel()
        vm.updateWord("hello")
        vm.updateTranslation("hola")

        vm.addWord()

        // Coroutine is suspended at insertGate, so isEnabled should still be false
        assertFalse(vm.currentState.textInputState.isEnabled)
    }

    @Test
    fun `importFile sets pending import action`() = runTest {
        val vm = createViewModel()

        vm.importFile("hello,hola", "words.txt")

        assertTrue(vm.currentState.showLanguageConfirmation)
        val pending = vm.currentState.pendingImportAction
        assertIs<PendingImportAction.File>(pending)
        assertEquals("hello,hola", pending.content)
        assertEquals("words.txt", pending.fileName)
    }

    @Test
    fun `dismissLanguageConfirmation clears pending state`() = runTest {
        val vm = createViewModel()
        vm.importFile("content", "file.txt")

        vm.dismissLanguageConfirmation()

        assertFalse(vm.currentState.showLanguageConfirmation)
        assertNull(vm.currentState.pendingImportAction)
    }

    @Test
    fun `selectSourceLanguage updates state`() = runTest {
        val vm = createViewModel()

        vm.selectSourceLanguage(Language.GERMAN)

        assertEquals(Language.GERMAN, vm.currentState.sourceLanguage)
    }

    @Test
    fun `selectTargetLanguage updates state`() = runTest {
        val vm = createViewModel()

        vm.selectTargetLanguage(Language.SPANISH)

        assertEquals(Language.SPANISH, vm.currentState.targetLanguage)
    }

    @Test
    fun `selectImage updates image tab`() = runTest {
        // Add Image tab first
        authRepository.featureAccessFlow = flowOf(
            FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = true))
        )
        userManager.userFlow.value = AuthUser(1L, "test@test.com", "Test")
        val vm = createViewModel()

        val imageBytes = byteArrayOf(1, 2, 3)
        vm.selectImage(imageBytes)

        val imageTab = vm.currentState.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        assertTrue(imageTab?.selectedImage?.contentEquals(imageBytes) == true)
    }

    @Test
    fun `clearSelectedImage clears image from tab`() = runTest {
        authRepository.featureAccessFlow = flowOf(
            FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = true))
        )
        userManager.userFlow.value = AuthUser(1L, "test@test.com", "Test")
        val vm = createViewModel()

        vm.selectImage(byteArrayOf(1, 2, 3))
        vm.clearSelectedImage()

        val imageTab = vm.currentState.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        assertNull(imageTab?.selectedImage)
    }

    @Test
    fun `premium user gets Image tab`() = runTest {
        authRepository.featureAccessFlow = flowOf(
            FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = true))
        )
        userManager.userFlow.value = AuthUser(1L, "test@test.com", "Test")

        val vm = createViewModel()

        val hasImageTab = vm.currentState.tabs.any { it is ImportTabV2.Image }
        assertTrue(hasImageTab)
    }

    @Test
    fun `non-premium user does not get Image tab`() = runTest {
        authRepository.featureAccessFlow = flowOf(
            FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = false))
        )
        userManager.userFlow.value = AuthUser(1L, "test@test.com", "Test")

        val vm = createViewModel()

        val hasImageTab = vm.currentState.tabs.any { it is ImportTabV2.Image }
        assertFalse(hasImageTab)
    }

    @Test
    fun `unauthenticated user does not get Image tab`() = runTest {
        userManager.userFlow.value = null

        val vm = createViewModel()

        val hasImageTab = vm.currentState.tabs.any { it is ImportTabV2.Image }
        assertFalse(hasImageTab)
    }

    // --- Fakes ---

    private class FakeSettingsRepo : ISettingsRepository {
        override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
        override suspend fun setLanguage(language: Language) {}
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode) {}
        override suspend fun getLastInsightDate(): String? = null
        override suspend fun getCachedInsight(): String? = null
        override suspend fun updateDailyInsight(date: String, insight: String) {}
        override suspend fun getLastInsightDismissedTime(): Long = 0L
        override suspend fun setLastInsightDismissedTime(timestamp: Long) {}
        override suspend fun clearInsightData() {}
        override suspend fun clearSettings() {}
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean) {}
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {}
        override suspend fun getDailyReminderTime(): String = "09:00"
        override suspend fun setDailyReminderTime(time: String) {}
        override suspend fun getMinimumDueCards(): Int = 5
        override suspend fun setMinimumDueCards(count: Int) {}
    }

    private class FakeWordRepo : IWordRepository {
        var insertResult: Try<Int> = Try.success(1)
        var insertGate: CompletableDeferred<Unit>? = null

        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> {
            insertGate?.await()
            return insertResult
        }
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String,
        ): Flow<UpdateWordsLanguagesProgress> =
            flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }

    private class FakeValidationService : IImportValidationService {
        var parseResult: Try<List<Word>> = Try.success(emptyList())

        override fun validateAndParse(
            text: String,
            sourceLanguage: Language,
            targetLanguage: Language
        ): Try<List<Word>> = parseResult
    }

    private class FakeAiRepo : IAiRepository {
        var extractResult: Try<String> = Try.success("")

        override suspend fun extractVocabularyFromImage(
            imageBytes: ByteArray,
            targetLanguage: Language,
            extractWords: Boolean,
            extractSentences: Boolean
        ): Try<String> = extractResult
    }

    private class FakeUserManager : IUserManager {
        val userFlow = MutableStateFlow<AuthUser?>(null)

        override fun observeUser(): Flow<AuthUser?> = userFlow
        override fun setUser(user: AuthUser?) { userFlow.value = user }
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
    }

    private class FakeAuthRepo : IAuthRepository {
        var featureAccessFlow: Flow<FeatureAccessResponse> = flowOf()

        override suspend fun isAuthenticated(): Boolean = true
        override fun isAuthenticatedAsFlow(): Flow<Boolean> = flowOf(true)
        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> =
            Try.success(AuthUser(1L, "test@test.com", "Test"))
        override suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser> =
            Try.success(AuthUser(1L, "test@test.com", "Test"))
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
        override suspend fun getAccessToken(): String = "fake-token"
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = featureAccessFlow
    }
}
