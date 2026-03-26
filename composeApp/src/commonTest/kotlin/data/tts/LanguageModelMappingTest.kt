package data.tts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for LanguageModelMapping — verifies that every declared language code
 * resolves to a non-null PiperModelInfo with a valid archive URL, that unsupported
 * codes return null, and that the helper flags reflect the map contents exactly.
 */
class LanguageModelMappingTest {

    // -------------------------------------------------------------------------
    // getModelInfo — known languages
    // -------------------------------------------------------------------------

    @Test
    fun `getModelInfo returns non-null for English`() {
        assertNotNull(LanguageModelMapping.getModelInfo("en"))
    }

    @Test
    fun `getModelInfo returns non-null for German`() {
        assertNotNull(LanguageModelMapping.getModelInfo("de"))
    }

    @Test
    fun `getModelInfo returns non-null for Spanish`() {
        assertNotNull(LanguageModelMapping.getModelInfo("es"))
    }

    @Test
    fun `getModelInfo returns non-null for French`() {
        assertNotNull(LanguageModelMapping.getModelInfo("fr"))
    }

    @Test
    fun `getModelInfo returns non-null for Italian`() {
        assertNotNull(LanguageModelMapping.getModelInfo("it"))
    }

    @Test
    fun `getModelInfo returns non-null for Portuguese`() {
        assertNotNull(LanguageModelMapping.getModelInfo("pt"))
    }

    @Test
    fun `getModelInfo returns non-null for Russian`() {
        assertNotNull(LanguageModelMapping.getModelInfo("ru"))
    }

    @Test
    fun `getModelInfo returns non-null for Chinese`() {
        assertNotNull(LanguageModelMapping.getModelInfo("zh"))
    }

    @Test
    fun `getModelInfo returns non-null for Turkish`() {
        assertNotNull(LanguageModelMapping.getModelInfo("tr"))
    }

    @Test
    fun `getModelInfo returns non-null for Dutch`() {
        assertNotNull(LanguageModelMapping.getModelInfo("nl"))
    }

    @Test
    fun `getModelInfo returns non-null for Arabic`() {
        assertNotNull(LanguageModelMapping.getModelInfo("ar"))
    }

    @Test
    fun `getModelInfo returns non-null for Hindi`() {
        assertNotNull(LanguageModelMapping.getModelInfo("hi"))
    }

    @Test
    fun `getModelInfo returns non-null for Farsi`() {
        assertNotNull(LanguageModelMapping.getModelInfo("fa"))
    }

    // -------------------------------------------------------------------------
    // getModelInfo — archive URL format
    // -------------------------------------------------------------------------

    @Test
    fun `getModelInfo archive URLs start with expected base URL`() {
        val base = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
        LanguageModelMapping.supportedLanguages.forEach { code ->
            val info = LanguageModelMapping.getModelInfo(code)
            assertNotNull(info, "Expected non-null model info for code: $code")
            assertTrue(
                info.archiveUrl.startsWith(base),
                "Archive URL for '$code' should start with base URL"
            )
        }
    }

    @Test
    fun `getModelInfo extractedDirName is non-blank for all supported languages`() {
        LanguageModelMapping.supportedLanguages.forEach { code ->
            val info = LanguageModelMapping.getModelInfo(code)
            assertNotNull(info, "Expected non-null model info for code: $code")
            assertTrue(
                info.extractedDirName.isNotBlank(),
                "extractedDirName must not be blank for code: $code"
            )
        }
    }

    // -------------------------------------------------------------------------
    // getModelInfo — unknown language
    // -------------------------------------------------------------------------

    @Test
    fun `getModelInfo returns null for unknown language code`() {
        assertNull(LanguageModelMapping.getModelInfo("xx"))
    }

    @Test
    fun `getModelInfo returns null for empty string`() {
        assertNull(LanguageModelMapping.getModelInfo(""))
    }

    @Test
    fun `getModelInfo returns null for uppercase variant of supported code`() {
        // Language codes are stored lowercase; uppercase should not match
        assertNull(LanguageModelMapping.getModelInfo("EN"))
    }

    // -------------------------------------------------------------------------
    // isSupported
    // -------------------------------------------------------------------------

    @Test
    fun `isSupported returns true for English`() {
        assertTrue(LanguageModelMapping.isSupported("en"))
    }

    @Test
    fun `isSupported returns false for unknown code`() {
        assertFalse(LanguageModelMapping.isSupported("xx"))
    }

    @Test
    fun `isSupported returns false for empty string`() {
        assertFalse(LanguageModelMapping.isSupported(""))
    }

    @Test
    fun `isSupported is consistent with getModelInfo non-null result`() {
        LanguageModelMapping.supportedLanguages.forEach { code ->
            assertTrue(
                LanguageModelMapping.isSupported(code),
                "isSupported should return true for all codes in supportedLanguages (failed for: $code)"
            )
        }
    }

    // -------------------------------------------------------------------------
    // supportedLanguages set
    // -------------------------------------------------------------------------

    @Test
    fun `supportedLanguages contains exactly 13 entries`() {
        assertEquals(13, LanguageModelMapping.supportedLanguages.size)
    }

    @Test
    fun `supportedLanguages contains all expected language codes`() {
        val expected = setOf("en", "de", "es", "fr", "it", "pt", "ru", "zh", "tr", "nl", "ar", "hi", "fa")
        assertEquals(expected, LanguageModelMapping.supportedLanguages)
    }

    @Test
    fun `supportedLanguages does not contain unknown code`() {
        assertFalse(LanguageModelMapping.supportedLanguages.contains("xx"))
    }
}
