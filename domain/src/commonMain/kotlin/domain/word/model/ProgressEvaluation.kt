package domain.word.model

/**
 * Fine-grained progress tier for motivational messaging.
 * Tiers are ordered from lowest to highest progress.
 */
enum class ProgressTier {
    /** No words added yet. */
    EMPTY,
    /** 1–9% — just getting started. */
    GETTING_STARTED,
    /** 10–24% — building a foundation. */
    BUILDING,
    /** 25–49% — making real progress. */
    PROGRESSING,
    /** 50–74% — past the halfway mark. */
    HALFWAY,
    /** 75–89% — strong knowledge base. */
    STRONG,
    /** 90–99% — nearly mastered everything. */
    ALMOST_MASTER,
    /** 100% — full mastery achieved. */
    MASTERED
}

/**
 * Evaluated progress result derived from [ProgressStats].
 *
 * @param progressFraction Weighted mastery score in 0.0–1.0 range.
 * @param progressPercent Integer percentage (0–100).
 * @param tier Fine-grained motivational tier.
 */
data class ProgressEvaluation(
    val progressFraction: Float,
    val progressPercent: Int,
    val tier: ProgressTier
)
