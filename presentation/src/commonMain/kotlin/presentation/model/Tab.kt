package presentation.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface TabDestination {
    @Serializable
    data object Profile : TabDestination
    
    @Serializable
    data object Study : TabDestination
    
    @Serializable
    data object Settings : TabDestination
    
    @Serializable
    data object WordManager : TabDestination
    
    @Serializable
    data object Subscription : TabDestination

    @Serializable
    data object Leaderboard : TabDestination

    @Serializable
    data object EditProfile : TabDestination
}
