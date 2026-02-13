package di

import data.subscription.RevenueCatSubscriptionManager
import domain.subscription.ISubscriptionManager
import org.koin.dsl.module

fun mobileModule() = module {
    single<ISubscriptionManager> { RevenueCatSubscriptionManager() }
}
