package presentation.feature.onboarding

import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.ProficiencyLevel
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import domain.onboarding.usecase.SubmitPreferencesUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.SetDailyGoalWordsUseCase
import fakes.FakeAnalyticsTracker
import feature.onboarding.DeviceLanguageProvider
import feature.onboarding.OnboardingViewModel
import feature.onboarding.model.DailyGoalOption
import feature.onboarding.model.OnboardingEffect
import feature.onboarding.model.OnboardingStep
import feature.onboarding.model.OnboardingSubmission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingViewModelTest : ViewModelTestBase() {

    private var submitResult: Try<List<SuggestedVocabulary>> = Try.success(testResponse())
    private var submitGate: CompletableDeferred<Unit>? = null
    private var submittedPreferences: OnboardingPreferences? = null
    private var submitCount = 0
    private var dailyGoalSet: Int? = null

    private fun testResponse() = listOf(
        SuggestedVocabulary("Hallo", "hello", "greeting", Language.ENGLISH, Language.GERMAN),
    )

    private val onboardingRepository = object : IOnboardingRepository {
        override suspend fun submitPreferences(
            preferences: OnboardingPreferences,
        ): Try<List<SuggestedVocabulary>> {
            submitCount++
            submittedPreferences = preferences
            submitGate?.await()
            return submitResult
        }
        override suspend fun hasCompletedOnboarding(): Try<Boolean> = Try.success(false)
        override suspend fun markOnboardingCompleted(): Try<Unit> = Try.success(Unit)
        override suspend fun resetOnboarding(): Try<Unit> = Try.success(Unit)
    }

    private val settingsRepository = object : ISettingsRepository {
        override suspend fun setDailyGoalWords(count: Int): Try<Unit> {
            dailyGoalSet = count
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

    private fun createViewModel(deviceLanguage: String? = "en") = OnboardingViewModel(
        submitPreferencesUseCase = SubmitPreferencesUseCase(onboardingRepository),
        setDailyGoalWordsUseCase = SetDailyGoalWordsUseCase(settingsRepository),
        analyticsTracker = FakeAnalyticsTracker(),
        deviceLanguageProvider = DeviceLanguageProvider { deviceLanguage },
    )

    /** Walks Welcome → DailyGoal answering German / English / Intermediate. */
    private fun OnboardingViewModel.answerAll() {
        next()
        selectTargetLanguage(Language.GERMAN)
        next()
        selectNativeLanguage(Language.ENGLISH)
        next()
        selectLevel(ProficiencyLevel.INTERMEDIATE)
        next()
    }

    // Initial state

    @Test
    fun `init when device language is supported starts on welcome with native prefilled`() {
        val vm = createViewModel(deviceLanguage = "en")

        assertEquals(OnboardingStep.Welcome, vm.currentState.step)
        assertNull(vm.currentState.targetLanguage)
        assertEquals(Language.ENGLISH, vm.currentState.deviceLanguage)
        assertEquals(Language.ENGLISH, vm.currentState.nativeLanguage)
        assertEquals(DailyGoalOption.Regular, vm.currentState.dailyGoal)
        assertEquals(OnboardingSubmission.Idle, vm.currentState.submission)
    }

    @Test
    fun `init when device language is unsupported leaves native empty`() {
        val vm = createViewModel(deviceLanguage = "hi")

        assertNull(vm.currentState.deviceLanguage)
        assertNull(vm.currentState.nativeLanguage)
    }

    @Test
    fun `init when device language is unknown leaves native empty`() {
        val vm = createViewModel(deviceLanguage = null)

        assertNull(vm.currentState.nativeLanguage)
    }

    // Navigation

    @Test
    fun `next from welcome shows the target language question`() {
        val vm = createViewModel()

        vm.next()

        assertEquals(OnboardingStep.TargetLanguage, vm.currentState.step)
    }

    @Test
    fun `next when the question is unanswered stays on the step`() {
        val vm = createViewModel()
        vm.next()

        vm.next()

        assertEquals(OnboardingStep.TargetLanguage, vm.currentState.step)
        assertFalse(vm.currentState.canContinue)
    }

    @Test
    fun `next walks every question in order`() {
        val vm = createViewModel()

        vm.answerAll()

        assertEquals(OnboardingStep.DailyGoal, vm.currentState.step)
        assertEquals(OnboardingStep.DailyGoal.questionNumber, OnboardingStep.QuestionCount)
    }

    @Test
    fun `back from the first question returns to welcome`() {
        val vm = createViewModel()
        vm.next()

        vm.back()

        assertEquals(OnboardingStep.Welcome, vm.currentState.step)
    }

    @Test
    fun `back on welcome does nothing`() {
        val vm = createViewModel()

        vm.back()

        assertEquals(OnboardingStep.Welcome, vm.currentState.step)
    }

    @Test
    fun `back keeps earlier answers`() {
        val vm = createViewModel()
        vm.answerAll()

        vm.back()
        vm.back()

        assertEquals(OnboardingStep.NativeLanguage, vm.currentState.step)
        assertEquals(Language.GERMAN, vm.currentState.targetLanguage)
        assertEquals(ProficiencyLevel.INTERMEDIATE, vm.currentState.level)
    }

    // Language rules

    @Test
    fun `selectTargetLanguage when it equals the native language clears native`() {
        val vm = createViewModel(deviceLanguage = "en")

        vm.selectTargetLanguage(Language.ENGLISH)

        assertNull(vm.currentState.nativeLanguage)
        assertNull(vm.currentState.nativeSuggestion)
    }

    @Test
    fun `selectTargetLanguage when changed away again restores the phone language as native`() {
        val vm = createViewModel(deviceLanguage = "en")
        vm.selectTargetLanguage(Language.ENGLISH)

        vm.selectTargetLanguage(Language.GERMAN)

        assertEquals(Language.ENGLISH, vm.currentState.nativeLanguage)
    }

    @Test
    fun `selectTargetLanguage keeps a native language the user picked`() {
        val vm = createViewModel(deviceLanguage = "en")
        vm.selectNativeLanguage(Language.PERSIAN)

        vm.selectTargetLanguage(Language.GERMAN)

        assertEquals(Language.PERSIAN, vm.currentState.nativeLanguage)
    }

    @Test
    fun `otherNativeLanguages excludes the target and the suggestion`() {
        val vm = createViewModel(deviceLanguage = "en")

        vm.selectTargetLanguage(Language.GERMAN)

        val others = vm.currentState.otherNativeLanguages
        assertFalse(Language.GERMAN in others)
        assertFalse(Language.ENGLISH in others)
        assertEquals(vm.currentState.languages.size - 2, others.size)
    }

    // Daily goal

    @Test
    fun `selectDailyGoal updates goal and monthly estimate`() {
        val vm = createViewModel()

        vm.selectDailyGoal(DailyGoalOption.Serious)

        assertEquals(DailyGoalOption.Serious, vm.currentState.dailyGoal)
        assertEquals(600, vm.currentState.monthlyWords)
    }

    // Submission

    @Test
    fun `next on the last question submits the typed answers`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        vm.answerAll()
        vm.selectDailyGoal(DailyGoalOption.Serious)

        vm.next()

        val effect = assertIs<OnboardingEffect.NavigateToPreview>(vm.effects.first())
        assertEquals(testResponse(), effect.words)
        assertEquals(
            OnboardingPreferences(Language.GERMAN, Language.ENGLISH, ProficiencyLevel.INTERMEDIATE),
            submittedPreferences,
        )
        assertEquals(20, dailyGoalSet)
    }

    @Test
    fun `submission success keeps the progress screen until navigation`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        vm.answerAll()

        vm.next()

        assertEquals(OnboardingSubmission.InProgress, vm.currentState.submission)
    }

    @Test
    fun `submission failure shows the error`() = runTest {
        submitResult = Try.failure(RuntimeException("Network error"))
        val vm = createViewModel()
        vm.answerAll()

        vm.next()

        assertEquals(OnboardingSubmission.Failed("Network error"), vm.currentState.submission)
        assertNull(dailyGoalSet)
    }

    @Test
    fun `back after a failure returns to the last question`() = runTest {
        submitResult = Try.failure(RuntimeException("Network error"))
        val vm = createViewModel()
        vm.answerAll()
        vm.next()

        vm.back()

        assertEquals(OnboardingSubmission.Idle, vm.currentState.submission)
        assertEquals(OnboardingStep.DailyGoal, vm.currentState.step)
    }

    @Test
    fun `retry after a failure submits again`() = runTest(UnconfinedTestDispatcher()) {
        submitResult = Try.failure(RuntimeException("Network error"))
        val vm = createViewModel()
        vm.answerAll()
        vm.next()
        submitResult = Try.success(testResponse())

        vm.retry()

        assertIs<OnboardingEffect.NavigateToPreview>(vm.effects.first())
        assertEquals(2, submitCount)
    }

    @Test
    fun `while submitting back and next are ignored`() = runTest {
        submitGate = CompletableDeferred()
        val vm = createViewModel()
        vm.answerAll()
        vm.next()

        vm.back()
        vm.next()

        assertEquals(OnboardingSubmission.InProgress, vm.currentState.submission)
        assertEquals(OnboardingStep.DailyGoal, vm.currentState.step)
        assertEquals(1, submitCount)
        submitGate?.complete(Unit)
    }

    @Test
    fun `retry without answers does not submit`() = runTest {
        val vm = createViewModel()

        vm.retry()

        assertEquals(0, submitCount)
        assertEquals(OnboardingSubmission.Idle, vm.currentState.submission)
    }

    @Test
    fun `skip emits NavigateToMain`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()

        vm.skip()

        assertIs<OnboardingEffect.NavigateToMain>(vm.effects.first())
    }

    @Test
    fun `canContinue on welcome and daily goal needs no answer`() {
        val vm = createViewModel()
        assertTrue(vm.currentState.canContinue)

        vm.answerAll()

        assertTrue(vm.currentState.canContinue)
    }
}
