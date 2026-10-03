package domain.focus

import domain.focus.model.LanguageSummary
import domain.focus.model.LearningFocus
import domain.tag.model.Tag
import domain.word.model.Word
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LearningFocusPolicyTest {

    private val now = 1_000_000_000L
    private val today = LearningFocusPolicy.epochDay(now)

    private fun word(
        id: Int,
        language: Language,
        due: Boolean = true,
        level: Int = 0,
        lastReview: Long = 0L,
        tags: List<Long> = emptyList(),
    ) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = language,
        level = level,
        lastReviewDate = lastReview,
        nextReviewDate = if (due) now - 1 else now + 1,
        tagIds = tags,
    )

    private fun tag(id: Long, name: String) =
        Tag(id = id, name = name, wordCount = 0L, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `resolve keys on targetLanguage - same native language with three learning languages is not All`() {
        // Real data shape: originalWord is in targetLanguage, translation in the shared native sourceLanguage.
        fun learned(id: Int, original: String, target: Language) = Word(
            id = id,
            originalWord = original,
            translation = "t$id",
            description = "",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = target,
            nextReviewDate = now - 1,
        )
        val words = listOf(
            learned(1, "zuverlässig", Language.GERMAN),
            learned(2, "Bonjour", Language.FRENCH),
            learned(3, "Hola", Language.SPANISH),
            learned(4, "Buenos días", Language.SPANISH),
        )

        val focus = LearningFocusPolicy.resolve(words, LearningFocus.Single(Language.GERMAN), now)

        assertEquals(LearningFocus.Single(Language.GERMAN), focus)
        assertEquals(listOf("zuverlässig"), words.filterBy(focus).map { it.originalWord })
    }

    @Test
    fun `resolve when no words returns All`() {
        assertEquals(LearningFocus.All, LearningFocusPolicy.resolve(emptyList(), null, now))
    }

    @Test
    fun `resolve when one language returns All even with stored preference`() {
        val words = listOf(word(1, Language.GERMAN))
        val result = LearningFocusPolicy.resolve(words, LearningFocus.Single(Language.GERMAN), now)
        assertEquals(LearningFocus.All, result)
    }

    @Test
    fun `resolve when stored language still present returns it`() {
        val words = listOf(word(1, Language.GERMAN, lastReview = 9), word(2, Language.SPANISH))
        val result = LearningFocusPolicy.resolve(words, LearningFocus.Single(Language.SPANISH), now)
        assertEquals(LearningFocus.Single(Language.SPANISH), result)
    }

    @Test
    fun `resolve when stored All returns All`() {
        val words = listOf(word(1, Language.GERMAN), word(2, Language.SPANISH))
        assertEquals(LearningFocus.All, LearningFocusPolicy.resolve(words, LearningFocus.All, now))
    }

    @Test
    fun `resolve when stored language missing falls back to most recently reviewed`() {
        val words = listOf(word(1, Language.GERMAN, lastReview = 5), word(2, Language.SPANISH, lastReview = 9))
        val result = LearningFocusPolicy.resolve(words, LearningFocus.Single(Language.FRENCH), now)
        assertEquals(LearningFocus.Single(Language.SPANISH), result)
    }

    @Test
    fun `resolve without preference picks most recently reviewed language`() {
        val words = listOf(word(1, Language.GERMAN, lastReview = 50), word(2, Language.SPANISH, lastReview = 9))
        assertEquals(LearningFocus.Single(Language.GERMAN), LearningFocusPolicy.resolve(words, null, now))
    }

    @Test
    fun `resolve when nothing reviewed picks language with most due words`() {
        val words = listOf(
            word(1, Language.GERMAN, due = false),
            word(2, Language.SPANISH),
            word(3, Language.SPANISH),
        )
        assertEquals(LearningFocus.Single(Language.SPANISH), LearningFocusPolicy.resolve(words, null, now))
    }

    @Test
    fun `resolve when due counts tie picks language with most words`() {
        val words = listOf(
            word(1, Language.GERMAN),
            word(2, Language.GERMAN, due = false),
            word(3, Language.SPANISH),
        )
        assertEquals(LearningFocus.Single(Language.GERMAN), LearningFocusPolicy.resolve(words, null, now))
    }

    @Test
    fun `filterBy Single keeps only that learning language`() {
        val words = listOf(word(1, Language.GERMAN), word(2, Language.SPANISH))
        assertEquals(listOf(1), words.filterBy(LearningFocus.Single(Language.GERMAN)).map { it.id })
        assertEquals(listOf(1, 2), words.filterBy(LearningFocus.All).map { it.id })
    }

    @Test
    fun `summaries put active language first then by due count`() {
        val words = listOf(
            word(1, Language.GERMAN, due = false),
            word(2, Language.SPANISH),
            word(3, Language.SPANISH),
            word(4, Language.FRENCH),
        )
        val result = LearningFocusPolicy.summaries(words, LearningFocus.Single(Language.GERMAN), now)
        assertEquals(
            listOf(
                LanguageSummary(Language.GERMAN, wordCount = 1, dueCount = 0),
                LanguageSummary(Language.SPANISH, wordCount = 2, dueCount = 2),
                LanguageSummary(Language.FRENCH, wordCount = 1, dueCount = 1),
            ),
            result,
        )
    }

    @Test
    fun `nudge returns other language with most due cards above threshold`() {
        val summaries = listOf(
            LanguageSummary(Language.GERMAN, 30, 30),
            LanguageSummary(Language.SPANISH, 20, 12),
            LanguageSummary(Language.FRENCH, 20, 15),
        )
        val result = LearningFocusPolicy.nudge(summaries, LearningFocus.Single(Language.GERMAN), null, now)
        assertEquals(Language.FRENCH, result?.language)
    }

    @Test
    fun `nudge is null below threshold`() {
        val summaries = listOf(LanguageSummary(Language.GERMAN, 5, 1), LanguageSummary(Language.SPANISH, 9, 9))
        assertNull(LearningFocusPolicy.nudge(summaries, LearningFocus.Single(Language.GERMAN), null, now))
    }

    @Test
    fun `nudge is null when dismissed today`() {
        val summaries = listOf(LanguageSummary(Language.GERMAN, 5, 1), LanguageSummary(Language.SPANISH, 20, 20))
        assertNull(LearningFocusPolicy.nudge(summaries, LearningFocus.Single(Language.GERMAN), today, now))
    }

    @Test
    fun `nudge reappears the day after dismissal`() {
        val summaries = listOf(LanguageSummary(Language.GERMAN, 5, 1), LanguageSummary(Language.SPANISH, 20, 20))
        val result = LearningFocusPolicy.nudge(summaries, LearningFocus.Single(Language.GERMAN), today - 1, now)
        assertEquals(Language.SPANISH, result?.language)
    }

    @Test
    fun `nudge is null in All focus`() {
        val summaries = listOf(LanguageSummary(Language.GERMAN, 5, 1), LanguageSummary(Language.SPANISH, 20, 20))
        assertNull(LearningFocusPolicy.nudge(summaries, LearningFocus.All, null, now))
    }

    @Test
    fun `progressStats counts levels and due words`() {
        val words = listOf(
            word(1, Language.GERMAN, level = 0),
            word(2, Language.GERMAN, level = 6, due = false),
            word(3, Language.GERMAN, level = 6),
        )
        val stats = LearningFocusPolicy.progressStats(words, now)
        assertEquals(1, stats.level0Count)
        assertEquals(2, stats.level6Count)
        assertEquals(3, stats.totalWords)
        assertEquals(2, stats.dueCards)
    }

    @Test
    fun `tagStats in Single focus drops tags without focused words`() {
        val tags = listOf(tag(1, "Food"), tag(2, "Travel"))
        val focused = listOf(word(1, Language.GERMAN, level = 2, tags = listOf(1L)))
        val stats = LearningFocusPolicy.tagStats(tags, focused, LearningFocus.Single(Language.GERMAN), now)
        assertEquals(listOf(1L), stats.tags.map { it.id })
        assertEquals(1L, stats.tags.single().wordCount)
        assertEquals(listOf(1L), stats.dueTags.map { it.id })
        assertEquals(mapOf(2 to listOf(1L)), stats.tagsByLevel.mapValues { (_, v) -> v.map { it.id } })
    }

    @Test
    fun `tagStats in All focus keeps empty tags but not in due or level maps`() {
        val tags = listOf(tag(1, "Food"), tag(2, "Travel"))
        val words = listOf(word(1, Language.GERMAN, due = false, tags = listOf(1L)))
        val stats = LearningFocusPolicy.tagStats(tags, words, LearningFocus.All, now)
        assertEquals(listOf(1L to 1L, 2L to 0L), stats.tags.map { it.id to it.wordCount })
        assertEquals(emptyList(), stats.dueTags)
        assertEquals(listOf(0), stats.tagsByLevel.keys.toList())
    }
}
