package feature.addwords

import domain.word.add.model.WordOrigin
import fakes.creditBalance
import feature.addwords.model.canStart
import kotlin.test.Test
import kotlin.test.assertFalse
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
}
