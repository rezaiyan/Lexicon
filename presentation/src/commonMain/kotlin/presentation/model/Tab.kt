package presentation.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface TabDestination {
    @Serializable
    data object Study : TabDestination

    @Serializable
    data object Words : TabDestination
}

/** Pushed on top of the Study tab from its top-bar gear; not a tab itself. */
@Serializable
data object SettingsRoute
