@file:OptIn(InternalResourceApi::class)

package presentation.util

import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.notification_message_few_words
import lexicon.resources.generated.resources.notification_message_large_session
import lexicon.resources.generated.resources.notification_message_many_words
import lexicon.resources.generated.resources.notification_message_one_word
import lexicon.resources.generated.resources.notification_message_several_words
import lexicon.resources.generated.resources.notification_message_very_large
import lexicon.resources.generated.resources.notification_title_daily
import lexicon.resources.generated.resources.notification_title_few_words
import lexicon.resources.generated.resources.notification_title_many_words
import lexicon.resources.generated.resources.notification_title_one_word

/**
 * Helper object to select appropriate notification string resources
 */
object NotificationStringHelper {
    
    data class NotificationStrings(
        val titleRes: StringResource,
        val titleParams: List<Any> = emptyList(),
        val messageRes: StringResource,
        val messageParams: List<Any> = emptyList()
    )
    
    fun getNotificationResources(dueCards: Int): NotificationStrings {
        return when {
            dueCards == 1 -> NotificationStrings(
                titleRes = Res.string.notification_title_one_word,
                messageRes = Res.string.notification_message_one_word
            )
            dueCards < 5 -> NotificationStrings(
                titleRes = Res.string.notification_title_few_words,
                messageRes = Res.string.notification_message_few_words,
                messageParams = listOf(dueCards, dueCards)
            )
            dueCards < 10 -> NotificationStrings(
                titleRes = Res.string.notification_title_few_words,
                messageRes = Res.string.notification_message_several_words,
                messageParams = listOf(dueCards)
            )
            dueCards < 20 -> NotificationStrings(
                titleRes = Res.string.notification_title_few_words,
                messageRes = Res.string.notification_message_many_words,
                messageParams = listOf(dueCards)
            )
            dueCards < 30 -> NotificationStrings(
                titleRes = Res.string.notification_title_many_words,
                titleParams = listOf(dueCards),
                messageRes = Res.string.notification_message_large_session,
                messageParams = listOf(dueCards)
            )
            dueCards < 40 -> NotificationStrings(
                titleRes = Res.string.notification_title_many_words,
                titleParams = listOf(dueCards),
                messageRes = Res.string.notification_message_large_session,
                messageParams = listOf(dueCards)
            )
            else -> NotificationStrings(
                titleRes = Res.string.notification_title_daily,
                messageRes = Res.string.notification_message_very_large,
                messageParams = listOf(dueCards)
            )
        }
    }
}

