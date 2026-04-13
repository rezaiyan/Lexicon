package feature.settings

import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import feature.settings.model.SettingsScreenState
import domain.settings.model.ThemeMode
import utils.Language

/**
 * Builder for Settings UI state
 * Combines individual setting flows into SettingsScreenState
 * All upstream flows handle errors gracefully via Flow operators
 */
internal object SettingsStateBuilder {

    fun buildStateFlow(
        currentLanguage: Flow<Language>,
        themeMode: Flow<ThemeMode>,
        notificationsEnabled: Flow<Boolean>,
        systemNotificationsEnabled: Flow<Boolean>,
        appVersion: Flow<String>,
        featureAccessFlow: Flow<FeatureAccessResponse>,
        reviewRemindersEnabled: Flow<Boolean>,
    ): Flow<SettingsScreenState> {
        return combine(
            currentLanguage.catch { emit(Language.ENGLISH) },
            themeMode.catch { emit(ThemeMode.AUTO) },
            notificationsEnabled.catch { emit(true) },
            systemNotificationsEnabled.catch { emit(true) },
            appVersion.catch { emit("Unknown") },
            featureAccessFlow.catch { emit(defaultFeatureAccess()) },
            reviewRemindersEnabled.catch { emit(true) },
        ) { values: Array<Any?> ->
            val language = values[0] as Language
            val mode = values[1] as ThemeMode
            val appNotifications = values[2] as Boolean
            val systemNotifications = values[3] as Boolean
            val version = values[4] as String
            val featureAccess = values[5] as FeatureAccessResponse
            val reviewReminders = values[6] as Boolean

            SettingsScreenState(
                currentLanguage = language,
                themeMode = mode,
                notificationsEnabled = appNotifications,
                systemNotificationsEnabled = systemNotifications,
                reviewRemindersEnabled = reviewReminders,
                appVersion = version,
                isPremiumFeatureEnabled = featureAccess.userAccess.hasPremiumAccess,
            )
        }
    }

    private fun defaultFeatureAccess(): FeatureAccessResponse {
        return FeatureAccessResponse(
            featureFlags = FeatureFlags(pushNotificationsEnabled = true),
            userAccess = UserFeatureAccess(hasPremiumAccess = false)
        )
    }
}
