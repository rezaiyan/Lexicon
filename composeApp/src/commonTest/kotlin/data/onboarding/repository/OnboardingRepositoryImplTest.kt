package data.onboarding.repository

import core.common.Try
import core.common.getOrThrow
import data.onboarding.remote.IOnboardingRemoteDataSource
import data.onboarding.remote.model.OnboardingPreferencesRequest
import data.onboarding.remote.model.SuggestedVocabularyDto
import data.onboarding.remote.model.SuggestedVocabularyResponseDto
import data.storage.SecureStorage
import domain.onboarding.model.OnboardingPreferences
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingRepositoryImplTest {

    private val remoteDataSource = FakeOnboardingRemoteDataSource()
    private val secureStorage = FakeSecureStorage()

    private fun createRepo() = OnboardingRepositoryImpl(remoteDataSource, secureStorage)

    @Test
    fun `submitPreferences maps response to domain model`() = runTest {
        remoteDataSource.result = Try.success(
            SuggestedVocabularyResponseDto(
                targetLanguage = "de",
                nativeLanguage = "en",
                currentLevel = "beginner",
                items = listOf(
                    SuggestedVocabularyDto("Hallo", "hello", "a greeting")
                )
            )
        )
        val repo = createRepo()

        val result = repo.submitPreferences(
            OnboardingPreferences(
                targetLanguage = "de",
                nativeLanguage = "en",
                level = "beginner",
                interests = listOf("travel")
            )
        )

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals("de", response.targetLanguage)
        assertEquals("en", response.nativeLanguage)
        assertEquals(1, response.suggestedVocabulary.size)
        assertEquals("Hallo", response.suggestedVocabulary.first().originalWord)
        assertEquals("hello", response.suggestedVocabulary.first().translation)
    }

    @Test
    fun `submitPreferences returns failure on error`() = runTest {
        remoteDataSource.result = Try.failure(RuntimeException("Server error"))
        val repo = createRepo()

        val result = repo.submitPreferences(
            OnboardingPreferences("de", "en", "beginner", emptyList())
        )

        assertTrue(result.isFailure)
    }

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

    // --- Fakes ---

    private class FakeOnboardingRemoteDataSource : IOnboardingRemoteDataSource {
        var result: Try<SuggestedVocabularyResponseDto> = Try.failure(RuntimeException("not set"))

        override suspend fun submitPreferences(
            request: OnboardingPreferencesRequest,
        ): Try<SuggestedVocabularyResponseDto> = result
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
        override suspend fun savePushToken(token: String) {}
        override fun getPushToken(): String? = null
        override suspend fun clearPushToken() {}
    }
}
