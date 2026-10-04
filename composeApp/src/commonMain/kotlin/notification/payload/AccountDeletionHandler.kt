package notification.payload

import domain.auth.usecase.ClearAllUserDataUseCase
import domain.onboarding.repository.IOnboardingRepository

class AccountDeletionHandler(
    private val clearAllUserDataUseCase: ClearAllUserDataUseCase,
    private val onboardingRepository: IOnboardingRepository,
) : NotificationPayloadHandler {

    override val type: String = "account_deleted"

    override suspend fun handle(data: Map<String, String>) {
        clearAllUserDataUseCase()
        onboardingRepository.resetOnboarding()
    }
}

