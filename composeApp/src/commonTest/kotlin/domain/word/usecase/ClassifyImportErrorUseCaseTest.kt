package domain.word.usecase

import domain.word.model.ImportErrorClassification
import kotlin.test.Test
import kotlin.test.assertIs

class ClassifyImportErrorUseCaseTest {

    private val useCase = ClassifyImportErrorUseCase()

    @Test
    fun `timeout message is network error`() {
        assertIs<ImportErrorClassification.NetworkError>(useCase("Connection timeout"))
    }

    @Test
    fun `connect message is network error`() {
        assertIs<ImportErrorClassification.NetworkError>(useCase("Failed to connect to server"))
    }

    @Test
    fun `network message is network error`() {
        assertIs<ImportErrorClassification.NetworkError>(useCase("network unreachable"))
    }

    @Test
    fun `network keywords are case insensitive`() {
        assertIs<ImportErrorClassification.NetworkError>(useCase("TIMEOUT occurred"))
        assertIs<ImportErrorClassification.NetworkError>(useCase("CONNECT failed"))
        assertIs<ImportErrorClassification.NetworkError>(useCase("NETWORK error"))
    }

    @Test
    fun `empty content message is empty content error`() {
        assertIs<ImportErrorClassification.EmptyContent>(useCase("empty response"))
        assertIs<ImportErrorClassification.EmptyContent>(useCase("no words found"))
        assertIs<ImportErrorClassification.EmptyContent>(useCase("no text in image"))
    }

    @Test
    fun `generic message is generic error`() {
        assertIs<ImportErrorClassification.GenericError>(useCase("Something went wrong"))
        assertIs<ImportErrorClassification.GenericError>(useCase("Unknown failure"))
        assertIs<ImportErrorClassification.GenericError>(useCase(""))
    }
}
