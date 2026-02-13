package presentation.feature.profile

import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.streak.model.StreakData
import presentation.model.ProfileUiData
import presentation.model.ProfileUserUiModel
import presentation.model.UiState

internal object ProfileStateBuilder {
    
    fun createUiState(
        user: AuthUser?,
        streak: UiState<StreakData>,
        featureAccessState: UiState<FeatureAccessResponse?>
    ): UiState<ProfileUiData> {
        return when {
            user == null -> createUnauthenticatedState()
            featureAccessState is UiState.Loading -> UiState.Loading
            streak is UiState.Error -> UiState.Error(streak.message)
            else -> {
                val featureAccess = (featureAccessState as? UiState.Loaded)?.value
                createLoadedState(user, streak, featureAccess)
            }
        }
    }
    
    private fun createUnauthenticatedState(): UiState.Loaded<ProfileUiData> {
        return UiState.Loaded(
            ProfileUiData(
                userInfo = null,
                streak = null,
                featureAccess = null,
                isSubscriptionsEnabled = false,
                shouldShowSubscriptionUI = false
            )
        )
    }
    
    private fun createLoadedState(
        user: AuthUser,
        streak: UiState<StreakData>,
        featureAccess: FeatureAccessResponse?
    ): UiState.Loaded<ProfileUiData> {
        val streakData = when (streak) {
            is UiState.Loaded -> streak.value
            else -> null
        }

        val hasPremiumAccess = featureAccess?.userAccess?.hasPremiumAccess == true

        return UiState.Loaded(
            ProfileUiData(
                userInfo = user.toProfileUserUiModel(),
                streak = streakData,
                featureAccess = featureAccess,
                isSubscriptionsEnabled = !hasPremiumAccess,
                shouldShowSubscriptionUI = !hasPremiumAccess
            )
        )
    }
    
    private fun AuthUser.toProfileUserUiModel(): ProfileUserUiModel {
        return ProfileUserUiModel(
            name = this.name,
            email = this.email
        )
    }
}



