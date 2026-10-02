package domain.auth.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserFeatureAccessTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `premiumSource when not premium is NONE regardless of source`() {
        assertEquals(PremiumSource.NONE, UserFeatureAccess(hasPremiumAccess = false, source = "STORE").premiumSource)
    }

    @Test
    fun `premiumSource parses known values`() {
        assertEquals(PremiumSource.STORE, UserFeatureAccess(hasPremiumAccess = true, source = "STORE").premiumSource)
        assertEquals(PremiumSource.GRANT, UserFeatureAccess(hasPremiumAccess = true, source = "GRANT").premiumSource)
    }

    @Test
    fun `premiumSource when premium with unknown or missing source falls back to GRANT`() {
        assertEquals(PremiumSource.GRANT, UserFeatureAccess(hasPremiumAccess = true, source = "PROMO").premiumSource)
        assertEquals(PremiumSource.GRANT, UserFeatureAccess(hasPremiumAccess = true).premiumSource)
    }

    @Test
    fun `expiresAtMillis parses ISO instants and ignores garbage`() {
        assertEquals(1_900_000_000_000L, UserFeatureAccess(expiresAt = "2030-03-17T17:46:40Z").expiresAtMillis)
        assertEquals(1_900_000_000_123L, UserFeatureAccess(expiresAt = "2030-03-17T17:46:40.123Z").expiresAtMillis)
        assertNull(UserFeatureAccess(expiresAt = "not-a-date").expiresAtMillis)
        assertNull(UserFeatureAccess().expiresAtMillis)
    }

    @Test
    fun `decodes response from an older server without the new fields`() {
        val access = json.decodeFromString<UserFeatureAccess>("""{"hasPremiumAccess":true}""")

        assertEquals(PremiumSource.GRANT, access.premiumSource)
        assertNull(access.expiresAt)
    }

    @Test
    fun `decodes full response from the current server`() {
        val access = json.decodeFromString<UserFeatureAccess>(
            """{"hasPremiumAccess":true,"source":"STORE","expiresAt":"2030-03-17T17:46:40Z",""" +
                """"willRenew":true,"isTrial":false}"""
        )

        assertEquals(PremiumSource.STORE, access.premiumSource)
        assertEquals(1_900_000_000_000L, access.expiresAtMillis)
        assertEquals(true, access.willRenew)
    }
}
