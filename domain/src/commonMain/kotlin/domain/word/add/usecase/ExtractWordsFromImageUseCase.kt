package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import core.error.DomainError
import domain.ai.repository.IAiRepository
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.service.IImagePreparer

/**
 * Reads vocabulary from a photo. Spends AI credits server-side (refunded when nothing is found);
 * fails with [DomainError.Commerce.InsufficientCredits] when the balance is short.
 * Returns drafts for the user to review; nothing is saved.
 */
class ExtractWordsFromImageUseCase(
    private val imagePreparer: IImagePreparer,
    private val aiRepository: IAiRepository,
) : UseCase<ExtractWordsFromImageUseCase.Params, List<WordDraft>> {

    /** [quarterTurns]: clockwise 90° turns the user applied in the preview (sideways photos are misread). */
    class Params(val image: ByteArray, val languages: LanguagePair, val quarterTurns: Int = 0)

    override suspend fun invoke(params: Params): Try<List<WordDraft>> =
        imagePreparer.prepare(params.image, params.quarterTurns)
            .flatMap { prepared -> aiRepository.extractWords(prepared, params.languages) }
            .flatMap { drafts ->
                if (drafts.isEmpty()) Try.failure(DomainError.AddWords.NothingRecognized) else Try.success(drafts)
            }
}
