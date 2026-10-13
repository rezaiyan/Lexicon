package feature.addwords

import domain.word.add.model.WordOrigin
import fakes.creditBalance
import domain.credits.model.CreditAction
import feature.addwords.model.canStart
import feature.addwords.model.costFor
import feature.addwords.model.creditAction
import feature.addwords.model.guidedSurcharge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreditCostTest {

    @Test
    fun `free sources can always start even with no credits`() {
        val broke = creditBalance(allowanceRemaining = 0, bonusBalance = 0)

        assertTrue(broke.canStart(WordOrigin.Manual))
        assertTrue(broke.canStart(WordOrigin.File))
    }

    @Test
    fun `a paid source needs at least its cost`() {
        val two = creditBalance(allowanceRemaining = 0, bonusBalance = 2)

        assertFalse(two.canStart(WordOrigin.Photo)) // costs 3
        assertTrue(two.canStart(WordOrigin.AiSuggestion)) // costs 1
    }

    @Test
    fun `with an unknown balance the server decides`() {
        assertTrue(null.canStart(WordOrigin.Photo))
    }

    private val priced = creditBalance(
        allowanceRemaining = 0,
        bonusBalance = 3,
        costs = mapOf(
            CreditAction.PHOTO_EXTRACTION to 2,
            CreditAction.PHOTO_EXTRACTION_GUIDED to 4,
            CreditAction.AI_SUGGESTION to 2,
        ),
    )

    @Test
    fun `guided requests spend the guided action`() {
        assertEquals(CreditAction.PHOTO_EXTRACTION_GUIDED, WordOrigin.Photo.creditAction(guided = true))
        assertEquals(CreditAction.AI_SUGGESTION, WordOrigin.AiSuggestion.creditAction())
        assertNull(WordOrigin.Manual.creditAction(guided = true))
    }

    @Test
    fun `the guided price can be out of reach while the plain one is not`() {
        assertTrue(priced.canStart(WordOrigin.Photo))
        assertFalse(priced.canStart(WordOrigin.Photo, guided = true))
    }

    @Test
    fun `costFor shows the price for how the request is set up`() {
        assertEquals(2, priced.costFor(WordOrigin.Photo, guided = false))
        assertEquals(4, priced.costFor(WordOrigin.Photo, guided = true))
        assertEquals(0, priced.costFor(WordOrigin.File, guided = true))
        assertNull(null.costFor(WordOrigin.Photo, guided = true))
    }

    @Test
    fun `an unpriced guided action costs what the server would charge - the plain price`() {
        assertEquals(2, priced.costFor(WordOrigin.AiSuggestion, guided = true))
        assertEquals(0, priced.guidedSurcharge(WordOrigin.AiSuggestion))
    }

    @Test
    fun `guidedSurcharge is the difference - or 0 when unknown`() {
        assertEquals(2, priced.guidedSurcharge(WordOrigin.Photo))
        assertEquals(0, null.guidedSurcharge(WordOrigin.Photo))
    }
}
