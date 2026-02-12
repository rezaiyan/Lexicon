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
        notificationModule(backendUrl, platform),
        settingsModule(),
        onboardingModule(),
        presentationModule(),
    )
}
