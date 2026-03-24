package domain.word.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import core.common.fold
import core.common.getOrDefault
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.repository.IWordRepository
import domain.word.service.IImportValidationService
import utils.Language

class ImportWordsUseCase(
    private val wordRepository: IWordRepository,
    private val validationService: IImportValidationService,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase
) : UseCase<ImportWordsUseCase.Params, Int> {

    data class Params(
        val text: String,
        val sourceLanguage: Language? = null,
        val targetLanguage: Language? = null,
        val tagId: Long? = null,
    )

    override suspend operator fun invoke(params: Params): Try<Int> =
        invoke(params.text, params.sourceLanguage, params.targetLanguage, params.tagId)

    suspend operator fun invoke(
        text: String,
        sourceLanguage: Language? = null,
        targetLanguage: Language? = null,
        tagId: Long? = null,
    ): Try<Int> {
        val resolvedSourceLanguage = sourceLanguage ?: Language.ENGLISH
        val resolvedTargetLanguage = targetLanguage ?: getCurrentLanguageUseCase().getOrDefault(Language.ENGLISH)
        return validationService.validateAndParse(
            text = text,
            sourceLanguage = resolvedSourceLanguage,
            targetLanguage = resolvedTargetLanguage
        ).fold(
            onSuccess = { parsedWords ->
                val tagIds = if (tagId != null) listOf(tagId) else emptyList()
                val importWords = parsedWords
                    .distinctBy { Pair(it.originalWord.trim().lowercase(), it.translation.trim().lowercase()) }
                    .map { it.copy(tagIds = tagIds) }

                wordRepository.insertWords(importWords).flatMap { importedCount ->
                    if (importedCount > 0) {
                        Try.success(importedCount)
                    } else {
                        Try.failure(Exception("All ${importWords.size} word(s) already exist in your collection."))
                    }
                }
            },
            onFailure = { throwable ->
                Try.failure(throwable)
            }
        )
    }

}
