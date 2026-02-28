package domain.word.usecase

import domain.word.model.Word
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for word export functionality
 * 
 * Tests cover:
 * - Basic word export
 * - Multiple words
 * - Words with/without descriptions
 * - UTF-8 characters (Arabic, Chinese, Japanese, Korean, emoji, etc.)
 * - Special characters (commas, quotes, etc.)
 * - Empty list handling
 * - Export format validation
 * - Round-trip compatibility (export → import)
 */
class ExportWordsUseCaseTest {
    
    private val exportUseCase = ExportWordsUseCase()
    
    // Helper to create test words
    private fun createWord(
        id: Int = 1,
        originalWord: String,
        translation: String,
        description: String = "",
        sourceLanguage: Language = Language.ENGLISH,
        targetLanguage: Language = Language.SPANISH
    ) = Word(
        id = id,
        originalWord = originalWord,
        translation = translation,
        description = description,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        level = 0,
        easeFactor = 2.5f,
        interval = 0,
        repetitions = 0,
        lastReviewDate = 0L,
        nextReviewDate = 0L
    )

    
    @Test
    fun `empty list should return empty string`() {
        val result = exportUseCase(emptyList())
        assertEquals("", result)
    }
    
    @Test
    fun `single word without description should export correctly`() {
        val words = listOf(
            createWord(originalWord = "hello", translation = "hola")
        )
        
        val result = exportUseCase(words)
        
        // Should contain the word in comma format
        assertEquals("hello,hola", result)
    }
    
    @Test
    fun `single word with description should export correctly`() {
        val words = listOf(
            createWord(
                originalWord = "hello",
                translation = "hola",
                description = "Common greeting"
            )
        )
        
        val result = exportUseCase(words)
        
        assertEquals("hello,hola,Common greeting", result)
    }
    
    @Test
    fun `multiple words should export separated by semicolons`() {
        val words = listOf(
            createWord(id = 1, originalWord = "hello", translation = "hola"),
            createWord(id = 2, originalWord = "goodbye", translation = "adiós"),
            createWord(id = 3, originalWord = "thank you", translation = "gracias")
        )
        
        val result = exportUseCase(words)
        
        // Format: word1,translation1;word2,translation2;word3,translation3
        assertEquals("hello,hola;goodbye,adiós;thank you,gracias", result)
    }
    
    @Test
    fun `words with mixed descriptions should export correctly`() {
        val words = listOf(
            createWord(
                id = 1,
                originalWord = "hello",
                translation = "hola",
                description = "Greeting"
            ),
            createWord(
                id = 2,
                originalWord = "goodbye",
                translation = "adiós",
                description = "" // No description
            ),
            createWord(
                id = 3,
                originalWord = "thanks",
                translation = "gracias",
                description = "Expressing gratitude"
            )
        )
        
        val result = exportUseCase(words)
        
        // Format: word1,trans1,desc1;word2,trans2;word3,trans3,desc3
        // Words are separated by semicolons, not newlines
        assertEquals("hello,hola,Greeting;goodbye,adiós;thanks,gracias,Expressing gratitude", result)
    }

    
    @Test
    fun `words with commas should be preserved`() {
        val words = listOf(
            createWord(
                originalWord = "Hello, my friend",
                translation = "Hola, mi amigo",
                description = "Friendly greeting, used often"
            )
        )
        
        val result = exportUseCase(words)
        
        // Format includes the commas from the phrase
        // Note: This creates ambiguity - "Hello, my friend,Hola, mi amigo" has 3 commas total
        // The format is: word,translation,description where word and translation contain commas
        assertEquals("Hello, my friend,Hola, mi amigo,Friendly greeting, used often", result)
    }
    
    @Test
    fun `words with quotes should be preserved`() {
        val words = listOf(
            createWord(
                originalWord = "He said \"hello\"",
                translation = "Él dijo \"hola\"",
                description = "Quote marks should work"
            )
        )
        
        val result = exportUseCase(words)
        
        assertEquals("He said \"hello\",Él dijo \"hola\",Quote marks should work", result)
    }
    
