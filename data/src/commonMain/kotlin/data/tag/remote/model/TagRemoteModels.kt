package data.tag.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoteTag(
    val id: Long,
    val name: String,
    val wordCount: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class CreateTagPayload(val name: String)

@Serializable
data class RenameTagPayload(val name: String)

@Serializable
data class UpdateWordTagsPayload(val tagIds: List<Long>)
