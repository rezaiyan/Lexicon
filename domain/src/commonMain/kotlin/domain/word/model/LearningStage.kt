package domain.word.model

/**
 * 7-Level Learning Stage System
 * Each stage corresponds to a level (0-6) in the spaced repetition algorithm
 */
enum class LearningStage(val level: Int) {
    LEVEL_0_FRESH(0),       // Brand new words
    LEVEL_1_LEARNING(1),    // First learning phase (10 min)
    LEVEL_2_FAMILIAR(2),    // Getting familiar (1 day)
    LEVEL_3_BUILDING(3),    // Building confidence (3 days)
    LEVEL_4_ALMOST(4),      // Almost there (7 days)
    LEVEL_5_STRONG(5),      // Strong grasp (14 days)
    LEVEL_6_MASTERED(6);    // Fully mastered (30+ days)
    
    companion object {
        fun fromLevel(level: Int): LearningStage {
            return entries.find { it.level == level } ?: LEVEL_0_FRESH
        }
    }
}

