package domain.word.usecase

import core.common.NoParamFlowUseCase
import domain.auth.manager.IUserManager
import domain.auth.usecase.GetFeatureAccessUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Observes whether the image import feature is accessible to the current user.
 *
 * Returns `true` when the user is authenticated AND has premium access.
 * Returns `false` when the user is not authenticated or does not have premium access.
 */
@Suppress("OPT_IN_USAGE")
class ObserveImageImportAccessUseCase(
    private val userManager: IUserManager,
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
) : NoParamFlowUseCase<Boolean> {

    override operator fun invoke(params: Unit): Flow<Boolean> =
        userManager.observeUser()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(false)
                } else {
                    getFeatureAccessUseCase()
                        .map { featureAccess -> featureAccess.userAccess.hasPremiumAccess }
                        .catch { emit(false) }
                }
            }
}
