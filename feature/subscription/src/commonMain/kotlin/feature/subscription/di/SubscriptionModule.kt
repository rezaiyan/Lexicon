package feature.subscription.di

import feature.subscription.SubscriptionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun subscriptionModule() = module {
    viewModel {
        SubscriptionViewModel(
            subscriptionManager = get()
        )
    }
}
