package presentation.feature.profile

import domain.auth.manager.IUserManager
import domain.auth.model.AuthUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class ProfileAuthHandler(
    private val userManager: IUserManager,
    private val loginUserEvents: MutableSharedFlow<AuthUser?>,
    private val refreshTrigger: MutableStateFlow<Int>,
    private val streakRefreshTrigger: MutableStateFlow<Int>,
    private val featureAccessRefreshTrigger: MutableStateFlow<Int>,
    private val scope: CoroutineScope
) {
    
    fun loginWithGoogle(idToken: String) {
        scope.launch {
            userManager.loginWithGoogle(idToken).fold(
                onSuccess = { authUser ->
                    handleSuccessfulLogin(authUser)
                },
                onFailure = { error ->
                    error.printStackTrace()
                }
            )
        }
    }
    
    fun loginWithApple(idToken: String, fullName: String?, appleUserId: String) {
        scope.launch {
            userManager.loginWithApple(idToken, fullName, appleUserId).fold(
                onSuccess = { authUser ->
                    handleSuccessfulLogin(authUser)
                },
                onFailure = { error ->
                    error.printStackTrace()
                }
            )
        }
    }
    
    fun logout() {
        scope.launch {
            invalidateAllFlows()
            userManager.logout().fold(
                onSuccess = {
                    triggerAllRefreshes()
                },
                onFailure = { error ->
                    error.printStackTrace()
                    triggerAllRefreshes()
                }
            )
        }
    }
    
    fun deleteAccount() {
        scope.launch {
            invalidateAllFlows()
            userManager.deleteAccount().fold(
                onSuccess = {
                    triggerAllRefreshes()
                },
                onFailure = { error ->
                    error.printStackTrace()
                    triggerAllRefreshes()
                }
            )
        }
    }
    
    private suspend fun handleSuccessfulLogin(user: AuthUser) {
        loginUserEvents.emit(user)
        triggerAllRefreshes()
    }
    
    private suspend fun invalidateAllFlows() {
        loginUserEvents.emit(null)
    }
    
    private fun triggerAllRefreshes() {
        refreshTrigger.value++
        streakRefreshTrigger.value++
        featureAccessRefreshTrigger.value++
    }
}

