package domain.word.add.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import core.common.onFailure
import core.error.DomainError
import domain.ai.repository.IAiRepository
import domain.subscription.usecase.RefreshFeatureAccessUseCase
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.service.IImagePreparer

/** Reads vocabulary from a photo (premium). Returns drafts for the user to review; nothing is saved. */
class ExtractWordsFromImageUseCase(
    private val imagePreparer: IImagePreparer,
    private val aiRepository: IAiRepository,
    private val refreshFeatureAccess: RefreshFeatureAccessUseCase,
) : UseCase<ExtractWordsFromImageUseCase.Params, List<WordDraft>> {

    class Params(val image: ByteArray, val languages: LanguagePair)

    override suspend fun invoke(params: Params): Try<List<WordDraft>> =
        imagePreparer.prepare(params.image)
            .flatMap { prepared -> aiRepository.extractWords(prepared, params.languages) }
            .flatMap { drafts ->
                if (drafts.isEmpty()) Try.failure(DomainError.AddWords.NothingRecognized) else Try.success(drafts)
            }
            .onFailure { error ->
                // The server says premium lapsed: refresh cached access so the UI locks the feature.
                if (error is DomainError.Commerce.PremiumRequired) refreshFeatureAccess()
            }
}
