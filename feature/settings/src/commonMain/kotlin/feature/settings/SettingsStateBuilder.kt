package feature.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import feature.settings.model.SettingsScreenState
import domain.settings.model.ThemeMode

/**
 * Builder for Settings UI state
 * Combines individual setting flows into SettingsScreenState
 * All upstream flows handle errors gracefully via Flow operators
 */
internal object SettingsStateBuilder {

    fun buildStateFlow(
        themeMode: Flow<ThemeMode>,
        notificationsEnabled: Flow<Boolean>,
        systemNotificationsEnabled: Flow<Boolean>,
        appVersion: Flow<String>,
        reviewRemindersEnabled: Flow<Boolean>,
    ): Flow<SettingsScreenState> {
        return combine(
            themeMode.catch { emit(ThemeMode.AUTO) },
            notificationsEnabled.catch { emit(true) },
            systemNotificationsEnabled.catch { emit(true) },
            appVersion.catch { emit("Unknown") },
            reviewRemindersEnabled.catch { emit(true) },
        ) { values: Array<Any?> ->
            val mode = values[0] as ThemeMode
            val appNotifications = values[1] as Boolean
            val systemNotifications = values[2] as Boolean
            val version = values[3] as String
            val reviewReminders = values[4] as Boolean

            SettingsScreenState(
                themeMode = mode,
                notificationsEnabled = appNotifications,
                systemNotificationsEnabled = systemNotifications,
                reviewRemindersEnabled = reviewReminders,
                appVersion = version,
            )
        }
    }
}
