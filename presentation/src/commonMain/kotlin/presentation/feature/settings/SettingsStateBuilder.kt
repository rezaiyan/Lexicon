package presentation.feature.settings

import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import presentation.model.SettingsScreenState
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
        successesToAdvance: Flow<Int>,
        forgotPenalty: Flow<Int>,
        appVersion: Flow<String>,
        featureAccessFlow: Flow<FeatureAccessResponse>,
    ): Flow<SettingsScreenState> {
        return combine(
            currentLanguage.catch { emit(Language.ENGLISH) },
            themeMode.catch { emit(ThemeMode.AUTO) },
            notificationsEnabled.catch { emit(true) },
            systemNotificationsEnabled.catch { emit(true) },
            successesToAdvance.catch { emit(1) },
            forgotPenalty.catch { emit(2) },
            appVersion.catch { emit("Unknown") },
            featureAccessFlow.catch { emit(defaultFeatureAccess()) },
        ) { values: Array<Any?> ->
            val language = values[0] as Language
            val mode = values[1] as ThemeMode
            val appNotifications = values[2] as Boolean
            val systemNotifications = values[3] as Boolean
            val successes = values[4] as Int
            val penalty = values[5] as Int
            val version = values[6] as String
            val featureAccess = values[7] as FeatureAccessResponse

            SettingsScreenState(
                currentLanguage = language,
                themeMode = mode,
                notificationsEnabled = appNotifications,
                systemNotificationsEnabled = systemNotifications,
                successesToAdvance = successes,
                forgotPenalty = penalty,
                appVersion = version,
                isPremiumFeatureEnabled = featureAccess.featureFlags.premiumFeaturesEnabled,
            )
        }
    }
    
    private fun defaultFeatureAccess(): FeatureAccessResponse {
        return FeatureAccessResponse(
            featureFlags = FeatureFlags(
                premiumFeaturesEnabled = false,
                subscriptionsEnabled = false,
                aiImageExtractionEnabled = false,
                aiDailyInsightEnabled = false,
                pushNotificationsEnabled = true
            ),
            userAccess = UserFeatureAccess(
                hasPremiumAccess = false,
                canUseAiImageExtraction = false,
                canUseAiDailyInsight = false,
                subscriptionStatus = "FREE",
                subscriptionExpiresAt = null,
                aiExtractionUsageCount = 0,
                aiExtractionUsageLimit = 10,
                remainingAiExtractions = 10
            )
        )
    }
}

