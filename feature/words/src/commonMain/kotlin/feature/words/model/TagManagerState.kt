package feature.words.model

import domain.tag.model.Tag

/** Below this many tags the whole list fits on screen, so the search field would only add noise. */
private const val SEARCH_THRESHOLD = 8

data class TagManagerState(
    val tags: List<Tag> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val skipTagSelector: Boolean = false,
) {
    val showSearch: Boolean get() = tags.size >= SEARCH_THRESHOLD

    val visibleTags: List<Tag>
        get() {
            val needle = query.trim()
            return if (needle.isEmpty()) tags else tags.filter { it.name.contains(needle, ignoreCase = true) }
        }
}

sealed class TagManagerEffect {
    data class TagCreated(val tag: Tag) : TagManagerEffect()
    data class TagRenamed(val tag: Tag) : TagManagerEffect()
    data class TagDeleted(val tagId: Long) : TagManagerEffect()
    data class Error(val message: String) : TagManagerEffect()
}
