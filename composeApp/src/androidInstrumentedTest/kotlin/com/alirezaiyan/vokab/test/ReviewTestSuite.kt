package com.alirezaiyan.vokab.test

import com.alirezaiyan.vokab.test.database.LexiconDaoTest
import com.alirezaiyan.vokab.test.integration.EndToEndReviewTest
import com.alirezaiyan.vokab.test.usecase.ReviewWordUseCaseTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test Suite for Review Behavior Verification
 * 
 * This suite runs all tests related to the word review system, including:
 * 
 * 1. DATABASE TESTS (LexiconDaoTest):
 *    - CRUD operations
 *    - Query functions (getDueCards, getWordsByLevel, etc.)
 *    - Due cards filtering
 *    - Progress tracking
 *    - Level counting
 * 
 * 2. REVIEW LOGIC TESTS (ReviewWordUseCaseTest):
 *    - FORGOT response (quality = 0):
 *      * Drops 2 levels (minimum 0)
 *      * Resets repetitions
 *      * Decreases ease factor
 *    
 *    - REMEMBERED response (quality = 1):
 *      * Increments repetitions
 *      * Advances to next bucket after 2 successes
 *      * Improves ease factor
 *    
 *    - Complete bucket progression (0 → 6):
 *      * Level 0: 1 minute interval
 *      * Level 1: 10 minutes interval
 *      * Level 2: 1 day interval
 *      * Level 3: 3 days interval
 *      * Level 4: 7 days interval
 *      * Level 5: 14 days interval
 *      * Level 6: 30+ days (exponential growth)
 *    
 *    - Resting time verification for each level
 *    - Ease factor boundaries (1.3 - 2.5)
 *    - Level 6 exponential growth and 365-day cap
 * 
 * 3. END-TO-END INTEGRATION TESTS (EndToEndReviewTest):
 *    - Single word complete review flow
 *    - Multiple words independent progression
 *    - Due cards filtering and reviewing
 *    - Progress statistics updates
 *    - Real-world scenarios:
 *      * Daily review sessions
 *      * Struggling with words (forgetting multiple times)
 *      * Mastering words (consistent success)
 *      * Mixed level progression
 *    - Data persistence verification
 * 
 * HOW TO RUN:
 * 
 * In Android Studio:
 * 1. Right-click on this file → "Run 'ReviewTestSuite'"
 * 2. Or right-click on the 'test' package → "Run 'Tests in 'com.alirezaiyan.vokab.test''"
 * 
 * From Command Line:
 * ./gradlew :composeApp:connectedAndroidTest
 * 
 * Run specific test class:
 * ./gradlew :composeApp:connectedAndroidTest --tests "com.alirezaiyan.vokab.test.usecase.ReviewWordUseCaseTest"
 * 
 * Run specific test method:
 * ./gradlew :composeApp:connectedAndroidTest --tests "com.alirezaiyan.vokab.test.usecase.ReviewWordUseCaseTest.bucketProgression_level0_to_level1"
 * 
 * WHAT IS TESTED:
 * 
 * ✅ Bucket Progression: All transitions from Level 0 → 1 → 2 → 3 → 4 → 5 → 6
 * ✅ Resting Times: Verified for each level (minutes for 0-1, days for 2-6)
 * ✅ FORGOT Response: 2-level drops, repetition reset, ease factor decrease
 * ✅ REMEMBERED Response: Progression after 2 successes, ease factor increase
 * ✅ Edge Cases: Invalid quality, minimum/maximum ease factors, interval caps
 * ✅ Database Operations: All CRUD and query functions
 * ✅ Due Cards: Filtering, counting, and review flow
 * ✅ Progress Stats: Updates after each review
 * ✅ Real-World Scenarios: Daily sessions, struggles, mastery, mixed results
 * ✅ Data Persistence: Survives database reopen
 * 
 * TOTAL TESTS: 50+ comprehensive test cases
 * 
 * COVERAGE:
 * - All 7 bucket levels (0-6)
 * - All interval times (1 min, 10 min, 1 day, 3 days, 7 days, 14 days, 30+ days)
 * - Both response types (FORGOT = 0, REMEMBERED = 1)
 * - Edge cases and error conditions
 * - Complete user journeys from new word to mastered
 * - Database persistence and queries
 * - Statistics and progress tracking
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    LexiconDaoTest::class,
    ReviewWordUseCaseTest::class,
    EndToEndReviewTest::class
)
class ReviewTestSuite {
    // This class remains empty, used only as a holder for the above annotations
}

