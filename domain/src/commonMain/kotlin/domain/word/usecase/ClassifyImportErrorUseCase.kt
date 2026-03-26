package domain.word.usecase

import domain.word.model.ImportErrorClassification

class ClassifyImportErrorUseCase {

    operator fun invoke(rawMessage: String): ImportErrorClassification {
        val msg = rawMessage.lowercase()
        return when {
            msg.contains("timeout") || msg.contains("connect") || msg.contains("network") ->
                ImportErrorClassification.NetworkError
            msg.contains("empty") || msg.contains("no words") || msg.contains("no text") ->
                ImportErrorClassification.EmptyContent
            else -> ImportErrorClassification.GenericError
        }
    }
}
