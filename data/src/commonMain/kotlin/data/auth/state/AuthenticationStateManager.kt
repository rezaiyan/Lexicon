package data.auth.state

import data.auth.token.ITokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Single source of truth for authentication state.
 * Provides atomic state transitions and ensures data consistency.
 */
interface IAuthenticationStateManager {
    /**
     * Flow of authentication state. Emits true when authenticated, false otherwise.
     * Distinct until changed to avoid unnecessary emissions.
     */
    val isAuthenticatedFlow: StateFlow<Boolean>

    /**
     * Current authentication state (synchronous check).
     * Use with caution - prefer isAuthenticatedFlow for reactive updates.
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Atomically sets authenticated state.
     * Should only be called after tokens are saved/cleared and data cleanup is complete.
     */
    suspend fun setAuthenticated(isAuthenticated: Boolean)

    /**
     * Initializes the state manager by checking token existence.
     * Must be called before first use. Uses coroutines properly (no runBlocking).
     */
    fun initialize(scope: CoroutineScope)
}

class AuthenticationStateManager(
    private val tokenManager: ITokenManager
) : IAuthenticationStateManager {

    private val _isAuthenticatedState = MutableStateFlow(false)
    
    override val isAuthenticatedFlow: StateFlow<Boolean> = 
        _isAuthenticatedState.asStateFlow()

    override suspend fun isAuthenticated(): Boolean {
        return _isAuthenticatedState.value
    }

    override suspend fun setAuthenticated(isAuthenticated: Boolean) {
        _isAuthenticatedState.value = isAuthenticated
    }

    override fun initialize(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            val hasTokens = tokenManager.hasTokens()
            _isAuthenticatedState.value = hasTokens
        }
    }
}
