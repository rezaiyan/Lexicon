package com.alirezaiyan.vokab.test.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import data.core.database.AppDatabase
import com.alirezaiyan.vokab.test.utils.TestUtils
import com.alirezaiyan.vokab.test.utils.createTestReviewSettingsUseCase
import data.word.repository.WordRepositoryImpl
import domain.word.usecase.ReviewWordUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ReviewWordUseCase response handling:
 * - FORGOT response (quality = 0) - drops 2 levels
 * - REMEMBERED response (quality = 1) - advances with successes
 * - Edge cases (invalid quality)
 */
@RunWith(AndroidJUnit4::class)
class ReviewWordResponseTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: WordRepositoryImpl
    private lateinit var reviewUseCase: ReviewWordUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        repository = TestUtils.createWordRepository(database.getDao())
        val reviewSettings = createTestReviewSettingsUseCase()
        reviewUseCase = ReviewWordUseCase(repository, reviewSettings)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== FORGOT Response Tests (quality = 0) ==========

    @Test
    fun forgotResponse_level0_staysAtLevel0() = runTest {
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 0)

        val updated = repository.getWordById(1)!!
        assertEquals(0, updated.level, "Word should stay at level 0")
        assertEquals(0, updated.repetitions, "Repetitions should reset to 0")
        assertTrue(updated.easeFactor < word.easeFactor, "Ease factor should decrease")
    }

    @Test
    fun forgotResponse_level2_dropsTo0() = runTest {
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 1, easeFactor = 2.5f)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 0)

        val updated = repository.getWordById(1)!!
        assertEquals(0, updated.level, "Word should drop from level 2 to 0")
        assertEquals(0, updated.repetitions, "Repetitions should reset")
        assertEquals(2.3f, updated.easeFactor, 0.01f, "Ease factor should decrease by 0.2")
    }

    @Test
    fun forgotResponse_level5_dropsTo3() = runTest {
        val word = TestUtils.createWord(id = 1, level = 5, repetitions = 3, easeFactor = 2.5f)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 0)

        val updated = repository.getWordById(1)!!
        assertEquals(3, updated.level, "Word should drop from level 5 to 3")
        assertEquals(0, updated.repetitions, "Repetitions should reset")
        assertTrue(updated.easeFactor < word.easeFactor)
    }

    @Test
    fun forgotResponse_level6_dropsTo4() = runTest {
        val word = TestUtils.createWord(id = 1, level = 6, repetitions = 5, easeFactor = 2.5f)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 0)

        val updated = repository.getWordById(1)!!
        assertEquals(4, updated.level, "Word should drop from level 6 to 4")
        assertEquals(0, updated.repetitions, "Repetitions should reset")
    }

    @Test
    fun forgotResponse_easeFactorMinimumIs1_3() = runTest {
        val word = TestUtils.createWord(id = 1, level = 2, easeFactor = 1.4f)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 0)

        val updated = repository.getWordById(1)!!
        assertTrue(updated.easeFactor >= 1.3f, "Ease factor should not go below 1.3")
        assertEquals(1.3f, updated.easeFactor, 0.01f)
    }

    // ========== REMEMBERED Response Tests (quality = 1) ==========

    @Test
    fun rememberedResponse_firstSuccess_advancesToNextLevel() = runTest {
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(1, updated.level, "Should advance to level 1 after 1 success")
        assertEquals(0, updated.repetitions, "Repetitions reset for new level")
    }

    @Test
    fun rememberedResponse_alwaysAdvances_withOneSuccess() = runTest {
        val word = TestUtils.createWord(id = 1, level = 0, repetitions = 0, easeFactor = 2.4f)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertEquals(1, updated.level, "Should advance from level 0 to level 1 after 1 success")
        assertEquals(0, updated.repetitions, "Repetitions should reset to 0 for new level")
        assertTrue(updated.easeFactor >= word.easeFactor, "Ease factor should increase or stay at max (2.5)")
        assertEquals(2.5f, updated.easeFactor, 0.01f, "Ease factor should reach 2.5")
    }

    @Test
    fun rememberedResponse_easeFactorMaximumIs2_5() = runTest {
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 1, easeFactor = 2.5f)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 1)

        val updated = repository.getWordById(1)!!
        assertTrue(updated.easeFactor <= 2.5f, "Ease factor should not exceed 2.5")
        assertEquals(2.5f, updated.easeFactor, 0.01f)
    }

    // ========== Edge Cases ==========

    @Test
    fun invalidQuality_treatedAsForgot() = runTest {
        val word = TestUtils.createWord(id = 1, level = 2, repetitions = 0)
        repository.insertWords(listOf(word))

        reviewUseCase(word, quality = 99)

        val updated = repository.getWordById(1)!!
        assertEquals(0, updated.level, "Invalid quality should be treated as FORGOT")
    }
}
