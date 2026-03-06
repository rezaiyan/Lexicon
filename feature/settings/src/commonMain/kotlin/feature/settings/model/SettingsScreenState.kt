package feature.settings.model

import domain.settings.model.ThemeMode
import utils.Language

data class SettingsScreenState(
    val currentLanguage: Language = Language.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val notificationsEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val isPremiumFeatureEnabled: Boolean = false,
    val appVersion: String = "Loading.."
)
