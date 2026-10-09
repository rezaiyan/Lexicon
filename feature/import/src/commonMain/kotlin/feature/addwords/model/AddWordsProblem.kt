package feature.addwords.model

import core.error.DomainError
import core.error.DomainError.AddWords.InvalidDraft.Reason
import domain.word.add.model.WordOrigin

/** What went wrong, in user terms. The UI maps each case to a localized message. */
enum class AddWordsProblem {
    Offline,
    PremiumRequired,
    FileTooLarge,
    UnsupportedFile,
    EmptyFile,
    NothingInFile,
    NothingInPhoto,
    NothingSuggested,
    ImageUnreadable,
    ImageTooLarge,
    MissingTerm,
    MissingTranslation,
    TooLong,
    Generic,
}

fun Throwable.toProblem(origin: WordOrigin): AddWordsProblem = when (this) {
    is DomainError.Network.NoConnection, is DomainError.Network.Timeout -> AddWordsProblem.Offline
    is DomainError.Commerce.PremiumRequired -> AddWordsProblem.PremiumRequired
    is DomainError.AddWords.FileTooLarge -> AddWordsProblem.FileTooLarge
    is DomainError.AddWords.UnsupportedFile -> AddWordsProblem.UnsupportedFile
    is DomainError.AddWords.EmptyInput -> AddWordsProblem.EmptyFile
    is DomainError.AddWords.ImageUnreadable -> AddWordsProblem.ImageUnreadable
    is DomainError.AddWords.ImageTooLarge -> AddWordsProblem.ImageTooLarge
    is DomainError.AddWords.NothingRecognized -> origin.nothingFound()
    is DomainError.AddWords.InvalidDraft -> reason.toProblem()
    else -> AddWordsProblem.Generic
}

private fun WordOrigin.nothingFound() = when (this) {
    WordOrigin.Photo -> AddWordsProblem.NothingInPhoto
    WordOrigin.AiSuggestion -> AddWordsProblem.NothingSuggested
    else -> AddWordsProblem.NothingInFile
}

private fun Reason.toProblem() = when (this) {
    Reason.BlankTerm -> AddWordsProblem.MissingTerm
    Reason.BlankTranslation -> AddWordsProblem.MissingTranslation
    Reason.TooLong -> AddWordsProblem.TooLong
}
