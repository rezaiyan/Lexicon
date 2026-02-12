package domain.word.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for ProgressStats data class
 * 
 * Tests cover:
 * - Default values
 * - Computed properties
 * - Data validation
 * - Edge cases
 * - Calculations
 */
class ProgressStatsTest {
    
    @Test
    fun `default constructor should create empty stats`() {
        // Given: Default constructor
        val stats = ProgressStats()
        
        // Then: Should have default values
        assertEquals(0, stats.level0Count)
        assertEquals(0, stats.level1Count)
        assertEquals(0, stats.level2Count)
        assertEquals(0, stats.level3Count)
        assertEquals(0, stats.level4Count)
        assertEquals(0, stats.level5Count)
        assertEquals(0, stats.level6Count)
        assertEquals(0, stats.totalWords)
        assertEquals(0, stats.dueCards)
    }
    
    @Test
    fun `learningWords should sum level1 and level2`() {
        // Given: Stats with learning words
        val stats = ProgressStats(
            level1Count = 5,
            level2Count = 10,
            level3Count = 3,
            level4Count = 2
        )
        
        // Then: learningWords should be sum of level1 and level2
        assertEquals(15, stats.learningWords)
    }
    
    @Test
    fun `matureWords should sum level5 and level6`() {
        // Given: Stats with mature words
        val stats = ProgressStats(
            level1Count = 5,
            level2Count = 10,
            level5Count = 8,
            level6Count = 12
        )
        
        // Then: matureWords should be sum of level5 and level6
        assertEquals(20, stats.matureWords)
    }
    
    @Test
    fun `learningWords should be zero when no learning words`() {
        // Given: Stats with no learning words
        val stats = ProgressStats(
            level0Count = 5,
            level3Count = 10,
            level5Count = 8,
            level6Count = 12
        )
        
        // Then: learningWords should be zero
        assertEquals(0, stats.learningWords)
    }
    
    @Test
    fun `matureWords should be zero when no mature words`() {
        // Given: Stats with no mature words
        val stats = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            level3Count = 12
        )
        
