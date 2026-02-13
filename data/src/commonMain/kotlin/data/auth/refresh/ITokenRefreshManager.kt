package data.auth.refresh

import domain.common.Try

/**
 * Interface for token refresh manager with single-flight refresh support
 */
interface ITokenRefreshManager {
    /**
     * Refreshes access and refresh tokens.
     * Uses single-flight pattern: concurrent callers are coalesced.
     * On success, saves new tokens.
     * On auth rejection (401/403), clears tokens and logs out.
     * On transient errors (network/server), returns failure without clearing tokens.
     * @return Try with new access token on success, or error on failure
     */
    suspend fun refresh(): Try<String>

    /**
     * Clears tokens and sets authentication state to false.
     * Called when a retried request is still rejected after a successful token refresh,
     * indicating the account itself is invalid (deleted, banned, etc.).
     */
    suspend fun clearSession()
}
