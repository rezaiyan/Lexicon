package domain.analytics.model

data class LevelTransition(
    val fromLevel: Int,
    val toLevel: Int,
    val count: Long,
)
