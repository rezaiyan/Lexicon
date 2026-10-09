package data.ai.remote

import data.ai.remote.model.ExtractWordsRequest
import data.ai.remote.model.ExtractWordsResponse
import data.ai.remote.model.SuggestWordsRequest
import data.ai.remote.model.SuggestWordsResponse
import data.core.network.client.ApiClient
import core.common.Try

/** AI endpoints: words from a photo (`/ai/extract-words`) and topic suggestions (`/ai/suggest-vocabulary`). */
class AiRemoteDataSource(
    private val apiClient: ApiClient
) : IAiRemoteDataSource {

    override suspend fun extractWords(request: ExtractWordsRequest): Try<ExtractWordsResponse> =
        apiClient.postNotNull("/ai/extract-words", request)

    override suspend fun suggestWords(request: SuggestWordsRequest): Try<SuggestWordsResponse> =
        apiClient.postNotNull("/ai/suggest-vocabulary", request)
}
