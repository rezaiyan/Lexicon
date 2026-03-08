package presentation.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface TabDestination {
    @Serializable
    data object Study : TabDestination

    @Serializable
    data object Settings : TabDestination
}
