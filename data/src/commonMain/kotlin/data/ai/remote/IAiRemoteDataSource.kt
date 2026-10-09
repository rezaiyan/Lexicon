package data.ai.remote

import core.common.Try
import data.ai.remote.model.ExtractWordsRequest
import data.ai.remote.model.ExtractWordsResponse
import data.ai.remote.model.SuggestWordsRequest
import data.ai.remote.model.SuggestWordsResponse

interface IAiRemoteDataSource {
    suspend fun extractWords(request: ExtractWordsRequest): Try<ExtractWordsResponse>

    suspend fun suggestWords(request: SuggestWordsRequest): Try<SuggestWordsResponse>
}
