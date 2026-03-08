package com.alirezaiyan.vokab.test.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import data.core.database.AppDatabase
import data.core.database.LexiconDao
import com.alirezaiyan.vokab.test.utils.TestUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock

/**
 * Database tests for LexiconDao
 * Tests all database operations including CRUD and query functions
 */
@RunWith(AndroidJUnit4::class)
class LexiconDaoTest {

    
    private lateinit var database: AppDatabase
    private lateinit var dao: LexiconDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Create an in-memory database for testing
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
    
    @Test
    fun insertWord_andRetrieveById() = runTest {
        // Given: A test word
        val word = TestUtils.createWordEntity(id = 1, originalWord = "hello", translation = "hola")
        
        // When: Insert the word
        dao.insert(word)
        
        // Then: Should be able to retrieve it
        val retrieved = dao.getWordById(1)
        assertNotNull(retrieved)
        assertEquals("hello", retrieved.originalWord)
        assertEquals("hola", retrieved.translation)
    }
    
    @Test
    fun insertMultipleWords_andRetrieveAll() = runTest {
        // Given: Multiple test words
        val words = listOf(
            TestUtils.createWordEntity(id = 1, originalWord = "hello"),
            TestUtils.createWordEntity(id = 2, originalWord = "goodbye"),
            TestUtils.createWordEntity(id = 3, originalWord = "thank you")
        )
        
        // When: Insert all words
        dao.insert(words)
        
        // Then: Should retrieve all
        val allWords = dao.getAll().first()
        assertEquals(3, allWords.size)
    }
    
    @Test
    fun updateWordContent_shouldModifyWord() = runTest {
        // Given: An inserted word
        val word = TestUtils.createWordEntity(id = 1, originalWord = "old", translation = "viejo")
        dao.insert(word)
        
        // When: Update the word content
        dao.updateWordContent(1, "new", "nuevo", "updated description")
        
        // Then: Should reflect changes
        val updated = dao.getWordById(1)
        assertNotNull(updated)
        assertEquals("new", updated.originalWord)
        assertEquals("nuevo", updated.translation)
        assertEquals("updated description", updated.description)
    }
    
    @Test
    fun deleteWord_shouldRemoveFromDatabase() = runTest {
        // Given: An inserted word
        val word = TestUtils.createWordEntity(id = 1)
        dao.insert(word)
        
        // When: Delete the word
        dao.deleteWord(1)
        
        // Then: Should not be retrievable
        val deleted = dao.getWordById(1)
        assertNull(deleted)
    }
    
    @Test
    fun countWords_shouldReturnCorrectCount() = runTest {
        // Given: Multiple words
        val words = listOf(
            TestUtils.createWordEntity(id = 0, level = 0),
            TestUtils.createWordEntity(id = 0, level = 1),
            TestUtils.createWordEntity(id = 0, level = 2),
            TestUtils.createWordEntity(id = 0, level = 3),
            TestUtils.createWordEntity(id = 0, level = 4),
            TestUtils.createWordEntity(id = 0, level = 5),
            TestUtils.createWordEntity(id = 0, level = 6)
        )
        dao.insert(words)
        
        // When: Count total words
        val count = dao.count()
        
        // Then: Should match inserted count
        assertEquals(7, count)
    }
    
    @Test
    fun getWordsByLevel_shouldFilterCorrectly() = runTest {
        // Given: Words at different levels
        val words = listOf(
            TestUtils.createWordEntity(id = 1, level = 0),
            TestUtils.createWordEntity(id = 2, level = 0),
            TestUtils.createWordEntity(id = 3, level = 2),
            TestUtils.createWordEntity(id = 4, level = 5)
        )
        dao.insert(words)
        
        // When: Query words at level 0
        val level0Words = dao.getWordsByLevel(0).first()
        
        // Then: Should get only level 0 words
        assertEquals(2, level0Words.size)
        level0Words.forEach { word ->
            assertEquals(0, word.level)
        }
    }
    
    @Test
    fun getWordsByLevelRange_shouldFilterCorrectly() = runTest {
        // Given: Words at different levels
        val words = listOf(
            TestUtils.createWordEntity(id = 0, level = 0),
            TestUtils.createWordEntity(id = 0, level = 1),
            TestUtils.createWordEntity(id = 0, level = 2),
            TestUtils.createWordEntity(id = 0, level = 3),
            TestUtils.createWordEntity(id = 0, level = 4),
            TestUtils.createWordEntity(id = 0, level = 5),
            TestUtils.createWordEntity(id = 0, level = 6)
        )
        dao.insert(words)
        
        // When: Query words in range 2-4
        val midLevelWords = dao.getWordsByLevelRange(2, 4).first()
        
        // Then: Should get only words in that range
        assertEquals(3, midLevelWords.size)
        midLevelWords.forEach { word ->
            assert(word.level in 2..4)
        }
    }
    
