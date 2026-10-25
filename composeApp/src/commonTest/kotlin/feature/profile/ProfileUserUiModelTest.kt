package feature.profile

import feature.profile.model.ProfileUserUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileUserUiModelTest {

    @Test
    fun `shownName prefers the alias the user picked`() {
        assertEquals("JD", ProfileUserUiModel(name = "Jane Doe", email = "jane@example.com", displayAlias = "JD").shownName)
    }

    @Test
    fun `shownName falls back to the name when the alias is blank`() {
        assertEquals("Jane", ProfileUserUiModel(name = "Jane", email = "jane@example.com", displayAlias = " ").shownName)
    }

    @Test
    fun `shownName is null for an account Apple shared no name for`() {
        assertNull(ProfileUserUiModel(name = "", email = "x7k2@privaterelay.appleid.com").shownName)
    }

    @Test
    fun `Apple relay and stand-in addresses count as hidden`() {
        assertTrue(ProfileUserUiModel(name = "", email = "x7k2@privaterelay.appleid.com").isEmailHiddenByApple)
        assertTrue(ProfileUserUiModel(name = "", email = "q9w8@private.icloud.com").isEmailHiddenByApple)
        assertTrue(ProfileUserUiModel(name = "", email = "apple_001_abc@apple.hidden").isEmailHiddenByApple)
    }

    @Test
    fun `a real address is not hidden, iCloud included`() {
        assertFalse(ProfileUserUiModel(name = "", email = "jane@icloud.com").isEmailHiddenByApple)
        assertFalse(ProfileUserUiModel(name = "", email = "jane@gmail.com").isEmailHiddenByApple)
    }
}
