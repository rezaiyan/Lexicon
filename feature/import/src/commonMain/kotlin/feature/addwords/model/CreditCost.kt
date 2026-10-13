package feature.addwords.model

import domain.credits.model.CreditAction
import domain.credits.model.CreditBalance
import domain.word.add.model.WordOrigin

/**
 * The AI credit action a source spends; null for free sources (typing, files). [guided]: the
 * request carries the learner's own instructions, which is priced separately.
 */
fun WordOrigin.creditAction(guided: Boolean = false): CreditAction? = when (this) {
    WordOrigin.Photo -> if (guided) CreditAction.PHOTO_EXTRACTION_GUIDED else CreditAction.PHOTO_EXTRACTION
    WordOrigin.AiSuggestion -> if (guided) CreditAction.AI_SUGGESTION_GUIDED else CreditAction.AI_SUGGESTION
    WordOrigin.Manual, WordOrigin.File, WordOrigin.Onboarding -> null
}

/**
 * Whether [origin] can be started. Free sources always can; with an unknown balance (not loaded,
 * offline) the server decides, so the request is let through.
 */
fun CreditBalance?.canStart(origin: WordOrigin, guided: Boolean = false): Boolean {
    val action = origin.creditAction(guided) ?: return true
    return this?.canAfford(action) ?: true
}

/**
 * What the learner pays for [origin] right now; null while unknown. A guided price the server
 * doesn't know yet (older server) falls back to the plain price, which is what it would charge.
 */
fun CreditBalance?.costFor(origin: WordOrigin, guided: Boolean): Int? {
    val balance = this ?: return null
    val plain = origin.creditAction(guided = false) ?: return 0
    val action = origin.creditAction(guided) ?: return 0
    return balance.costs[action] ?: balance.costOf(plain)
}

/** How many more credits a guided request costs than a plain one; 0 when unknown or the same. */
fun CreditBalance?.guidedSurcharge(origin: WordOrigin): Int {
    val guided = costFor(origin, guided = true) ?: return 0
    val plain = costFor(origin, guided = false) ?: return 0
    return (guided - plain).coerceAtLeast(0)
}
