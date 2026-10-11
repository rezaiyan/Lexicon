package feature.support.di

import feature.support.SupportViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun supportModule() = module {
    viewModel {
        SupportViewModel(
            observeMessages = get(),
            refreshMessages = get(),
            sendMessage = get(),
            retryMessage = get(),
        )
    }
}