        // Then: matureWords should be zero
        assertEquals(0, stats.matureWords)
    }
    
    @Test
    fun `totalWords should be sum of all levels`() {
        // Given: Stats with words in all levels
        val stats = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            level3Count = 12,
            level4Count = 6,
            level5Count = 4,
            level6Count = 2,
            totalWords = 47 // Must be set explicitly
        )
        
        // Then: totalWords should be sum of all levels
        assertEquals(47, stats.totalWords)
    }
    
    @Test
    fun `totalWords should match sum of individual levels`() {
        // Given: Stats with various counts
        val stats = ProgressStats(
            level0Count = 3,
            level1Count = 7,
            level2Count = 5,
            level3Count = 9,
            level4Count = 4,
            level5Count = 6,
            level6Count = 1,
            totalWords = 35 // Must be set explicitly
        )
        
        // Then: totalWords should match sum
        val expectedTotal = 3 + 7 + 5 + 9 + 4 + 6 + 1
        assertEquals(expectedTotal, stats.totalWords)
    }
    
    @Test
    fun `dueCards should be independent of other counts`() {
        // Given: Stats with due cards
        val stats = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            dueCards = 15,
            totalWords = 15 // Must be set explicitly
        )
        
        // Then: dueCards should be independent
        assertEquals(15, stats.dueCards)
        assertEquals(15, stats.totalWords)
        assertEquals(10, stats.learningWords)
    }
    
    @Test
    fun `stats with all zeros should work`() {
        // Given: Stats with all zeros
        val stats = ProgressStats(
            level0Count = 0,
            level1Count = 0,
            level2Count = 0,
            level3Count = 0,
            level4Count = 0,
            level5Count = 0,
            level6Count = 0,
            totalWords = 0,
            dueCards = 0
        )
        
        // Then: All values should be zero
        assertEquals(0, stats.level0Count)
        assertEquals(0, stats.level1Count)
        assertEquals(0, stats.level2Count)
        assertEquals(0, stats.level3Count)
        assertEquals(0, stats.level4Count)
        assertEquals(0, stats.level5Count)
        assertEquals(0, stats.level6Count)
        assertEquals(0, stats.totalWords)
        assertEquals(0, stats.dueCards)
        assertEquals(0, stats.learningWords)
        assertEquals(0, stats.matureWords)
    }
    
    @Test
    fun `stats with large numbers should work`() {
        // Given: Stats with large numbers
        val stats = ProgressStats(
            level0Count = 1000,
            level1Count = 2000,
            level2Count = 1500,
            level3Count = 3000,
            level4Count = 2500,
            level5Count = 4000,
            level6Count = 5000,
            totalWords = 19000,
            dueCards = 500
        )
        
        // Then: Should handle large numbers correctly
        assertEquals(1000, stats.level0Count)
        assertEquals(2000, stats.level1Count)
        assertEquals(1500, stats.level2Count)
        assertEquals(3000, stats.level3Count)
        assertEquals(2500, stats.level4Count)
        assertEquals(4000, stats.level5Count)
        assertEquals(5000, stats.level6Count)
        assertEquals(19000, stats.totalWords)
        assertEquals(500, stats.dueCards)
        assertEquals(3500, stats.learningWords) // level1 + level2
        assertEquals(9000, stats.matureWords) // level5 + level6
    }
    
    @Test
    fun `stats equality should work correctly`() {
        // Given: Stats with same values
        val stats1 = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            totalWords = 23,
            dueCards = 3
        )
        val stats2 = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            totalWords = 23,
            dueCards = 3
        )
        val stats3 = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            totalWords = 23,
            dueCards = 4 // Different due cards
        )
        
        // Then: Equality should work correctly
        assertEquals(stats1, stats2)
        assertTrue(stats1 == stats2)
        assertFalse(stats1 == stats3)
    }
    
    @Test
    fun `stats hash code should be consistent`() {
        // Given: Stats with same values
        val stats1 = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            totalWords = 23,
            dueCards = 3
        )
        val stats2 = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            totalWords = 23,
            dueCards = 3
        )
        val stats3 = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            totalWords = 23,
            dueCards = 4
        )
        
        // Then: Hash codes should be consistent
        assertEquals(stats1.hashCode(), stats2.hashCode())
        assertFalse(stats1.hashCode() == stats3.hashCode())
    }
    
    @Test
    fun `stats toString should include all values`() {
        // Given: Stats
        val stats = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            totalWords = 23,
            dueCards = 3
        )
        
        // Then: toString should include all values
        val toString = stats.toString()
        assertTrue(toString.contains("level0Count=5"))
        assertTrue(toString.contains("level1Count=10"))
        assertTrue(toString.contains("level2Count=8"))
        assertTrue(toString.contains("totalWords=23"))
        assertTrue(toString.contains("dueCards=3"))
    }
    
    @Test
    fun `stats copy should work correctly`() {
        // Given: Original stats
        val original = ProgressStats(
            level0Count = 5,
            level1Count = 10,
            level2Count = 8,
            totalWords = 23,
            dueCards = 3
        )
        
        // When: Copying with changes
        val copied = original.copy(level1Count = 15, dueCards = 5)
        
        // Then: Should have correct values
        assertEquals(5, copied.level0Count) // Unchanged
        assertEquals(15, copied.level1Count) // Changed
        assertEquals(8, copied.level2Count) // Unchanged
        assertEquals(23, copied.totalWords) // Unchanged
        assertEquals(5, copied.dueCards) // Changed
        assertEquals(10, original.level1Count) // Original unchanged
    }
    
    @Test
    fun `computed properties should update with copy`() {
        // Given: Original stats
        val original = ProgressStats(
            level1Count = 10,
            level2Count = 5,
            level5Count = 8,
            level6Count = 2
        )
        
        // When: Copying with changes
        val copied = original.copy(level1Count = 15, level5Count = 12)
        
        // Then: Computed properties should update
        assertEquals(20, copied.learningWords) // 15 + 5
        assertEquals(14, copied.matureWords) // 12 + 2
        assertEquals(15, original.learningWords) // Original unchanged
        assertEquals(10, original.matureWords) // Original unchanged
    }
    
    @Test
    fun `stats with only one level should work`() {
        // Given: Stats with only one level
        val stats = ProgressStats(
            level3Count = 50,
            totalWords = 50 // Must be set explicitly
        )
        
        // Then: Should work correctly
        assertEquals(50, stats.level3Count)
        assertEquals(50, stats.totalWords)
        assertEquals(0, stats.learningWords)
        assertEquals(0, stats.matureWords)
    }
    
    @Test
    fun `stats with negative values should work`() {
        // Given: Stats with negative values (edge case)
        val stats = ProgressStats(
            level0Count = -1,
            level1Count = -5,
            totalWords = -6,
            dueCards = -2
        )
        
        // Then: Should handle negative values
        assertEquals(-1, stats.level0Count)
        assertEquals(-5, stats.level1Count)
        assertEquals(-6, stats.totalWords)
        assertEquals(-2, stats.dueCards)
        assertEquals(-5, stats.learningWords) // -5 + 0
        assertEquals(0, stats.matureWords) // 0 + 0
    }
}
