package domain.word.model

/**
 * Progress statistics showing word counts for each of the 7 levels
 */
data class ProgressStats(
    val level0Count: Int = 0,  // Fresh
    val level1Count: Int = 0,  // Learning
    val level2Count: Int = 0,  // Familiar
    val level3Count: Int = 0,  // Building
    val level4Count: Int = 0,  // Almost there
    val level5Count: Int = 0,  // Strong
    val level6Count: Int = 0,  // Mastered
    val totalWords: Int = 0,
    val dueCards: Int = 0
) {
    val learningWords: Int get() = level1Count + level2Count
    val matureWords: Int get() = level5Count + level6Count

    fun countFor(stage: LearningStage): Int = when (stage) {
        LearningStage.LEVEL_0_FRESH -> level0Count
        LearningStage.LEVEL_1_LEARNING -> level1Count
        LearningStage.LEVEL_2_FAMILIAR -> level2Count
        LearningStage.LEVEL_3_BUILDING -> level3Count
        LearningStage.LEVEL_4_ALMOST -> level4Count
        LearningStage.LEVEL_5_STRONG -> level5Count
        LearningStage.LEVEL_6_MASTERED -> level6Count
    }
}

