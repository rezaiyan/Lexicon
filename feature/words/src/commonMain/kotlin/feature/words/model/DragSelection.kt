package feature.words.model

/**
 * One hold-and-drag selection pass over the word list, Photos style: everything between
 * [anchorIndex] and the finger is in the range, so moving back toward the anchor shrinks it
 * and a fast swipe never skips rows. A drag that starts on a selected word deselects instead.
 */
data class DragSelection(
    val anchorIndex: Int,
    val baseSelection: Set<Int>,
    val selecting: Boolean,
) {
    /** Selection to show while the finger is over [index] of [orderedIds]. */
    fun selectionAt(orderedIds: List<Int>, index: Int): Set<Int> {
        if (orderedIds.isEmpty()) return baseSelection
        val from = minOf(anchorIndex, index).coerceIn(0, orderedIds.lastIndex)
        val to = maxOf(anchorIndex, index).coerceIn(0, orderedIds.lastIndex)
        val range = orderedIds.subList(from, to + 1)
        return if (selecting) baseSelection + range else baseSelection - range.toSet()
    }
}