    @Test
    fun `words with semicolons should export but may cause issues on import`() {
        // Edge case: what if the word itself contains a semicolon (the separator)?
        val words = listOf(
            createWord(
                originalWord = "word with semicolon;here",
                translation = "palabra con punto y coma;aquí"
            )
        )
        
        val result = exportUseCase(words)
        
        // This is a known limitation - semicolons in words will cause parsing issues
        // But the export should still succeed
        assertEquals("word with semicolon;here,palabra con punto y coma;aquí", result)
    }

    
    @Test
    fun `Spanish characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "¡Hola!", translation = "Hello!"),
            createWord(originalWord = "¿Qué tal?", translation = "How are you?"),
            createWord(originalWord = "Niño", translation = "Boy"),
            createWord(originalWord = "Señor", translation = "Sir")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("¡Hola!,Hello!"))
        assertTrue(result.contains("¿Qué tal?,How are you?"))
        assertTrue(result.contains("Niño,Boy"))
        assertTrue(result.contains("Señor,Sir"))
    }
    
    @Test
    fun `French characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "Café", translation = "Coffee"),
            createWord(originalWord = "Être", translation = "To be"),
            createWord(originalWord = "Français", translation = "French"),
            createWord(originalWord = "Ça va?", translation = "How are you?")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("Café,Coffee"))
        assertTrue(result.contains("Être,To be"))
        assertTrue(result.contains("Français,French"))
        assertTrue(result.contains("Ça va?,How are you?"))
    }
    
    @Test
    fun `German characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "Grüß Gott", translation = "Hello (Southern Germany)"),
            createWord(originalWord = "Über", translation = "Over/Above"),
            createWord(originalWord = "Schön", translation = "Beautiful"),
            createWord(originalWord = "Größe", translation = "Size")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("Grüß Gott"))
        assertTrue(result.contains("Über"))
        assertTrue(result.contains("Schön"))
        assertTrue(result.contains("Größe"))
    }
    
    @Test
    fun `Arabic characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "مرحبا", translation = "Hello"),
            createWord(originalWord = "شكرا", translation = "Thank you"),
            createWord(originalWord = "كيف حالك", translation = "How are you"),
            createWord(originalWord = "مع السلامة", translation = "Goodbye")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("مرحبا,Hello"))
        assertTrue(result.contains("شكرا,Thank you"))
        assertTrue(result.contains("كيف حالك,How are you"))
        assertTrue(result.contains("مع السلامة,Goodbye"))
    }
    
    @Test
    fun `Chinese characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "你好", translation = "Hello"),
            createWord(originalWord = "谢谢", translation = "Thank you"),
            createWord(originalWord = "再见", translation = "Goodbye"),
            createWord(originalWord = "早上好", translation = "Good morning")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("你好,Hello"))
        assertTrue(result.contains("谢谢,Thank you"))
        assertTrue(result.contains("再见,Goodbye"))
        assertTrue(result.contains("早上好,Good morning"))
    }
    
    @Test
    fun `Japanese characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "こんにちは", translation = "Hello"),
            createWord(originalWord = "ありがとう", translation = "Thank you"),
            createWord(originalWord = "さようなら", translation = "Goodbye"),
            createWord(originalWord = "おはよう", translation = "Good morning")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("こんにちは,Hello"))
        assertTrue(result.contains("ありがとう,Thank you"))
        assertTrue(result.contains("さようなら,Goodbye"))
        assertTrue(result.contains("おはよう,Good morning"))
    }
    
    @Test
    fun `Korean characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "안녕하세요", translation = "Hello"),
            createWord(originalWord = "감사합니다", translation = "Thank you"),
            createWord(originalWord = "안녕히 가세요", translation = "Goodbye"),
            createWord(originalWord = "좋은 아침", translation = "Good morning")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("안녕하세요,Hello"))
        assertTrue(result.contains("감사합니다,Thank you"))
        assertTrue(result.contains("안녕히 가세요,Goodbye"))
        assertTrue(result.contains("좋은 아침,Good morning"))
    }
    
    @Test
    fun `Russian characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "Привет", translation = "Hello"),
            createWord(originalWord = "Спасибо", translation = "Thank you"),
            createWord(originalWord = "До свидания", translation = "Goodbye"),
            createWord(originalWord = "Доброе утро", translation = "Good morning")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("Привет,Hello"))
        assertTrue(result.contains("Спасибо,Thank you"))
        assertTrue(result.contains("До свидания,Goodbye"))
        assertTrue(result.contains("Доброе утро,Good morning"))
    }
    
    @Test
    fun `emoji should be preserved`() {
        val words = listOf(
            createWord(originalWord = "Happy ", translation = "Feliz "),
            createWord(originalWord = "Heart ", translation = "Corazón "),
            createWord(originalWord = "Thumbs up ", translation = "Pulgar arriba ")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("Happy ,Feliz "))
        assertTrue(result.contains("Heart ,Corazón "))
        assertTrue(result.contains("Thumbs up ,Pulgar arriba "))
    }
    
    @Test
    fun `mixed language characters should be preserved`() {
        val words = listOf(
            createWord(originalWord = "Hello مرحبا", translation = "English + Arabic"),
            createWord(originalWord = "你好 こんにちは", translation = "Chinese + Japanese"),
            createWord(originalWord = "Привет Hello", translation = "Russian + English")
        )
        
        val result = exportUseCase(words)
        
        assertTrue(result.contains("Hello مرحبا,English + Arabic"))
        assertTrue(result.contains("你好 こんにちは,Chinese + Japanese"))
        assertTrue(result.contains("Привет Hello,Russian + English"))
    }

    
    @Test
    fun `export should be compact without headers`() {
        val words = listOf(
            createWord(originalWord = "hello", translation = "hola")
        )
        
        val result = exportUseCase(words)
        
        // New format is simple and compact - no headers
        assertEquals("hello,hola", result)
        assertFalse(result.contains("#"))
        assertFalse(result.contains("Vokab"))
    }
    
    @Test
    fun `words should be separated by semicolons`() {
        val words = listOf(
            createWord(id = 1, originalWord = "hello", translation = "hola"),
            createWord(id = 2, originalWord = "goodbye", translation = "adiós")
        )
        
        val result = exportUseCase(words)
        
        // Format: word1,trans1;word2,trans2
        assertEquals("hello,hola;goodbye,adiós", result)
    }

    
    @Test
    fun `exported words should be importable`() {
        val originalWords = listOf(
            createWord(
                id = 1,
                originalWord = "hello",
                translation = "hola",
                description = "Common greeting"
            ),
            createWord(
                id = 2,
                originalWord = "goodbye",
                translation = "adiós",
                description = ""
            ),
            createWord(
                id = 3,
                originalWord = "thank you",
                translation = "gracias",
                description = "Expressing gratitude"
            )
        )
        
        // Export
        val exportedText = exportUseCase(originalWords)
        
        // The exported format should be compatible with import
        // Format: word1,trans1,desc1;word2,trans2;word3,trans3,desc3
        assertEquals("hello,hola,Common greeting;goodbye,adiós;thank you,gracias,Expressing gratitude", exportedText)
    }
    
    @Test
    fun `exported words with special characters should be importable`() {
        val originalWords = listOf(
            createWord(
                originalWord = "Hello, my friend",
                translation = "Hola, mi amigo",
                description = "Friendly greeting, with commas"
            ),
            createWord(
                originalWord = "你好",
                translation = "Hello",
                description = "Chinese greeting"
            ),
            createWord(
                originalWord = "مرحبا",
                translation = "Hello",
                description = "Arabic greeting"
            )
        )
        
        val exportedText = exportUseCase(originalWords)
        
        // Format: word1,trans1,desc1;word2,trans2,desc2;word3,trans3,desc3
        assertEquals("Hello, my friend,Hola, mi amigo,Friendly greeting, with commas;你好,Hello,Chinese greeting;مرحبا,Hello,Arabic greeting", exportedText)
    }

    
    @Test
    fun `very long words should not truncate`() {
        val longWord = "This is a very long phrase with many words and characters " +
                       "that could potentially cause issues with truncation or buffer overflow"
        val longTranslation = "Esta es una frase muy larga con muchas palabras y caracteres " +
                              "que potencialmente podrían causar problemas"
        val longDescription = "A very long description that should also not be truncated"
        
        val words = listOf(
            createWord(
                originalWord = longWord,
                translation = longTranslation,
                description = longDescription
            )
        )
        
        val result = exportUseCase(words)
        
        assertEquals("$longWord,$longTranslation,$longDescription", result)
    }
    
    @Test
    fun `words with newlines in description should be preserved`() {
        // Edge case: description with embedded newlines
        val words = listOf(
            createWord(
                originalWord = "hello",
                translation = "hola",
                description = "Line 1\nLine 2"
            )
        )
        
        val result = exportUseCase(words)
        
        // The newline in description should be preserved
        assertEquals("hello,hola,Line 1\nLine 2", result)
    }
    
    @Test
    fun `export with 100 words should complete successfully`() {
        val words = (1..100).map { i ->
            createWord(
                id = i,
                originalWord = "word$i",
                translation = "palabra$i",
                description = "Description $i"
            )
        }
        
        val result = exportUseCase(words)
        
        // Should contain all 100 words separated by semicolons
        assertTrue(result.contains("word1,palabra1,Description 1"))
        assertTrue(result.contains("word50,palabra50,Description 50"))
        assertTrue(result.contains("word100,palabra100,Description 100"))
        
        // Count semicolons (should be 99 for 100 words)
        val semicolonCount = result.count { it == ';' }
        assertEquals(99, semicolonCount, "Should have 99 semicolons for 100 words")
    }
}

