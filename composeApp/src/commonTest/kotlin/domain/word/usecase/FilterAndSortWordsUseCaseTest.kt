package domain.word.usecase

import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.model.WordSortOption
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterAndSortWordsUseCaseTest {

    private val useCase = FilterAndSortWordsUseCase()

    private fun word(
        id: Int,
        original: String = "word$id",
        translation: String = "trans$id",
        description: String = "desc$id",
        sourceLanguage: Language = Language.ENGLISH,
        targetLanguage: Language = Language.GERMAN,
        level: Int = 0,
        dateAdded: Long = 0L,
        tagIds: List<Long> = emptyList(),
    ) = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = description,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        level = level,
        dateAdded = dateAdded,
        nextReviewDate = 0L,
        tagIds = tagIds,
    )

    // -----------------------------------------------------------------------
    // Empty input
    // -----------------------------------------------------------------------

    @Test
    fun `empty word list returns empty`() {
        val result = useCase(FilterAndSortWordsUseCase.Params(words = emptyList()))
        assertTrue(result.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Query filter
    // -----------------------------------------------------------------------

    @Test
    fun `query matches originalWord case insensitively`() {
        val words = listOf(
            word(1, original = "Apple"),
            word(2, original = "banana"),
            word(3, original = "Cherry"),
        )
        val result = useCase(FilterAndSortWordsUseCase.Params(words = words, query = "app"))
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun `query matches translation case insensitively`() {
        val words = listOf(
            word(1, translation = "Apfel"),
            word(2, translation = "Banane"),
        )
        val result = useCase(FilterAndSortWordsUseCase.Params(words = words, query = "APFEL"))
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun `query matches description case insensitively`() {
        val words = listOf(
            word(1, description = "A common fruit"),
            word(2, description = "Another common vegetable"),
        )
        val result = useCase(FilterAndSortWordsUseCase.Params(words = words, query = "FRUIT"))
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun `blank query does not filter any words`() {
        val words = listOf(word(1), word(2), word(3))
        val result = useCase(FilterAndSortWordsUseCase.Params(words = words, query = "   "))
        assertEquals(3, result.size)
    }

    // -----------------------------------------------------------------------
    // Language filter
    // -----------------------------------------------------------------------

    @Test
    fun `language filter keeps words with matching source language`() {
        val words = listOf(
            word(1, sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN),
            word(2, sourceLanguage = Language.FRENCH, targetLanguage = Language.GERMAN),
            word(3, sourceLanguage = Language.SPANISH, targetLanguage = Language.ENGLISH),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, filterLanguage = Language.ENGLISH)
        )
        // id=1 has source=ENGLISH, id=3 has target=ENGLISH
        val ids = result.map { it.id }.toSet()
        assertEquals(setOf(1, 3), ids)
    }

    @Test
    fun `language filter keeps words with matching target language`() {
        val words = listOf(
            word(1, sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN),
            word(2, sourceLanguage = Language.ENGLISH, targetLanguage = Language.FRENCH),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, filterLanguage = Language.GERMAN)
        )
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun `null language filter returns all words`() {
        val words = listOf(word(1), word(2), word(3))
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, filterLanguage = null)
        )
        assertEquals(3, result.size)
    }

    // -----------------------------------------------------------------------
    // Stage filter
    // -----------------------------------------------------------------------

    @Test
    fun `stage filter uses LearningStage fromLevel`() {
        val words = listOf(
            word(1, level = 0),  // LEVEL_0_FRESH
            word(2, level = 2),  // LEVEL_2_FAMILIAR
            word(3, level = 5),  // LEVEL_5_STRONG
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, filterStage = LearningStage.LEVEL_2_FAMILIAR)
        )
        assertEquals(listOf(2), result.map { it.id })
    }

    @Test
    fun `null stage filter returns all words`() {
        val words = listOf(word(1, level = 0), word(2, level = 3), word(3, level = 6))
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, filterStage = null)
        )
        assertEquals(3, result.size)
    }

    // -----------------------------------------------------------------------
    // Tag filter
    // -----------------------------------------------------------------------

    @Test
    fun `tag filter keeps only words containing the tagId`() {
        val words = listOf(
            word(1, tagIds = listOf(10L, 20L)),
            word(2, tagIds = listOf(20L, 30L)),
            word(3, tagIds = listOf(30L)),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, filterTagId = 20L)
        )
        val ids = result.map { it.id }.toSet()
        assertEquals(setOf(1, 2), ids)
    }

    @Test
    fun `null tag filter returns all words`() {
        val words = listOf(
            word(1, tagIds = listOf(1L)),
            word(2, tagIds = emptyList()),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, filterTagId = null)
        )
        assertEquals(2, result.size)
    }

    // -----------------------------------------------------------------------
    // Sort options
    // -----------------------------------------------------------------------

    @Test
    fun `DATE_ADDED_DESC sorts newest first with id tiebreaker`() {
        val words = listOf(
            word(1, dateAdded = 1000L),
            word(2, dateAdded = 3000L),
            word(3, dateAdded = 2000L),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, sortOption = WordSortOption.DATE_ADDED_DESC)
        )
        assertEquals(listOf(2, 3, 1), result.map { it.id })
    }

    @Test
    fun `ALPHABETICAL_AZ sorts case insensitively with id tiebreaker`() {
        val words = listOf(
            word(1, original = "Cherry"),
            word(2, original = "apple"),
            word(3, original = "Banana"),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, sortOption = WordSortOption.ALPHABETICAL_AZ)
        )
        assertEquals(listOf(2, 3, 1), result.map { it.id })
    }

    @Test
    fun `ALPHABETICAL_ZA sorts reverse alphabetically`() {
        val words = listOf(
            word(1, original = "apple"),
            word(2, original = "cherry"),
            word(3, original = "banana"),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, sortOption = WordSortOption.ALPHABETICAL_ZA)
        )
        assertEquals(listOf(2, 3, 1), result.map { it.id })
    }

    @Test
    fun `LEVEL_ASC sorts lowest level first`() {
        val words = listOf(
            word(1, level = 3),
            word(2, level = 1),
            word(3, level = 5),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, sortOption = WordSortOption.LEVEL_ASC)
        )
        assertEquals(listOf(2, 1, 3), result.map { it.id })
    }

    @Test
    fun `LEVEL_DESC sorts highest level first`() {
        val words = listOf(
            word(1, level = 3),
            word(2, level = 5),
            word(3, level = 1),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(words = words, sortOption = WordSortOption.LEVEL_DESC)
        )
        assertEquals(listOf(2, 1, 3), result.map { it.id })
    }

    // -----------------------------------------------------------------------
    // Multiple filters combined
    // -----------------------------------------------------------------------

    @Test
    fun `multiple filters applied together - query and language`() {
        val words = listOf(
            word(1, original = "apple", sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN),
            word(2, original = "apricot", sourceLanguage = Language.FRENCH, targetLanguage = Language.GERMAN),
            word(3, original = "banana", sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN),
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(
                words = words,
                query = "ap",
                filterLanguage = Language.ENGLISH,
            )
        )
        // "ap" matches word(1) and word(2); language=ENGLISH further filters to word(1)
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun `multiple filters applied together - stage and tag`() {
        val words = listOf(
            word(1, level = 2, tagIds = listOf(10L)),  // LEVEL_2_FAMILIAR, tagged
            word(2, level = 2, tagIds = listOf(20L)),  // LEVEL_2_FAMILIAR, different tag
            word(3, level = 3, tagIds = listOf(10L)),  // LEVEL_3_BUILDING, tagged
        )
        val result = useCase(
            FilterAndSortWordsUseCase.Params(
                words = words,
                filterStage = LearningStage.LEVEL_2_FAMILIAR,
                filterTagId = 10L,
            )
        )
        assertEquals(listOf(1), result.map { it.id })
    }
}
