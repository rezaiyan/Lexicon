package domain.auth.repository

import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication
 */
interface IAuthRepository {
    /**
     * Login with Google ID token
     */
    suspend fun loginWithGoogle(idToken: String): Result<AuthUser>

    /**
     * Login with Apple ID token
     */
    suspend fun loginWithApple(idToken: String, fullName: String? = null, appleUserId: String): Result<AuthUser>
    
    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Delete user account permanently
     */
    suspend fun deleteAccount(): Result<Unit>
    
    /**
     * Get current access token
     */
    suspend fun getAccessToken(): String?

    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Reactive authentication state
     * Emits true when authenticated, false otherwise. Distinct until changed.
     */
    fun isAuthenticatedAsFlow(): Flow<Boolean>
    
    /**
     * Get feature access as Flow
     * Returns user's feature flags and access status
     */
    fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse>
}


