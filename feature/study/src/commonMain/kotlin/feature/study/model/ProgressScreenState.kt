package feature.study.model

import domain.word.model.ProgressEvaluation
import domain.word.model.ProgressStats

data class ProgressScreenState(
    val progressStats: ProgressStats,
    val progressEvaluation: ProgressEvaluation,
)
