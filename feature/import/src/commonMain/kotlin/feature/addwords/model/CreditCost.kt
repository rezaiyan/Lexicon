package feature.addwords.model

import domain.credits.model.CreditAction
import domain.credits.model.CreditBalance
import domain.word.add.model.WordOrigin

/** The AI credit action a source spends; null for free sources (typing, files). */
val WordOrigin.creditAction: CreditAction?
    get() = when (this) {
        WordOrigin.Photo -> CreditAction.PHOTO_EXTRACTION
        WordOrigin.AiSuggestion -> CreditAction.AI_SUGGESTION
        WordOrigin.Manual, WordOrigin.File, WordOrigin.Onboarding -> null
    }

/**
 * Whether [origin] can be started. Free sources always can; with an unknown balance (not loaded,
 * offline) the server decides, so the request is let through.
 */
fun CreditBalance?.canStart(origin: WordOrigin): Boolean {
    val action = origin.creditAction ?: return true
    return this?.canAfford(action) ?: true
}
