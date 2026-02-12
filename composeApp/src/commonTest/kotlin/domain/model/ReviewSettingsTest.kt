package domain.settings.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Comprehensive tests for ReviewSettings data class
 * 
 * Tests cover:
 * - Constructor validation
 * - Preset modes
 * - Default values
 * - Edge cases
 * - Validation rules
 */
class ReviewSettingsTest {
    
    @Test
    fun `default constructor should create balanced settings`() {
        // Given: Default constructor
        val settings = ReviewSettings()
        
        // Then: Should have default values
        assertEquals(1, settings.successesToAdvance)
        assertEquals(2, settings.forgotPenalty)
    }
    
    @Test
    fun `valid settings should be created successfully`() {
        // Given: Valid parameters
        val settings = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        
        // Then: Should have correct values
        assertEquals(2, settings.successesToAdvance)
        assertEquals(3, settings.forgotPenalty)
    }
    
    @Test
    fun `successes to advance below minimum should throw exception`() {
        // Given: Invalid successes to advance
        val invalidValues = listOf(0, -1, -10)
        
        // When/Then: Each should throw exception
        invalidValues.forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                ReviewSettings(successesToAdvance = value, forgotPenalty = 2)
            }
        }
    }
    
    @Test
    fun `successes to advance above maximum should throw exception`() {
        // Given: Invalid successes to advance
        val invalidValues = listOf(4, 5, 10, Int.MAX_VALUE)
        
        // When/Then: Each should throw exception
        invalidValues.forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                ReviewSettings(successesToAdvance = value, forgotPenalty = 2)
            }
        }
    }
    
    @Test
    fun `forgot penalty below minimum should throw exception`() {
        // Given: Invalid forgot penalty
        val invalidValues = listOf(0, -1, -10)
        
        // When/Then: Each should throw exception
        invalidValues.forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                ReviewSettings(successesToAdvance = 1, forgotPenalty = value)
            }
        }
    }
    
    @Test
    fun `forgot penalty above maximum should throw exception`() {
        // Given: Invalid forgot penalty
        val invalidValues = listOf(4, 5, 10, Int.MAX_VALUE)
        
        // When/Then: Each should throw exception
        invalidValues.forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                ReviewSettings(successesToAdvance = 1, forgotPenalty = value)
            }
        }
    }
    
    @Test
    fun `easy mode should have correct values`() {
        // Given: Easy mode
        val easy = ReviewSettings.EASY
        
        // Then: Should have easy values
        assertEquals(1, easy.successesToAdvance)
        assertEquals(1, easy.forgotPenalty)
    }
    
    @Test
    fun `balanced mode should have correct values`() {
        // Given: Balanced mode
        val balanced = ReviewSettings.BALANCED
        
        // Then: Should have balanced values
        assertEquals(1, balanced.successesToAdvance)
        assertEquals(2, balanced.forgotPenalty)
    }
    
    @Test
    fun `rigorous mode should have correct values`() {
        // Given: Rigorous mode
        val rigorous = ReviewSettings.RIGOROUS
        
        // Then: Should have rigorous values
        assertEquals(2, rigorous.successesToAdvance)
        assertEquals(3, rigorous.forgotPenalty)
    }
    
    @Test
    fun `expert mode should have correct values`() {
        // Given: Expert mode
        val expert = ReviewSettings.EXPERT
        
        // Then: Should have expert values
        assertEquals(3, expert.successesToAdvance)
        assertEquals(3, expert.forgotPenalty)
    }
    
    @Test
    fun `preset modes should be valid`() {
        // Given: All preset modes
        val presets = listOf(
            ReviewSettings.EASY,
            ReviewSettings.BALANCED,
            ReviewSettings.RIGOROUS,
            ReviewSettings.EXPERT
        )
        
        // Then: All should be valid (no exceptions thrown)
        presets.forEach { preset ->
            // Validation happens in constructor, so if we get here, it's valid
            assertTrue(preset.successesToAdvance in 1..3)
            assertTrue(preset.forgotPenalty in 1..3)
        }
    }
    
    @Test
    fun `preset modes should have different characteristics`() {
        // Given: Preset modes
        val easy = ReviewSettings.EASY
        val balanced = ReviewSettings.BALANCED
        val rigorous = ReviewSettings.RIGOROUS
        val expert = ReviewSettings.EXPERT
        
        // Then: Should have different characteristics
        assertTrue(easy.successesToAdvance <= balanced.successesToAdvance)
        assertTrue(balanced.successesToAdvance <= rigorous.successesToAdvance)
        assertTrue(rigorous.successesToAdvance <= expert.successesToAdvance)
        
        assertTrue(easy.forgotPenalty <= balanced.forgotPenalty)
        assertTrue(balanced.forgotPenalty <= rigorous.forgotPenalty)
        assertTrue(rigorous.forgotPenalty <= expert.forgotPenalty)
    }
    
    @Test
    fun `settings equality should work correctly`() {
        // Given: Settings with same values
        val settings1 = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        val settings2 = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        val settings3 = ReviewSettings(successesToAdvance = 1, forgotPenalty = 2)
        
        // Then: Equality should work correctly
        assertEquals(settings1, settings2)
        assertTrue(settings1 == settings2)
        assertFalse(settings1 == settings3)
    }
    
    @Test
    fun `settings hash code should be consistent`() {
        // Given: Settings with same values
        val settings1 = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        val settings2 = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        val settings3 = ReviewSettings(successesToAdvance = 1, forgotPenalty = 2)
        
        // Then: Hash codes should be consistent
        assertEquals(settings1.hashCode(), settings2.hashCode())
        assertFalse(settings1.hashCode() == settings3.hashCode())
    }
    
    @Test
    fun `settings toString should include both values`() {
        // Given: Settings
        val settings = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        
        // Then: toString should include both values
        val toString = settings.toString()
        assertTrue(toString.contains("successesToAdvance=2"))
        assertTrue(toString.contains("forgotPenalty=3"))
    }
    
    @Test
    fun `settings copy should work correctly`() {
        // Given: Original settings
        val original = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        
        // When: Copying with changes
        val copied = original.copy(successesToAdvance = 1)
        
        // Then: Should have correct values
        assertEquals(1, copied.successesToAdvance)
        assertEquals(3, copied.forgotPenalty)
        assertEquals(2, original.successesToAdvance) // Original unchanged
    }
    
    @Test
    fun `settings copy with invalid values should throw exception`() {
        // Given: Valid settings
        val settings = ReviewSettings(successesToAdvance = 2, forgotPenalty = 3)
        
        // When/Then: Copying with invalid values should throw exception
        assertFailsWith<IllegalArgumentException> {
            settings.copy(successesToAdvance = 0)
        }
        
        assertFailsWith<IllegalArgumentException> {
            settings.copy(forgotPenalty = 4)
        }
    }
    
    @Test
    fun `minimum valid values should work`() {
        // Given: Minimum valid values
        val settings = ReviewSettings(successesToAdvance = 1, forgotPenalty = 1)
        
        // Then: Should be valid
        assertEquals(1, settings.successesToAdvance)
        assertEquals(1, settings.forgotPenalty)
    }
    
    @Test
    fun `maximum valid values should work`() {
        // Given: Maximum valid values
        val settings = ReviewSettings(successesToAdvance = 3, forgotPenalty = 3)
        
        // Then: Should be valid
        assertEquals(3, settings.successesToAdvance)
        assertEquals(3, settings.forgotPenalty)
    }
    
    @Test
    fun `all valid combinations should work`() {
        // Given: All valid combinations
        val validCombinations = listOf(
            Pair(1, 1), Pair(1, 2), Pair(1, 3),
            Pair(2, 1), Pair(2, 2), Pair(2, 3),
            Pair(3, 1), Pair(3, 2), Pair(3, 3)
        )
        
        // When/Then: Each combination should work
        validCombinations.forEach { (successes, penalty) ->
            val settings = ReviewSettings(successesToAdvance = successes, forgotPenalty = penalty)
            assertEquals(successes, settings.successesToAdvance)
            assertEquals(penalty, settings.forgotPenalty)
        }
    }
    
    @Test
    fun `preset modes should be immutable`() {
        // Given: Preset modes
        val easy = ReviewSettings.EASY
        val balanced = ReviewSettings.BALANCED
        
        // When: Attempting to modify (this should create new instances)
        val modifiedEasy = easy.copy(successesToAdvance = 2)
        
        // Then: Original should be unchanged
        assertEquals(1, easy.successesToAdvance)
        assertEquals(1, easy.forgotPenalty)
        assertEquals(2, modifiedEasy.successesToAdvance)
        assertEquals(1, modifiedEasy.forgotPenalty)
    }
}
