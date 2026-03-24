package domain.tag.model

data class Tag(
    val id: Long,
    val name: String,
    val wordCount: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long,
)
