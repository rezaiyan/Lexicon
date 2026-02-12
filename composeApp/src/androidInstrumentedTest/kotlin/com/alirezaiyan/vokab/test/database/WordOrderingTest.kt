package com.alirezaiyan.vokab.test.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import data.core.database.AppDatabase
import data.core.database.LexiconDao
import data.core.database.WordEntity
import com.alirezaiyan.vokab.test.utils.TestConstants
import com.alirezaiyan.vokab.test.utils.TestUtils
import com.alirezaiyan.vokab.test.database.WordOrderingTestHelpers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for word ordering/sorting functionality
 * 
 * Tests FILO (First In Last Out) behavior:
 * - Due words appear first, ordered by nextReviewDate ASC (earliest due first)
 * - Non-due words appear after, ordered by lastReviewDate DESC (most recently reviewed first = FILO)
 * 
 * Covers:
 * - getWordsByLevel with FILO sorting
 * - getWordsByLevelRange with FILO sorting
 * - getDueCards ordering
 * - Both authenticated word entities across levels
 */
@RunWith(AndroidJUnit4::class)
class WordOrderingTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: LexiconDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        dao = database.getDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    // ========== getWordsByLevel FILO Tests ==========
    
    @Test
    fun getWordsByLevel_dueWordsComeFirst_sortedByNextReviewDateAsc() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = listOf(
            WordOrderingTestHelpers.createDueWord(id = 1, level = 2, hoursAgo = 1, now),
            WordOrderingTestHelpers.createDueWord(id = 2, level = 2, hoursAgo = 3, now),
            WordOrderingTestHelpers.createDueWord(id = 3, level = 2, hoursAgo = 2, now)
        )
        dao.insert(words)
        
        val result = dao.getWordsByLevel(2, now).first()
        
        assertEquals(3, result.size)
        assertEquals(2, result[0].id)
        assertEquals(3, result[1].id)
        assertEquals(1, result[2].id)
        
        assertTrue(result[0].nextReviewDate <= result[1].nextReviewDate)
        assertTrue(result[1].nextReviewDate <= result[2].nextReviewDate)
    }
    
    @Test
    fun getWordsByLevel_nonDueWordsSortedByFILO_mostRecentlyReviewedFirst() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = listOf(
            WordOrderingTestHelpers.createNonDueWord(id = 1, level = 2, hoursInFuture = 1, hoursAgoReviewed = 3, now),
            WordOrderingTestHelpers.createNonDueWord(id = 2, level = 2, hoursInFuture = 2, hoursAgoReviewed = 2, now),
            WordOrderingTestHelpers.createNonDueWord(id = 3, level = 2, hoursInFuture = 3, hoursAgoReviewed = 1, now)
        )
        dao.insert(words)
        
        val result = dao.getWordsByLevel(2, now).first()
        
        assertEquals(3, result.size)
        assertEquals(3, result[0].id)
        assertEquals(2, result[1].id)
        assertEquals(1, result[2].id)
        
        assertTrue(result[0].lastReviewDate >= result[1].lastReviewDate)
        assertTrue(result[1].lastReviewDate >= result[2].lastReviewDate)
    }
    
    @Test
    fun getWordsByLevel_mixedDueAndNonDue_dueWordsFirstThenFILO() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = listOf(
            WordOrderingTestHelpers.createNonDueWord(id = 1, level = 2, hoursInFuture = 1, hoursAgoReviewed = 3, now),
            WordOrderingTestHelpers.createNonDueWord(id = 2, level = 2, hoursInFuture = 2, hoursAgoReviewed = 1, now),
            WordOrderingTestHelpers.createDueWord(id = 3, level = 2, hoursAgo = 2, now),
            WordOrderingTestHelpers.createDueWord(id = 4, level = 2, hoursAgo = 1, now)
        )
        dao.insert(words)
        
        val result = dao.getWordsByLevel(2, now).first()
        
        assertEquals(4, result.size)
        assertEquals(3, result[0].id)
        assertEquals(4, result[1].id)
        assertEquals(2, result[2].id)
        assertEquals(1, result[3].id)
        
        assertTrue(result[0].nextReviewDate <= now)
        assertTrue(result[1].nextReviewDate <= now)
        assertTrue(result[2].nextReviewDate > now)
        assertTrue(result[3].nextReviewDate > now)
    }
    
    @Test
    fun getWordsByLevel_multipleDueWords_sameNextReviewDate_stableOrder() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val sameDueDate = now - TestConstants.MILLIS_PER_HOUR
        val words = listOf(
            WordOrderingTestHelpers.createDueWord(id = 1, level = 2, hoursAgo = 1, now),
            WordOrderingTestHelpers.createDueWord(id = 2, level = 2, hoursAgo = 1, now),
            WordOrderingTestHelpers.createDueWord(id = 3, level = 2, hoursAgo = 1, now)
        )
        dao.insert(words)
        
        val result = dao.getWordsByLevel(2, now).first()
        
        assertEquals(3, result.size)
        result.forEach { word ->
            assertTrue(word.nextReviewDate <= now)
        }
    }
    
    @Test
    fun getWordsByLevel_multipleNonDueWords_sameLastReviewDate_stableOrder() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = listOf(
            WordOrderingTestHelpers.createNonDueWord(id = 1, level = 2, hoursInFuture = 1, hoursAgoReviewed = 2, now),
            WordOrderingTestHelpers.createNonDueWord(id = 2, level = 2, hoursInFuture = 1, hoursAgoReviewed = 2, now),
            WordOrderingTestHelpers.createNonDueWord(id = 3, level = 2, hoursInFuture = 1, hoursAgoReviewed = 2, now)
        )
        dao.insert(words)
        
        val result = dao.getWordsByLevel(2, now).first()
        
        assertEquals(3, result.size)
        result.forEach { word ->
            assertTrue(word.nextReviewDate > now)
        }
    }
    
    // ========== getWordsByLevelRange FILO Tests ==========
    
    @Test
    fun getWordsByLevelRange_mixedLevels_dueWordsFirstThenFILO() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = listOf(
            WordOrderingTestHelpers.createDueWord(id = 1, level = 2, hoursAgo = 1, now),
            WordOrderingTestHelpers.createNonDueWord(id = 2, level = 3, hoursInFuture = 1, hoursAgoReviewed = 3, now),
            WordOrderingTestHelpers.createNonDueWord(id = 3, level = 3, hoursInFuture = 1, hoursAgoReviewed = 1, now),
            WordOrderingTestHelpers.createDueWord(id = 4, level = 4, hoursAgo = 1, now)
        )
        dao.insert(words)
        
        val result = dao.getWordsByLevelRange(2, 4, now).first()
        
        assertEquals(4, result.size)
        assertTrue(result[0].nextReviewDate <= now)
        assertTrue(result[1].nextReviewDate <= now)
        assertTrue(result[2].nextReviewDate > now)
        assertTrue(result[3].nextReviewDate > now)
        assertTrue(result[2].lastReviewDate >= result[3].lastReviewDate)
    }
    
    // ========== getDueCards Ordering Tests ==========
    
    @Test
    fun getDueCards_ordersByNextReviewDateAsc_earliestDueFirst() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = listOf(
            WordOrderingTestHelpers.createDueWord(id = 1, level = 2, hoursAgo = 1, now),
            WordOrderingTestHelpers.createDueWord(id = 2, level = 2, hoursAgo = 3, now),
            WordOrderingTestHelpers.createNonDueWord(id = 3, level = 2, hoursInFuture = 1, hoursAgoReviewed = 1, now),
            WordOrderingTestHelpers.createDueWord(id = 4, level = 2, hoursAgo = 2, now),
            WordOrderingTestHelpers.createNeverReviewedDueWord(id = 5, level = 0, hoursAgo = 1, daysAgoAdded = 2, now),
            WordOrderingTestHelpers.createNeverReviewedDueWord(id = 6, level = 0, hoursAgo = 1, daysAgoAdded = 1, now)
        )
        dao.insert(words)
        
        val result = dao.getDueCards(now).first()
        
        assertEquals(5, result.size)
        result.forEach { word ->
            assertTrue(word.nextReviewDate <= now)
        }
        
        val neverReviewedWords = result.filter { it.repetitions == 0 && it.lastReviewDate == 0L }
        val reviewedWords = result.filter { !(it.repetitions == 0 && it.lastReviewDate == 0L) }
        
        assertEquals(2, neverReviewedWords.size, "Should have 2 never-reviewed words")
        assertEquals(3, reviewedWords.size, "Should have 3 reviewed words")
        
        val neverReviewedStartIndex = result.indexOfFirst { it.repetitions == 0 && it.lastReviewDate == 0L }
        val reviewedStartIndex = result.indexOfFirst { !(it.repetitions == 0 && it.lastReviewDate == 0L) }
        
        assertTrue(neverReviewedStartIndex < reviewedStartIndex, "Never-reviewed words should come before reviewed words")
        
        assertTrue(result[neverReviewedStartIndex].dateAdded >= result[neverReviewedStartIndex + 1].dateAdded,
            "Never-reviewed words should be ordered by dateAdded DESC (newest first)")
        assertEquals(6, result[neverReviewedStartIndex].id, "Newest never-reviewed word (id=6) should come first")
        assertEquals(5, result[neverReviewedStartIndex + 1].id, "Older never-reviewed word (id=5) should come second")
        
        assertTrue(result[reviewedStartIndex].nextReviewDate <= result[reviewedStartIndex + 1].nextReviewDate,
            "Reviewed words should be ordered by nextReviewDate ASC")
        assertTrue(result[reviewedStartIndex + 1].nextReviewDate <= result[reviewedStartIndex + 2].nextReviewDate,
            "Reviewed words should be ordered by nextReviewDate ASC")
        assertEquals(2, result[reviewedStartIndex].id, "Most overdue reviewed word (id=2) should come first")
        assertEquals(4, result[reviewedStartIndex + 1].id, "Second most overdue reviewed word (id=4) should come second")
        assertEquals(1, result[reviewedStartIndex + 2].id, "Least overdue reviewed word (id=1) should come last")
    }
    
    @Test
    fun getDueCards_excludesNonDueWords() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = listOf(
            WordOrderingTestHelpers.createDueWord(id = 1, level = 2, hoursAgo = 1, now),
            WordOrderingTestHelpers.createNonDueWord(id = 2, level = 2, hoursInFuture = 1, hoursAgoReviewed = 1, now),
            WordOrderingTestHelpers.createDueWord(id = 3, level = 2, hoursAgo = 1, now),
            WordOrderingTestHelpers.createNonDueWord(id = 4, level = 2, hoursInFuture = 1, hoursAgoReviewed = 1, now)
        )
        dao.insert(words)
        
        val result = dao.getDueCards(now).first()
        
        assertEquals(2, result.size)
        result.forEach { word ->
            assertTrue(word.nextReviewDate <= now)
            assertTrue(word.id in listOf(1, 3))
        }
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun getWordsByLevel_emptyResult_whenNoWordsAtLevel() = runTest {
        val words = listOf(
            TestUtils.createWordEntity(id = 1, level = 0),
            TestUtils.createWordEntity(id = 2, level = 1),
            TestUtils.createWordEntity(id = 3, level = 3)
        )
        dao.insert(words)
        
        val result = dao.getWordsByLevel(2, WordOrderingTestHelpers.getCurrentTime()).first()
        
        assertEquals(0, result.size)
    }
    
    @Test
    fun getWordsByLevel_exactDueBoundary_includesWordsAtCurrentTime() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = listOf(
            TestUtils.createWordEntity(id = 1, level = 2, nextReviewDate = now),
            TestUtils.createWordEntity(id = 2, level = 2, nextReviewDate = now - 1),
            TestUtils.createWordEntity(id = 3, level = 2, nextReviewDate = now + 1)
        )
        dao.insert(words)
        
        val result = dao.getWordsByLevel(2, now).first()
        
        assertEquals(3, result.size)
        
        val dueWords = result.filter { it.nextReviewDate <= now }
        val nonDueWords = result.filter { it.nextReviewDate > now }
        
        assertEquals(2, dueWords.size)
        assertEquals(1, nonDueWords.size)
        
        assertTrue(dueWords[0].nextReviewDate <= dueWords[1].nextReviewDate)
    }
    
    @Test
    fun getWordsByLevel_largeDataSet_maintainsCorrectOrder() = runTest {
        val now = WordOrderingTestHelpers.getCurrentTime()
        val words = mutableListOf<WordEntity>()
        
        (1..10).forEach { i ->
            words.add(WordOrderingTestHelpers.createDueWord(
                id = i,
                level = 2,
                hoursAgo = i,
                now = now
            ))
        }
        
        (11..20).forEach { i ->
            words.add(WordOrderingTestHelpers.createNonDueWord(
                id = i,
                level = 2,
                hoursInFuture = i - 10,
                hoursAgoReviewed = 20 - i,
                now = now
            ))
        }
        
        dao.insert(words)
        
        val result = dao.getWordsByLevel(2, now).first()
        
        assertEquals(20, result.size)
        
        (0..9).forEach { i ->
            assertTrue(result[i].nextReviewDate <= now)
            if (i > 0) {
                assertTrue(result[i - 1].nextReviewDate <= result[i].nextReviewDate)
            }
        }
        
        (10..19).forEach { i ->
            assertTrue(result[i].nextReviewDate > now)
            if (i > 10) {
                assertTrue(result[i - 1].lastReviewDate >= result[i].lastReviewDate)
            }
        }
    }
}

