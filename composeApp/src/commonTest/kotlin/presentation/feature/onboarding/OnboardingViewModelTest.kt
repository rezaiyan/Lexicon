package presentation.feature.onboarding

import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.usecase.SubmitPreferencesUseCase
import domain.onboarding.repository.IOnboardingRepository
import domain.settings.model.ThemeMode
import domain.settings.usecase.SetDailyGoalWordsUseCase
import domain.settings.usecase.SetLanguageUseCase
import domain.settings.repository.ISettingsRepository
import fakes.FakeAnalyticsTracker
import feature.onboarding.OnboardingViewModel
import feature.onboarding.model.OnboardingEffect
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

class OnboardingViewModelTest : ViewModelTestBase() {

    private var submitResult: Try<SuggestedVocabularyResponse> = Try.success(testResponse())
    private var languageSet: Language? = null
    private var dailyGoalSet: Int? = null

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
            override suspend fun submitPreferences(
                preferences: OnboardingPreferences,
            ): Try<SuggestedVocabularyResponse> = submitResult
            override suspend fun hasCompletedOnboarding(): Try<Boolean> = Try.success(false)
            override suspend fun markOnboardingCompleted(): Try<Unit> = Try.success(Unit)
        }
        return SubmitPreferencesUseCase(repo)
    }

    private fun fakeSetLanguageUseCase(): SetLanguageUseCase {
        val repo = object : ISettingsRepository {
            override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
            override suspend fun setLanguage(language: Language): Try<Unit> {
                languageSet = language
                return Try.success(Unit)
            }
            override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
            override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
            override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
            override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
            override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
            override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
            override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
            override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
            override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
            override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
        }
        return SetLanguageUseCase(repo)
    }

    private fun fakeSetDailyGoalWordsUseCase(): SetDailyGoalWordsUseCase {
        val repo = object : ISettingsRepository {
            override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
            override suspend fun setLanguage(language: Language): Try<Unit> = Try.success(Unit)
            override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
            override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
            override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
            override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
            override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
            override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
            override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
            override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
            override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
            override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
            override suspend fun setDailyGoalWords(count: Int): Try<Unit> {
                dailyGoalSet = count
                return Try.success(Unit)
            }
        }
        return SetDailyGoalWordsUseCase(repo)
    }

    private fun createViewModel() = OnboardingViewModel(
        submitPreferencesUseCase = fakeSubmitUseCase(),
        setLanguageUseCase = fakeSetLanguageUseCase(),
        setDailyGoalWordsUseCase = fakeSetDailyGoalWordsUseCase(),
        analyticsTracker = FakeAnalyticsTracker(),
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
            assertIs<OnboardingEffect.NavigateToPreview>(event)
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
        assertIs<OnboardingEffect.NavigateToMain>(event)
    }

    @Test
    fun `selectDailyGoal updates selectedDailyGoal state`() {
        val vm = createViewModel()
        vm.selectDailyGoal(20)
        assertEquals(20, vm.currentState.selectedDailyGoal)
    }

    @Test
    fun `default goal is 10 when not explicitly selected`() {
        val vm = createViewModel()
        assertEquals(10, vm.currentState.selectedDailyGoal)
    }

    @Test
    fun `submit persists the daily goal via SetDailyGoalWordsUseCase`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            vm.selectTargetLanguage("Spanish")
            vm.selectNativeLanguage("English")
            vm.selectLevel("beginner")
            vm.selectDailyGoal(20)

            vm.submit()

            assertEquals(20, dailyGoalSet)
        }
}
