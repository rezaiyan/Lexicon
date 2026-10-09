package data.credits.remote

import core.common.Try
import data.core.network.client.ApiClient
import data.credits.remote.model.CreditBalanceDto

interface ICreditsRemoteDataSource {
    suspend fun fetchBalance(): Try<CreditBalanceDto>
}

/** AI credit balance (`GET /credits`). Spending happens server-side inside the AI endpoints. */
class CreditsRemoteDataSource(
    private val apiClient: ApiClient,
) : ICreditsRemoteDataSource {

    override suspend fun fetchBalance(): Try<CreditBalanceDto> = apiClient.getNotNull(CREDITS_PATH)

    private companion object {
        const val CREDITS_PATH = "/credits"
    }
}
