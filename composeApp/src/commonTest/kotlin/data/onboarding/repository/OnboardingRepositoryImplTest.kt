package data.onboarding.repository

import core.common.Try
import core.common.getOrThrow
import data.onboarding.remote.IOnboardingRemoteDataSource
import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.onboarding.remote.model.SuggestedVocabularyDto
import data.onboarding.remote.model.SuggestedVocabularyResponseDto
import data.storage.SecureStorage
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.ProficiencyLevel
import domain.onboarding.model.SuggestedVocabulary
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import utils.Language

class OnboardingRepositoryImplTest {

    private val remoteDataSource = FakeOnboardingRemoteDataSource()
    private val secureStorage = FakeSecureStorage()

    private fun createRepo() = OnboardingRepositoryImpl(remoteDataSource, secureStorage)

    private val preferences = OnboardingPreferences(
        targetLanguage = Language.GERMAN,
        nativeLanguage = Language.ENGLISH,
        level = ProficiencyLevel.BEGINNER,
        interests = listOf("travel"),
    )

    @Test
    fun `submitPreferences sends display names and a lowercase level`() = runTest {
        remoteDataSource.result = Try.success(responseDto())
        val repo = createRepo()

        repo.submitPreferences(preferences)

        assertEquals(
            OnboardingPreferencesRequest(
                targetLanguage = "German",
                nativeLanguage = "English",
                currentLevel = "beginner",
                interests = listOf("travel"),
            ),
            remoteDataSource.lastRequest,
        )
    }

    @Test
    fun `submitPreferences maps items and labels them with the requested languages`() = runTest {
        remoteDataSource.result = Try.success(responseDto())
        val repo = createRepo()

        val words = repo.submitPreferences(preferences).getOrThrow()

        assertEquals(
            listOf(SuggestedVocabulary("Hallo", "hello", "a greeting", Language.ENGLISH, Language.GERMAN)),
            words,
        )
    }

    @Test
    fun `submitPreferences returns failure on error`() = runTest {
        remoteDataSource.result = Try.failure(RuntimeException("Server error"))
        val repo = createRepo()

        val result = repo.submitPreferences(preferences)

        assertTrue(result.isFailure)
    }

    private fun responseDto() = SuggestedVocabularyResponseDto(
        targetLanguage = "German",
        nativeLanguage = "English",
        currentLevel = "beginner",
        items = listOf(SuggestedVocabularyDto("Hallo", "hello", "a greeting")),
    )

    @Test
    fun `hasCompletedOnboarding delegates to secure storage`() = runTest {
        secureStorage.onboardingCompleted = true
        val repo = createRepo()

        assertTrue(repo.hasCompletedOnboarding().getOrThrow())
    }

    @Test
    fun `hasCompletedOnboarding returns false when not completed`() = runTest {
        secureStorage.onboardingCompleted = false
        val repo = createRepo()

        assertFalse(repo.hasCompletedOnboarding().getOrThrow())
    }

    @Test
    fun `markOnboardingCompleted delegates to secure storage`() = runTest {
        val repo = createRepo()

        repo.markOnboardingCompleted()

        assertTrue(secureStorage.onboardingCompleted)
    }

    @Test
    fun `resetOnboarding clears the completion flag in secure storage`() = runTest {
        secureStorage.onboardingCompleted = true
        val repo = createRepo()

        repo.resetOnboarding()

        assertFalse(secureStorage.onboardingCompleted)
    }

    // --- Fakes ---

    private class FakeOnboardingRemoteDataSource : IOnboardingRemoteDataSource {
        var result: Try<SuggestedVocabularyResponseDto> = Try.failure(RuntimeException("not set"))
        var lastRequest: OnboardingPreferencesRequest? = null

        override suspend fun submitPreferences(
            request: OnboardingPreferencesRequest,
        ): Try<SuggestedVocabularyResponseDto> {
            lastRequest = request
            return result
        }
    }

    private class FakeSecureStorage : SecureStorage {
        var onboardingCompleted = false

        override suspend fun saveAccessToken(token: String) {}
        override suspend fun saveRefreshToken(token: String) {}
        override fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = null
        override suspend fun clearTokens() {}
        override suspend fun saveTokenExpiresAt(expiresAtMs: Long) {}
        override fun getTokenExpiresAt(): Long = 0L
        override suspend fun hasCompletedOnboarding(): Boolean = onboardingCompleted
        override suspend fun markOnboardingCompleted() { onboardingCompleted = true }
        override suspend fun clearOnboardingCompleted() { onboardingCompleted = false }
        override suspend fun savePushToken(token: String) {}
        override fun getPushToken(): String? = null
        override suspend fun clearPushToken() {}
    }
}
