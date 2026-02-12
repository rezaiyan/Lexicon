package domain.word.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for LearningStage enum
 * 
 * Tests cover:
 * - Level mapping
 * - Enum values
 * - Companion object functions
 * - Edge cases
 */
class LearningStageTest {
    
    @Test
    fun `all learning stages should have correct levels`() {
        // Given: All learning stages
        val stages = LearningStage.entries
        
        // Then: Each should have correct level
        assertEquals(0, LearningStage.LEVEL_0_FRESH.level)
        assertEquals(1, LearningStage.LEVEL_1_LEARNING.level)
        assertEquals(2, LearningStage.LEVEL_2_FAMILIAR.level)
        assertEquals(3, LearningStage.LEVEL_3_BUILDING.level)
        assertEquals(4, LearningStage.LEVEL_4_ALMOST.level)
        assertEquals(5, LearningStage.LEVEL_5_STRONG.level)
        assertEquals(6, LearningStage.LEVEL_6_MASTERED.level)
    }
    
    @Test
    fun `fromLevel should return correct stage for valid levels`() {
        // Given: Valid levels
        val testCases = mapOf(
            0 to LearningStage.LEVEL_0_FRESH,
            1 to LearningStage.LEVEL_1_LEARNING,
            2 to LearningStage.LEVEL_2_FAMILIAR,
            3 to LearningStage.LEVEL_3_BUILDING,
            4 to LearningStage.LEVEL_4_ALMOST,
            5 to LearningStage.LEVEL_5_STRONG,
            6 to LearningStage.LEVEL_6_MASTERED
        )
        
        // When/Then: Each level should map to correct stage
        testCases.forEach { (level, expectedStage) ->
            assertEquals(expectedStage, LearningStage.fromLevel(level))
        }
    }
    
    @Test
    fun `fromLevel should return LEVEL_0_FRESH for invalid levels`() {
        // Given: Invalid levels
        val invalidLevels = listOf(-1, -10, 7, 10, 100, Int.MAX_VALUE, Int.MIN_VALUE)
        
        // When/Then: Each invalid level should return LEVEL_0_FRESH
        invalidLevels.forEach { level ->
            assertEquals(LearningStage.LEVEL_0_FRESH, LearningStage.fromLevel(level))
        }
    }
    
    @Test
    fun `all stages should be unique`() {
        // Given: All learning stages
        val stages = LearningStage.entries
        
        // Then: Each stage should have unique level
        val levels = stages.map { it.level }
        val uniqueLevels = levels.distinct()
        
        assertEquals(levels.size, uniqueLevels.size, "All stages should have unique levels")
    }
    
    @Test
    fun `stages should be ordered correctly`() {
        // Given: All learning stages
        val stages = LearningStage.entries
        
        // Then: Levels should be in ascending order
        for (i in 1 until stages.size) {
            assertTrue(
                stages[i].level > stages[i - 1].level,
                "Stage ${stages[i]} should have higher level than ${stages[i - 1]}"
            )
        }
    }
    
    @Test
    fun `level range should be 0 to 6`() {
        // Given: All learning stages
        val stages = LearningStage.entries
        
        // Then: Levels should be in range 0-6
        val levels = stages.map { it.level }
        val minLevel = levels.minOrNull() ?: Int.MAX_VALUE
        val maxLevel = levels.maxOrNull() ?: Int.MIN_VALUE
        
        assertEquals(0, minLevel, "Minimum level should be 0")
        assertEquals(6, maxLevel, "Maximum level should be 6")
    }
    
    @Test
    fun `stage names should be descriptive`() {
        // Given: Learning stages
        val stages = LearningStage.entries
        
        // Then: Names should be descriptive and follow pattern
        assertTrue(LearningStage.LEVEL_0_FRESH.name.contains("FRESH"))
        assertTrue(LearningStage.LEVEL_1_LEARNING.name.contains("LEARNING"))
        assertTrue(LearningStage.LEVEL_2_FAMILIAR.name.contains("FAMILIAR"))
        assertTrue(LearningStage.LEVEL_3_BUILDING.name.contains("BUILDING"))
        assertTrue(LearningStage.LEVEL_4_ALMOST.name.contains("ALMOST"))
        assertTrue(LearningStage.LEVEL_5_STRONG.name.contains("STRONG"))
        assertTrue(LearningStage.LEVEL_6_MASTERED.name.contains("MASTERED"))
    }
    
    @Test
    fun `stage progression should be logical`() {
        // Given: Learning stages in order
        val stages = listOf(
            LearningStage.LEVEL_0_FRESH,
            LearningStage.LEVEL_1_LEARNING,
            LearningStage.LEVEL_2_FAMILIAR,
            LearningStage.LEVEL_3_BUILDING,
            LearningStage.LEVEL_4_ALMOST,
            LearningStage.LEVEL_5_STRONG,
            LearningStage.LEVEL_6_MASTERED
        )
        
        // Then: Each stage should have incrementally higher level
        for (i in 1 until stages.size) {
            assertEquals(
                stages[i - 1].level + 1,
                stages[i].level,
                "Stage ${stages[i]} should have level ${stages[i - 1].level + 1}"
            )
        }
    }
    
    @Test
    fun `fromLevel should handle edge cases`() {
        // Given: Edge case levels
        val edgeCases = mapOf(
            0 to LearningStage.LEVEL_0_FRESH,
            6 to LearningStage.LEVEL_6_MASTERED,
            -1 to LearningStage.LEVEL_0_FRESH,
            7 to LearningStage.LEVEL_0_FRESH
        )
        
        // When/Then: Edge cases should be handled correctly
        edgeCases.forEach { (level, expectedStage) ->
            assertEquals(expectedStage, LearningStage.fromLevel(level))
        }
    }
    
    @Test
    fun `enum should have exactly 7 stages`() {
        // Given: LearningStage enum
        val stages = LearningStage.entries
        
        // Then: Should have exactly 7 stages
        assertEquals(7, stages.size, "Should have exactly 7 learning stages")
    }
    
    @Test
    fun `stage equality should work correctly`() {
        // Given: Learning stages
        val stage1 = LearningStage.LEVEL_2_FAMILIAR
        val stage2 = LearningStage.LEVEL_2_FAMILIAR
        val stage3 = LearningStage.LEVEL_3_BUILDING
        
        // Then: Equality should work correctly
        assertEquals(stage1, stage2)
        assertTrue(stage1 == stage2)
        assertFalse(stage1 == stage3)
        assertFalse(stage1.equals(stage3))
    }
    
    @Test
    fun `stage hash codes should be consistent`() {
        // Given: Learning stages
        val stage1 = LearningStage.LEVEL_2_FAMILIAR
        val stage2 = LearningStage.LEVEL_2_FAMILIAR
        val stage3 = LearningStage.LEVEL_3_BUILDING
        
        // Then: Hash codes should be consistent
        assertEquals(stage1.hashCode(), stage2.hashCode())
        assertFalse(stage1.hashCode() == stage3.hashCode())
    }
    
    @Test
    fun `stage toString should return name`() {
        // Given: Learning stages
        val stage = LearningStage.LEVEL_4_ALMOST
        
        // Then: toString should return the name
        assertEquals("LEVEL_4_ALMOST", stage.toString())
    }
    
    @Test
    fun `stage ordinal should match level`() {
        // Given: Learning stages
        val stages = LearningStage.entries
        
        // Then: Ordinal should match level
        stages.forEach { stage ->
            assertEquals(stage.level, stage.ordinal, "Ordinal should match level for ${stage}")
        }
    }
}

