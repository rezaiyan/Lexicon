package data.credits.repository

import core.common.Try
import core.common.map
import core.common.onSuccess
import core.error.DomainError
import data.credits.mapper.toDomain
import data.credits.remote.ICreditsRemoteDataSource
import domain.auth.manager.IUserManager
import domain.credits.ICreditsRepository
import domain.credits.model.CreditBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * Process-wide cache of the balance, tagged with the account it belongs to: after a sign-out or an
 * account switch the old balance is never shown, without auth code having to know about credits.
 * Not persisted: a balance shown offline would be stale and can't be spent anyway.
 */
class CreditsRepositoryImpl(
    private val remote: ICreditsRemoteDataSource,
    private val userManager: IUserManager,
) : ICreditsRepository {

    private data class Cached(val userId: Long, val balance: CreditBalance)

    private val cache = MutableStateFlow<Cached?>(null)

    override fun observeBalance(): Flow<CreditBalance?> =
        combine(userManager.observeUser(), cache) { user, cached ->
            cached?.takeIf { user != null && it.userId == user.id }?.balance
        }.distinctUntilChanged()

    override suspend fun refresh(): Try<CreditBalance> {
        val userId = userManager.observeUser().first()?.id
            ?: return Try.failure(DomainError.Auth.NotAuthenticated)
        // Tagged with the account that asked, so a switch mid-request can't leak this balance
        return remote.fetchBalance()
            .map { it.toDomain() }
            .onSuccess { cache.value = Cached(userId, it) }
    }
}
