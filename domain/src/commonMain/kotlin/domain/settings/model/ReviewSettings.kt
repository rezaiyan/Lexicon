package domain.settings.model

/**
 * Review/Learning settings configuration
 * 
 * These settings control the spaced repetition algorithm behavior:
 * - successesToAdvance: How many consecutive successes needed to advance to next level
 * - forgotPenalty: How many levels to drop when user forgets a word
 */
data class ReviewSettings(
    val successesToAdvance: Int = 1, // Range: 1-3
    val forgotPenalty: Int = 2        // Range: 1-3
) {
    init {
        require(successesToAdvance in 1..3) { "Successes to advance must be between 1 and 3" }
        require(forgotPenalty in 1..3) { "Forgot penalty must be between 1 and 3" }
    }
    
    companion object {
        /**
         * Easy mode: Advance quickly, small penalty
         */
        val EASY = ReviewSettings(successesToAdvance = 1, forgotPenalty = 1)
        
        /**
         * Balanced mode: Default settings
         */
        val BALANCED = ReviewSettings(successesToAdvance = 1, forgotPenalty = 2)
        
        /**
         * Rigorous mode: Require more proof, bigger penalty
         */
        val RIGOROUS = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        
        /**
         * Expert mode: Maximum difficulty
         */
        val EXPERT = ReviewSettings(successesToAdvance = 3, forgotPenalty = 3)
    }
}