    @Test
    fun getDueCards_shouldReturnOnlyDueWords() = runTest {
        // Given: Words with different review dates
        val now = Clock.System.now().toEpochMilliseconds()
        val past = now - 1000 * 60 * 60 // 1 hour ago
        val future = now + 1000 * 60 * 60 // 1 hour from now
        
        val words = listOf(
            TestUtils.createWordEntity(id = 1, nextReviewDate = past),    // Due
            TestUtils.createWordEntity(id = 2, nextReviewDate = past),    // Due
            TestUtils.createWordEntity(id = 3, nextReviewDate = future)   // Not due
        )
        dao.insert(words)
        
        // When: Get due cards
        val dueCards = dao.getDueCards(now).first()
        
        // Then: Should only get the 2 due cards
        assertEquals(2, dueCards.size)
    }
    
    @Test
    fun countDueCards_shouldReturnCorrectCount() = runTest {
        // Given: Words with different review dates
        val now = Clock.System.now().toEpochMilliseconds()
        val past = now - 1000 * 60 * 60
        val future = now + 1000 * 60 * 60
        
        val words = listOf(
            TestUtils.createWordEntity(id = 1, nextReviewDate = past),
            TestUtils.createWordEntity(id = 2, nextReviewDate = past),
            TestUtils.createWordEntity(id = 3, nextReviewDate = past),
            TestUtils.createWordEntity(id = 4, nextReviewDate = future)
        )
        dao.insert(words)
        
        // When: Count due cards
        val dueCount = dao.countDueCards(now)
        
        // Then: Should match
        assertEquals(3, dueCount)
    }
    
    @Test
    fun updateProgress_shouldUpdateAllFields() = runTest {
        // Given: A word at level 0
        val word = TestUtils.createWordEntity(
            id = 1,
            level = 0,
            easeFactor = 2.5f,
            interval = 0,
            repetitions = 0
        )
        dao.insert(word)
        
        val now = Clock.System.now().toEpochMilliseconds()
        val nextReview = now + 1000 * 60 * 10 // 10 minutes later
        
        // When: Update progress (simulating a successful review)
        dao.updateProgress(
            id = 1,
            level = 1,
            easeFactor = 2.6f,
            interval = 10,
            repetitions = 1,
            lastReviewDate = now,
            nextReviewDate = nextReview
        )
        
        // Then: All fields should be updated
        val updated = dao.getWordById(1)
        assertNotNull(updated)
        assertEquals(1, updated.level)
        assertEquals(2.6f, updated.easeFactor, 0.01f)
        assertEquals(10, updated.interval)
        assertEquals(1, updated.repetitions)
        assertEquals(now, updated.lastReviewDate)
        assertEquals(nextReview, updated.nextReviewDate)
    }
    
    @Test
    fun countLevelFunctions_shouldReturnCorrectCounts() = runTest {
        // Given: Words at all levels
        val words = listOf(
            TestUtils.createWordEntity(id = 1, level = 0),
            TestUtils.createWordEntity(id = 2, level = 0),
            TestUtils.createWordEntity(id = 3, level = 1),
            TestUtils.createWordEntity(id = 4, level = 2),
            TestUtils.createWordEntity(id = 5, level = 3),
            TestUtils.createWordEntity(id = 6, level = 4),
            TestUtils.createWordEntity(id = 7, level = 5),
            TestUtils.createWordEntity(id = 8, level = 6),
            TestUtils.createWordEntity(id = 9, level = 6)
        )
        dao.insert(words)
        
        // When/Then: Each count function should return correct count
        assertEquals(2, dao.countLevel0())
        assertEquals(1, dao.countLevel1())
        assertEquals(1, dao.countLevel2())
        assertEquals(1, dao.countLevel3())
        assertEquals(1, dao.countLevel4())
        assertEquals(1, dao.countLevel5())
        assertEquals(2, dao.countLevel6())
    }
    
    @Test
    fun upsert_shouldInsertNewOrUpdateExisting() = runTest {
        // Given: A word
        val word = TestUtils.createWordEntity(id = 1, originalWord = "test")
        
        // When: Upsert new word
        dao.upsert(word)
        
        // Then: Should be inserted
        val inserted = dao.getWordById(1)
        assertNotNull(inserted)
        assertEquals("test", inserted.originalWord)
        
        // When: Upsert again with same id but different content
        val updated = word.copy(originalWord = "updated")
        dao.upsert(updated)
        
        // Then: Should be updated (not duplicated)
        val afterUpsert = dao.getWordById(1)
        assertNotNull(afterUpsert)
        assertEquals("updated", afterUpsert.originalWord)
        
        // Should still have only 1 word
        val count = dao.count()
        assertEquals(1, count)
    }
}

