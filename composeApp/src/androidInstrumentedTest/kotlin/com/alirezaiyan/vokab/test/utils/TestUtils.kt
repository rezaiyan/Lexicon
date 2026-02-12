package com.alirezaiyan.vokab.test.utils

import data.core.database.WordEntity
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_DESCRIPTION
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_EASE_FACTOR
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_INTERVAL
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_LAST_REVIEW_DATE
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_LEVEL
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_ORIGINAL_WORD
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_REPETITIONS
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_SOURCE_LANGUAGE
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_TARGET_LANGUAGE
import com.alirezaiyan.vokab.test.utils.TestConstants.DEFAULT_TRANSLATION
import data.word.repository.WordRepositoryImpl
import domain.settings.model.ReviewSettings
import domain.word.model.Word
import kotlin.time.Clock

/**
 * Test utilities for creating test data and helper functions
 */
object TestUtils {
    
    /**
     * Default test review settings (matches current test expectations)
     */
    val DEFAULT_TEST_SETTINGS = ReviewSettings(
        successesToAdvance = 1,
        forgotPenalty = 2
    )
    
    /**
     * Create a test WordEntity with custom parameters
     */
    fun createWordEntity(
        id: Int = 1,
        originalWord: String = DEFAULT_ORIGINAL_WORD,
        translation: String = DEFAULT_TRANSLATION,
        description: String = DEFAULT_DESCRIPTION,
        sourceLanguage: String = DEFAULT_SOURCE_LANGUAGE,
        targetLanguage: String = DEFAULT_TARGET_LANGUAGE,
        level: Int = DEFAULT_LEVEL,
        easeFactor: Float = DEFAULT_EASE_FACTOR,
        interval: Int = DEFAULT_INTERVAL,
        repetitions: Int = DEFAULT_REPETITIONS,
        lastReviewDate: Long = DEFAULT_LAST_REVIEW_DATE,
        nextReviewDate: Long = Clock.System.now().toEpochMilliseconds(),
        dateAdded: Long = Clock.System.now().toEpochMilliseconds()
    ): WordEntity {
        return WordEntity(
            id = id,
            originalWord = originalWord,
            translation = translation,
            description = description,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            level = level,
            easeFactor = easeFactor,
            interval = interval,
            repetitions = repetitions,
            lastReviewDate = lastReviewDate,
            nextReviewDate = nextReviewDate,
            dateAdded = dateAdded
        )
    }
    
    /**
     * Create a test Word domain model with custom parameters
     */
    fun createWord(
        id: Int = 1,
        originalWord: String = DEFAULT_ORIGINAL_WORD,
        translation: String = DEFAULT_TRANSLATION,
        description: String = DEFAULT_DESCRIPTION,
        sourceLanguage: String = DEFAULT_SOURCE_LANGUAGE,
        targetLanguage: String = DEFAULT_TARGET_LANGUAGE,
        level: Int = DEFAULT_LEVEL,
        easeFactor: Float = DEFAULT_EASE_FACTOR,
        interval: Int = DEFAULT_INTERVAL,
        repetitions: Int = DEFAULT_REPETITIONS,
        lastReviewDate: Long = DEFAULT_LAST_REVIEW_DATE,
        nextReviewDate: Long = Clock.System.now().toEpochMilliseconds(),
        dateAdded: Long = Clock.System.now().toEpochMilliseconds()
    ): Word {
        return Word(
            id = id,
            originalWord = originalWord,
            translation = translation,
            description = description,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            level = level,
            easeFactor = easeFactor,
            interval = interval,
            repetitions = repetitions,
            lastReviewDate = lastReviewDate,
            nextReviewDate = nextReviewDate,
            dateAdded = dateAdded
        )
    }
    
    fun getIntervalForLevel(level: Int): Int {
        return when (level) {
            0 -> 1      // 1 minute
            1 -> 10     // 10 minutes
            2 -> 1      // 1 day
            3 -> 3      // 3 days
            4 -> 7      // 1 week
            5 -> 14     // 2 weeks
            6 -> 30     // 1 month
            else -> 1
        }
    }
    
    fun millisToDays(millis: Long): Long {
        return millis / TestConstants.MILLIS_PER_DAY
    }
    
    /**
     * Convert milliseconds to minutes for testing minute-based intervals
     */
    fun millisToMinutes(millis: Long): Long {
        return millis / TestConstants.MILLIS_PER_MINUTE
    }
    
    /**
     * Create a WordRepositoryImpl with test doubles for testing
     * Uses TestDoubles for dependency injection following Dependency Inversion Principle
     */
    fun createWordRepository(dao: data.core.database.LexiconDao): WordRepositoryImpl {
        return WordRepositoryImpl(
            localDataSource = data.word.local.WordLocalDataSource(dao),
            remoteSyncHandler = )
    }
}

