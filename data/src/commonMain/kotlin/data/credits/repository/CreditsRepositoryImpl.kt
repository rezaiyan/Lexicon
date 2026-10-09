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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide cache of the balance, tagged with the account it belongs to: after a sign-out or an
 * account switch the old balance is never shown, without auth code having to know about credits.
 * Not persisted: a balance shown offline would be stale and can't be spent anyway.
 *
 * @param scope outlives screens, so a re-read started by [invalidate] finishes after the caller is gone.
 */
class CreditsRepositoryImpl(
    private val remote: ICreditsRemoteDataSource,
    private val userManager: IUserManager,
    private val scope: CoroutineScope,
) : ICreditsRepository {

    private data class Cached(val userId: Long, val balance: CreditBalance)

    private val cache = MutableStateFlow<Cached?>(null)

    // One read at a time: an earlier read answering late must not overwrite a later one.
    private val reads = Mutex()

    override fun observeBalance(): Flow<CreditBalance?> =
        combine(userManager.observeUser(), cache) { user, cached ->
            cached?.takeIf { user != null && it.userId == user.id }?.balance
        }.distinctUntilChanged()

    override suspend fun refresh(): Try<CreditBalance> = reads.withLock {
        val userId = userManager.observeUser().first()?.id
            ?: return@withLock Try.failure(DomainError.Auth.NotAuthenticated)
        // Tagged with the account that asked, so a switch mid-request can't leak this balance
        remote.fetchBalance()
            .map { it.toDomain() }
            .onSuccess { cache.value = Cached(userId, it) }
    }

    override fun invalidate() {
        scope.launch { refresh() }
    }
}
