package data.tts

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeechSegmentsTest {

    @Test
    fun `split when text has no separators returns text trimmed`() {
        assertEquals(listOf("apple"), SpeechSegments.split("  apple "))
    }

    @Test
    fun `split when text has slash returns parts so slash is not read aloud`() {
        assertEquals(listOf("he", "she"), SpeechSegments.split("he/she"))
        assertEquals(listOf("der Hund", "die Hunde"), SpeechSegments.split("der Hund / die Hunde"))
    }

    @Test
    fun `split when text has commas returns each part`() {
        assertEquals(listOf("big", "large", "huge"), SpeechSegments.split("big, large,huge"))
    }

    @Test
    fun `split when text mixes separators returns every part`() {
        assertEquals(listOf("a", "b", "c", "d"), SpeechSegments.split("a, b / c; d"))
    }

    @Test
    fun `split when text uses full width separators returns each part`() {
        assertEquals(listOf("我", "你"), SpeechSegments.split("我，你"))
    }

    @Test
    fun `split when separators leave empty parts drops them`() {
        assertEquals(listOf("a", "b"), SpeechSegments.split(",a,, /b/"))
    }

    @Test
    fun `split when text is only separators returns empty list`() {
        assertEquals(emptyList(), SpeechSegments.split(" / , "))
    }
}
