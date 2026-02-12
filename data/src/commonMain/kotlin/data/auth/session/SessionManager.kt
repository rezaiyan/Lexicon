package data.auth.session

import data.auth.state.IAuthenticationStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface ISessionManager {
    val isAuthenticatedFlow: StateFlow<Boolean>
    suspend fun setAuthenticated(isAuthenticated: Boolean)
    suspend fun isAuthenticated(): Boolean
    fun initialize(scope: CoroutineScope)
}

class SessionManager(
    private val authenticationStateManager: IAuthenticationStateManager
) : ISessionManager {

    override val isAuthenticatedFlow: StateFlow<Boolean> = 
        authenticationStateManager.isAuthenticatedFlow

    override suspend fun setAuthenticated(isAuthenticated: Boolean) {
        authenticationStateManager.setAuthenticated(isAuthenticated)
    }

    override suspend fun isAuthenticated(): Boolean {
        return authenticationStateManager.isAuthenticated()
    }

    override fun initialize(scope: CoroutineScope) {
        authenticationStateManager.initialize(scope)
    }
}

