package domain.credits

import core.common.Try
import domain.credits.model.CreditBalance
import kotlinx.coroutines.flow.Flow

/**
 * AI credit balance. Server-authoritative by design (an exception to offline-first): credits are
 * only spent by server calls, so there is nothing to do with them offline.
 */
interface ICreditsRepository {
    /**
     * The signed-in user's last known balance; null while unknown (not loaded yet, signed out, or
     * never reachable). Never shows one account's balance to another.
     */
    fun observeBalance(): Flow<CreditBalance?>

    /** Re-reads the balance from the server. On failure the last known value is kept. */
    suspend fun refresh(): Try<CreditBalance>
}
