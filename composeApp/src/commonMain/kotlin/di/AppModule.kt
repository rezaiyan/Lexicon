package di

import data.notification.remote.model.Platform
import org.koin.core.module.Module
import org.koin.dsl.module

fun appModule(
    backendUrl: String = "",
    platform: Platform
): Module = module {
    includes(
        networkModule(backendUrl),
        authModule(backendUrl),
        wordModule(),
        focusModule(),
        notificationModule(backendUrl, platform),
        ttsModule(),
        listeningModule(),
        addWordsModule(),
        creditsModule(),
        settingsModule(),
        onboardingModule(),
        profileModule(),
        leaderboardModule(),
        presentationModule(),
        analyticsModule(),
    )
}
