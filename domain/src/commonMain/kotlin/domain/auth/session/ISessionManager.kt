package domain.auth.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface ISessionManager {
    val isAuthenticatedFlow: StateFlow<Boolean>
    suspend fun setAuthenticated(isAuthenticated: Boolean)
    suspend fun isAuthenticated(): Boolean
    fun initialize(scope: CoroutineScope)
}
