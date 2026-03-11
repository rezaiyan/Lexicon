package presentation.feature.words

import domain.word.model.Word
import feature.words.model.WordManagerScreenState
import feature.words.model.WordSortOption
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class WordManagerSortingTest {

    private fun word(
        id: Int,
        original: String = "word$id",
        dateAdded: Long = 0L,
        level: Int = 0
    ) = Word(
        id = id,
        originalWord = original,
        translation = "trans$id",
        description = "desc$id",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L,
        dateAdded = dateAdded,
        level = level
    )

    // --- DATE_ADDED_DESC (Newest first) ---

    @Test
    fun `DATE_ADDED_DESC sorts newest first`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, dateAdded = 1000L),
                word(2, dateAdded = 3000L),
                word(3, dateAdded = 2000L)
            ),
            sortOption = WordSortOption.DATE_ADDED_DESC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(2, 3, 1), ids, "Should sort by dateAdded descending")
    }

    @Test
    fun `DATE_ADDED_DESC with same dateAdded uses id descending as tiebreaker`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, dateAdded = 1000L),
                word(3, dateAdded = 1000L),
                word(2, dateAdded = 1000L)
            ),
            sortOption = WordSortOption.DATE_ADDED_DESC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(3, 2, 1), ids, "Same dateAdded should use id descending as tiebreaker")
    }

    @Test
    fun `DATE_ADDED_DESC with zero dateAdded uses id descending as tiebreaker`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(5, original = "apple", dateAdded = 5000L),
                word(1, original = "banana", dateAdded = 0L),
                word(4, original = "cherry", dateAdded = 0L),
                word(2, original = "date", dateAdded = 0L),
                word(3, original = "elderberry", dateAdded = 0L)
            ),
            sortOption = WordSortOption.DATE_ADDED_DESC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(
            listOf(5, 4, 3, 2, 1), ids,
            "Newest first, then words with dateAdded=0 by id descending"
        )
    }

    // --- DATE_ADDED_ASC (Oldest first) ---

    @Test
    fun `DATE_ADDED_ASC sorts oldest first`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, dateAdded = 3000L),
                word(2, dateAdded = 1000L),
                word(3, dateAdded = 2000L)
            ),
            sortOption = WordSortOption.DATE_ADDED_ASC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(2, 3, 1), ids, "Should sort by dateAdded ascending")
    }

    @Test
    fun `DATE_ADDED_ASC with same dateAdded uses id ascending as tiebreaker`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(3, dateAdded = 1000L),
                word(1, dateAdded = 1000L),
                word(2, dateAdded = 1000L)
            ),
            sortOption = WordSortOption.DATE_ADDED_ASC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(1, 2, 3), ids, "Same dateAdded should use id ascending as tiebreaker")
    }

    // --- ALPHABETICAL_AZ ---

    @Test
    fun `ALPHABETICAL_AZ sorts A to Z`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, original = "cherry"),
                word(2, original = "apple"),
                word(3, original = "banana")
            ),
            sortOption = WordSortOption.ALPHABETICAL_AZ
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(2, 3, 1), ids, "Should sort alphabetically A-Z")
    }

    @Test
    fun `ALPHABETICAL_AZ is case insensitive`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, original = "Cherry"),
                word(2, original = "apple"),
                word(3, original = "Banana")
            ),
            sortOption = WordSortOption.ALPHABETICAL_AZ
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(2, 3, 1), ids, "Should sort case-insensitively")
    }

    @Test
    fun `ALPHABETICAL_AZ with same name uses id ascending as tiebreaker`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(3, original = "apple"),
                word(1, original = "apple"),
                word(2, original = "apple")
            ),
            sortOption = WordSortOption.ALPHABETICAL_AZ
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(1, 2, 3), ids, "Same name should use id ascending as tiebreaker")
    }

    // --- ALPHABETICAL_ZA ---

    @Test
    fun `ALPHABETICAL_ZA sorts Z to A`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, original = "apple"),
                word(2, original = "cherry"),
                word(3, original = "banana")
            ),
            sortOption = WordSortOption.ALPHABETICAL_ZA
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(2, 3, 1), ids, "Should sort alphabetically Z-A")
    }

    // --- LEVEL_ASC ---

    @Test
    fun `LEVEL_ASC sorts lowest level first`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, level = 3),
                word(2, level = 1),
                word(3, level = 5)
            ),
            sortOption = WordSortOption.LEVEL_ASC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(2, 1, 3), ids, "Should sort by level ascending")
    }

    @Test
    fun `LEVEL_ASC with same level uses dateAdded descending as tiebreaker`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, level = 0, dateAdded = 1000L),
                word(2, level = 0, dateAdded = 3000L),
                word(3, level = 0, dateAdded = 2000L)
            ),
            sortOption = WordSortOption.LEVEL_ASC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(
            listOf(2, 3, 1), ids,
            "Same level should show newest first as tiebreaker"
        )
    }

    // --- LEVEL_DESC ---

    @Test
    fun `LEVEL_DESC sorts highest level first`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, level = 3),
                word(2, level = 5),
                word(3, level = 1)
            ),
            sortOption = WordSortOption.LEVEL_DESC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(2, 1, 3), ids, "Should sort by level descending")
    }

    @Test
    fun `LEVEL_DESC with same level uses dateAdded descending as tiebreaker`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, level = 6, dateAdded = 1000L),
                word(2, level = 6, dateAdded = 3000L),
                word(3, level = 6, dateAdded = 2000L)
            ),
            sortOption = WordSortOption.LEVEL_DESC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(
            listOf(2, 3, 1), ids,
            "Same level should show newest first as tiebreaker"
        )
    }

    // --- Sorting with filters ---

    @Test
    fun `sorting applies after search filter`() {
        val state = WordManagerScreenState(
            words = listOf(
                word(1, original = "cherry", dateAdded = 3000L),
                word(2, original = "chocolate", dateAdded = 1000L),
                word(3, original = "banana", dateAdded = 2000L),
                word(4, original = "chip", dateAdded = 4000L)
            ),
            searchQuery = "ch",
            sortOption = WordSortOption.DATE_ADDED_DESC
        )

        val ids = state.filteredWords.map { it.id }
        assertEquals(listOf(4, 1, 2), ids, "Should filter by 'ch' then sort newest first")
    }
}
