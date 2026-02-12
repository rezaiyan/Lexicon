package com.alirezaiyan.vokab.test.database

import data.core.database.WordEntity
import com.alirezaiyan.vokab.test.utils.TestConstants
import com.alirezaiyan.vokab.test.utils.TestUtils
import kotlin.time.Clock

object WordOrderingTestHelpers {
    
    fun getCurrentTime(): Long = Clock.System.now().toEpochMilliseconds()
    
    fun createDueWord(id: Int, level: Int, hoursAgo: Int, now: Long): WordEntity {
        val dueDate = now - (hoursAgo * TestConstants.MILLIS_PER_HOUR)
        val lastReview = now - ((hoursAgo + 1) * TestConstants.MILLIS_PER_HOUR)
        return TestUtils.createWordEntity(
            id = id,
            level = level,
            nextReviewDate = dueDate,
            lastReviewDate = lastReview
        )
    }
    
    fun createNonDueWord(id: Int, level: Int, hoursInFuture: Int, hoursAgoReviewed: Int, now: Long): WordEntity {
        val futureDate = now + (hoursInFuture * TestConstants.MILLIS_PER_HOUR)
        val lastReview = now - (hoursAgoReviewed * TestConstants.MILLIS_PER_HOUR)
        return TestUtils.createWordEntity(
            id = id,
            level = level,
            nextReviewDate = futureDate,
            lastReviewDate = lastReview
        )
    }
    
    fun createNeverReviewedDueWord(id: Int, level: Int, hoursAgo: Int, daysAgoAdded: Int, now: Long): WordEntity {
        val dueDate = now - (hoursAgo * TestConstants.MILLIS_PER_HOUR)
        val dateAdded = now - (daysAgoAdded * TestConstants.MILLIS_PER_DAY)
        return TestUtils.createWordEntity(
            id = id,
            level = level,
            nextReviewDate = dueDate,
            lastReviewDate = 0L,
            repetitions = 0,
            dateAdded = dateAdded
        )
    }
    
}

