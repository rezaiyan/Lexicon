package domain.word.usecase

import core.common.Try
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.Language

class ImportViaFileUseCase(
    private val importWordsUseCase: ImportWordsUseCase
) {

    suspend operator fun invoke(
        fileContent: String,
        fileName: String? = null,
        sourceLanguage: Language? = null,
        targetLanguage: Language? = null,
    ): Try<Int> {
        return withContext(Dispatchers.Default) {
            if (fileContent.isBlank()) {
                return@withContext Try.failure(Exception("File is empty"))
            }

            fileName?.let {
                val extension = it.substringAfterLast('.', "").lowercase()
                if (extension !in listOf("txt", "text")) {
                    return@withContext Try.failure(
                        Exception("Unsupported file format: .$extension. Please use .txt files only.")
                    )
                }
            }

            importWordsUseCase.execute(fileContent, sourceLanguage, targetLanguage)
        }
    }
}
