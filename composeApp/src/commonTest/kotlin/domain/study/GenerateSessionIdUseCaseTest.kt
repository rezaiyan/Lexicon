package domain.study

import domain.study.usecase.GenerateSessionIdUseCase
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateSessionIdUseCaseTest {

    @Test
    fun `session id has timestamp-suffix format`() {
        val useCase = GenerateSessionIdUseCase()
        val id = useCase()
        assertTrue(id.contains("-"), "Expected hyphen separator in '$id'")
        val parts = id.split("-")
        assertEquals(2, parts.size, "Expected exactly two parts in '$id'")
        assertTrue(parts[0].toLongOrNull() != null, "First part should be numeric timestamp")
        assertEquals(6, parts[1].length, "Suffix should be 6 digits")
    }

    @Test
    fun `suffix is zero-padded to six digits`() {
        val useCase = GenerateSessionIdUseCase(Random(42))
        val id = useCase()
        val suffix = id.substringAfter("-")
        assertEquals(6, suffix.length)
        assertTrue(suffix.all { it.isDigit() })
    }

    @Test
    fun `successive calls produce different ids`() {
        val useCase = GenerateSessionIdUseCase()
        val id1 = useCase()
        val id2 = useCase()
        // Timestamps should differ (calls separated in time) or suffixes differ
        assertTrue(id1 != id2 || id1.substringBefore("-") != id2.substringBefore("-"))
    }
}
