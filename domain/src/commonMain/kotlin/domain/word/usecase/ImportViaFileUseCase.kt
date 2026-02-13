package domain.word.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportViaFileUseCase(
    private val importWordsUseCase: ImportWordsUseCase
) {

    suspend operator fun invoke(fileContent: String, fileName: String? = null): ImportResult {
        return withContext(Dispatchers.Default) {
            if (fileContent.isBlank()) {
                return@withContext ImportResult.Error("File is empty")
            }

            fileName?.let {
                val extension = it.substringAfterLast('.', "").lowercase()
                if (extension !in listOf("txt", "text")) {
                    return@withContext ImportResult.Error(
                        "Unsupported file format: .$extension. Please use .txt files only."
                    )
                }
            }

            when (val result = importWordsUseCase.execute(fileContent)) {
                is ImportWordsUseCase.ImportResult.Success -> {
                    ImportResult.Success(result.count)
                }
                is ImportWordsUseCase.ImportResult.Error -> {
                    ImportResult.Error(result.message)
                }
            }
        }
    }

    sealed class ImportResult {
        data class Success(val count: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
