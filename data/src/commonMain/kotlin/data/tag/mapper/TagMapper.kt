package data.tag.mapper

import data.tag.remote.model.RemoteTag
import domain.tag.model.Tag

fun RemoteTag.toDomain(): Tag = Tag(
    id = id,
    name = name,
    wordCount = wordCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)
