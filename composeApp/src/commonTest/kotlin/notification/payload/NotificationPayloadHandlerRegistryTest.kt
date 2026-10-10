package notification.payload

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationPayloadHandlerRegistryTest {

    @Test
    fun `payloadWithBody keeps the data body of a silent push`() {
        val payload = NotificationPayloadHandlerRegistry.payloadWithBody(
            body = "",
            data = mapOf("type" to "daily_insight", "body" to "You reviewed 40 words this week"),
        )

        assertEquals("You reviewed 40 words this week", payload["body"])
    }

    @Test
    fun `payloadWithBody uses the displayed text of a visible push`() {
        val payload = NotificationPayloadHandlerRegistry.payloadWithBody(
            body = "Shown text",
            data = mapOf("type" to "daily_insight"),
        )

        assertEquals("Shown text", payload["body"])
    }
}
