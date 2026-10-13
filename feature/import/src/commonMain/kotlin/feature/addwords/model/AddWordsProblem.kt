package feature.addwords.model

import core.error.DomainError
import core.error.DomainError.AddWords.InvalidDraft.Reason
import domain.word.add.model.WordOrigin

/** What went wrong, in user terms. The UI maps each case to a localized message. */
enum class AddWordsProblem {
    Offline,
    /** The request got no answer in time (slow network or server); not the same as being offline. */
    TimedOut,
    /** The server failed on a paid AI request; it refunds those, so no credits were used. */
    AiUnavailable,
    PremiumRequired,
    OutOfCredits,
    RateLimited,
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
    is DomainError.Network -> toProblem(origin)
    is DomainError.Commerce.PremiumRequired -> AddWordsProblem.PremiumRequired
    is DomainError.Commerce.InsufficientCredits -> AddWordsProblem.OutOfCredits
    is DomainError.AddWords.FileTooLarge -> AddWordsProblem.FileTooLarge
    is DomainError.AddWords.UnsupportedFile -> AddWordsProblem.UnsupportedFile
    is DomainError.AddWords.EmptyInput -> AddWordsProblem.EmptyFile
    is DomainError.AddWords.ImageUnreadable -> AddWordsProblem.ImageUnreadable
    is DomainError.AddWords.ImageTooLarge -> AddWordsProblem.ImageTooLarge
    is DomainError.AddWords.NothingRecognized -> origin.nothingFound()
    is DomainError.AddWords.InvalidDraft -> reason.toProblem()
    else -> AddWordsProblem.Generic
}

private fun DomainError.Network.toProblem(origin: WordOrigin) = when (this) {
    DomainError.Network.NoConnection -> AddWordsProblem.Offline
    DomainError.Network.Timeout -> AddWordsProblem.TimedOut
    DomainError.Network.RateLimited -> AddWordsProblem.RateLimited
    is DomainError.Network.ServerError ->
        if (origin.isPaidAi) AddWordsProblem.AiUnavailable else AddWordsProblem.Generic
}

/** Sources that spend credits on an AI call; the server refunds them when that call fails. */
private val WordOrigin.isPaidAi: Boolean
    get() = this == WordOrigin.Photo || this == WordOrigin.AiSuggestion

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
