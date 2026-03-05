package presentation.feature.onboarding

import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.usecase.SubmitPreferencesUseCase
import domain.onboarding.repository.IOnboardingRepository
import domain.settings.model.ThemeMode
import domain.settings.usecase.SetLanguageUseCase
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest : ViewModelTestBase() {

    private var submitResult: Try<SuggestedVocabularyResponse> = Try.success(testResponse())
    private var languageSet: Language? = null

    private fun testResponse() = SuggestedVocabularyResponse(
        suggestedVocabulary = listOf(
            SuggestedVocabulary("hola", "hello", "greeting", "es", "en")
        ),
        targetLanguage = "Spanish",
        nativeLanguage = "English",
        currentLevel = "beginner"
    )

    private fun fakeSubmitUseCase(): SubmitPreferencesUseCase {
        val repo = object : IOnboardingRepository {
            override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse> =
                submitResult
            override suspend fun hasCompletedOnboarding(): Boolean = false
            override suspend fun markOnboardingCompleted() {}
        }
        return SubmitPreferencesUseCase(repo)
    }

    private fun fakeSetLanguageUseCase(): SetLanguageUseCase {
        val repo = object : ISettingsRepository {
            override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
            override suspend fun setLanguage(language: Language) { languageSet = language }
            override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
            override suspend fun setThemeMode(mode: ThemeMode) {}
            override suspend fun getLastInsightDate(): String? = null
            override suspend fun getCachedInsight(): String? = null
            override suspend fun updateDailyInsight(date: String, insight: String) {}
            override suspend fun getLastInsightDismissedTime(): Long = 0L
            override suspend fun setLastInsightDismissedTime(timestamp: Long) {}
            override suspend fun clearInsightData() {}
            override suspend fun clearSettings() {}
            override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setNotificationsEnabled(enabled: Boolean) {}
            override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setReviewRemindersEnabled(enabled: Boolean) {}
            override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {}
            override suspend fun getDailyReminderTime(): String = "09:00"
            override suspend fun setDailyReminderTime(time: String) {}
            override suspend fun getMinimumDueCards(): Int = 5
            override suspend fun setMinimumDueCards(count: Int) {}
        }
        return SetLanguageUseCase(repo)
    }

    private fun createViewModel() = OnboardingViewModel(
        submitPreferencesUseCase = fakeSubmitUseCase(),
        setLanguageUseCase = fakeSetLanguageUseCase()
    )

    @Test
    fun `initial state has step 0 and no selections`() {
        val vm = createViewModel()
        assertEquals(0, vm.currentState.currentStep)
        assertNull(vm.currentState.selectedTargetLanguage)
        assertNull(vm.currentState.selectedNativeLanguage)
        assertNull(vm.currentState.selectedLevel)
    }

    @Test
    fun `nextStep increments currentStep`() {
        val vm = createViewModel()
        vm.nextStep()
        assertEquals(1, vm.currentState.currentStep)
        vm.nextStep()
        assertEquals(2, vm.currentState.currentStep)
    }

    @Test
    fun `nextStep does not exceed totalSteps`() {
        val vm = createViewModel()
        repeat(10) { vm.nextStep() }
        assertEquals(vm.currentState.totalSteps, vm.currentState.currentStep)
    }

    @Test
    fun `previousStep decrements currentStep`() {
        val vm = createViewModel()
        vm.nextStep()
        vm.nextStep()
        vm.previousStep()
        assertEquals(1, vm.currentState.currentStep)
    }

    @Test
    fun `previousStep does not go below 1`() {
        val vm = createViewModel()
        vm.nextStep()
        vm.previousStep()
        vm.previousStep()
        assertEquals(1, vm.currentState.currentStep)
    }

    @Test
    fun `selectTargetLanguage updates state`() {
        val vm = createViewModel()
        vm.selectTargetLanguage("German")
        assertEquals("German", vm.currentState.selectedTargetLanguage)
    }

    @Test
    fun `selectNativeLanguage updates state`() {
        val vm = createViewModel()
        vm.selectNativeLanguage("English")
        assertEquals("English", vm.currentState.selectedNativeLanguage)
    }

    @Test
    fun `selectLevel updates state`() {
        val vm = createViewModel()
        vm.selectLevel("beginner")
        assertEquals("beginner", vm.currentState.selectedLevel)
    }

    @Test
    fun `submit with valid preferences emits NavigateToPreview`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            vm.selectTargetLanguage("Spanish")
            vm.selectNativeLanguage("English")
            vm.selectLevel("beginner")

            vm.submit()

            val event = vm.effects.first()
            assertIs<OnboardingViewModel.Event.NavigateToPreview>(event)
            assertEquals(false, vm.currentState.isLoading)
            assertEquals(Language.SPANISH, languageSet)
        }

    @Test
    fun `submit with missing selections does not emit`() = runTest {
        val vm = createViewModel()
        vm.selectTargetLanguage("Spanish")
        // Missing native language and level

        vm.submit()

        assertEquals(false, vm.currentState.isLoading)
    }

    @Test
    fun `submit failure sets error state`() = runTest {
        submitResult = Try.failure(RuntimeException("Network error"))
        val vm = createViewModel()
        vm.selectTargetLanguage("Spanish")
        vm.selectNativeLanguage("English")
        vm.selectLevel("beginner")

        vm.submit()

        assertEquals(false, vm.currentState.isLoading)
        assertEquals("Network error", vm.currentState.error)
    }

    @Test
    fun `skip emits NavigateToMain`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()

        vm.skip()

        val event = vm.effects.first()
        assertIs<OnboardingViewModel.Event.NavigateToMain>(event)
    }
}
