package feature.addwords

import core.error.DomainError
import domain.word.add.model.WordOrigin
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.toProblem
import kotlin.test.Test
import kotlin.test.assertEquals

class AddWordsProblemTest {

    @Test
    fun `a server error on a paid AI source is the AI being unavailable`() {
        val error = DomainError.Network.ServerError(502)

        assertEquals(AddWordsProblem.AiUnavailable, error.toProblem(WordOrigin.Photo))
        assertEquals(AddWordsProblem.AiUnavailable, error.toProblem(WordOrigin.AiSuggestion))
    }

    @Test
    fun `a server error elsewhere stays generic - no credits are involved`() {
        assertEquals(AddWordsProblem.Generic, DomainError.Network.ServerError(500).toProblem(WordOrigin.File))
    }

    @Test
    fun `a timeout is not the same as being offline`() {
        assertEquals(AddWordsProblem.TimedOut, DomainError.Network.Timeout.toProblem(WordOrigin.AiSuggestion))
        assertEquals(AddWordsProblem.Offline, DomainError.Network.NoConnection.toProblem(WordOrigin.AiSuggestion))
    }
}
