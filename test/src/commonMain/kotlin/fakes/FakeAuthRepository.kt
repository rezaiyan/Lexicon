package fakes

import core.common.Try
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAuthRepository : IAuthRepository {
    var authenticated = false
    var authenticatedFlow: Flow<Boolean> = flowOf(false)
    var shouldThrow = false
    var loginCallCount = 0
    var logoutCallCount = 0
    var deleteAccountCallCount = 0
    var featureAccessFlow: Flow<FeatureAccessResponse> = flowOf()

    override suspend fun isAuthenticated(): Boolean {
        if (shouldThrow) throw RuntimeException("Auth check failed")
        return authenticated
    }

    override fun isAuthenticatedAsFlow(): Flow<Boolean> = authenticatedFlow

    override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> {
        loginCallCount++
        return Try.success(AuthUser(1L, "test@test.com", "Test"))
    }

    override suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser> {
        loginCallCount++
        return Try.success(AuthUser(1L, "test@test.com", "Test"))
    }

    override suspend fun logout(): Try<Unit> {
        logoutCallCount++
        return Try.success(Unit)
    }

    override suspend fun deleteAccount(): Try<Unit> {
        deleteAccountCallCount++
        return Try.success(Unit)
    }

    override suspend fun getAccessToken(): String? = "fake-token"

    override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = featureAccessFlow
}
