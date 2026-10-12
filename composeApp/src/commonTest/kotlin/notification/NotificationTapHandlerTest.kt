package notification

import domain.notifications.usecase.FakeNotificationEngagementRepository
import domain.notifications.usecase.ReportNotificationOpenedUseCase
import fakes.FakeSubscriptionAccessRepository
import fakes.FakeSubscriptionManager
import app.cash.turbine.test
import domain.subscription.usecase.RefreshSubscriptionStateUseCase
import kotlinx.coroutines.test.runTest
import notification.payload.SubscriptionUpdatedHandler
import presentation.navigation.NotificationDestination
import presentation.navigation.NotificationNavigator
import domain.word.model.ReviewSource
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationTapHandlerTest {

    private val engagementRepository = FakeNotificationEngagementRepository()
    private val navigator = NotificationNavigator()

    private fun kotlinx.coroutines.test.TestScope.handler() = NotificationTapHandler(
        reportNotificationOpened = ReportNotificationOpenedUseCase(engagementRepository),
        navigator = navigator,
        scope = this,
    )

    @Test
    fun `billing issue tap opens subscription screen and reports the open`() = runTest {
        navigator.destinations.test {
            handler().onNotificationTapped(
                mapOf(NotificationTapHandler.TYPE_KEY to PushTypes.BILLING_ISSUE, "notification_log_id" to "42")
            )

            assertEquals(NotificationDestination.Subscription, awaitItem())
            testScheduler.advanceUntilIdle()
            assertEquals(listOf(42L), engagementRepository.reported)
        }
    }

    @Test
    fun `review reminder tap opens study tab and requests a due review and reports the open`() = runTest {
        navigator.reviewRequests.test {
            handler().onNotificationTapped(
                mapOf(NotificationTapHandler.TYPE_KEY to PushTypes.REVIEW_REMINDER, "notification_log_id" to "5")
            )

            awaitItem()
            testScheduler.advanceUntilIdle()
            assertEquals(listOf(5L), engagementRepository.reported)
        }
        navigator.destinations.test {
            assertEquals(NotificationDestination.Study, awaitItem())
        }
    }

    @Test
    fun `support reply tap opens the support chat`() = runTest {
        navigator.destinations.test {
            handler().onNotificationTapped(mapOf(NotificationTapHandler.TYPE_KEY to PushTypes.SUPPORT_REPLY))

            assertEquals(NotificationDestination.Support, awaitItem())
        }
    }

    @Test
    fun `tap of other types only reports the open`() = runTest {
        navigator.destinations.test {
            handler().onNotificationTapped(
                mapOf(NotificationTapHandler.TYPE_KEY to "streak_reminder", "notification_log_id" to "7")
            )
            testScheduler.advanceUntilIdle()

            expectNoEvents()
            assertEquals(listOf(7L), engagementRepository.reported)
        }
    }

    @Test
    fun `due cards tap follows its review deep link`() = runTest {
        navigator.reviewRequests.test {
            handler().onNotificationTapped(
                mapOf(NotificationTapHandler.TYPE_KEY to "due_cards", NotificationTapHandler.DEEP_LINK_KEY to "vokab://review/due")
            )

            awaitItem()
        }
        navigator.destinations.test {
            assertEquals(NotificationDestination.Study, awaitItem())
        }
    }

    @Test
    fun `insight and stats deep links open the insights tab`() = runTest {
        navigator.destinations.test {
            handler().onNotificationTapped(mapOf(NotificationTapHandler.DEEP_LINK_KEY to "vokab://insights"))
            assertEquals(NotificationDestination.Insights, awaitItem())

            handler().onNotificationTapped(mapOf(NotificationTapHandler.DEEP_LINK_KEY to "vokab://stats/weekly"))
            assertEquals(NotificationDestination.Insights, awaitItem())
        }
    }

    @Test
    fun `add words deep link opens the add words sheet on the study tab`() = runTest {
        navigator.addWordsRequests.test {
            handler().onNotificationTapped(mapOf(NotificationTapHandler.DEEP_LINK_KEY to "vokab://words/add"))

            awaitItem()
        }
        navigator.destinations.test {
            assertEquals(NotificationDestination.Study, awaitItem())
        }
    }

    @Test
    fun `word deep link starts a review of that word`() = runTest {
        navigator.reviewRequests.test {
            handler().onNotificationTapped(mapOf(NotificationTapHandler.DEEP_LINK_KEY to "vokab://word/42"))

            assertEquals(ReviewSource.ByWords(listOf(42L)), awaitItem())
        }
        navigator.destinations.test {
            assertEquals(NotificationDestination.Study, awaitItem())
        }
    }

    @Test
    fun `comeback rescue link reviews exactly the planned words with the outros and reports the open`() = runTest {
        navigator.reviewRequests.test {
            handler().onNotificationTapped(
                mapOf(
                    NotificationTapHandler.TYPE_KEY to "comeback_rescue",
                    NotificationTapHandler.DEEP_LINK_KEY to "vokab://review/rescue?ids=5,1,20,3&rid=42",
                    "rid" to "42",
                    "outro_win" to "Gewitter: cracked.",
                    "outro_soft" to "Soon.",
                    "notification_log_id" to "9",
                )
            )

            assertEquals(ReviewSource.ComebackRescue(42L, listOf(5L, 1L, 20L, 3L), "Gewitter: cracked.", "Soon."), awaitItem())
            testScheduler.advanceUntilIdle()
            assertEquals(listOf(9L), engagementRepository.reported)
        }
        navigator.destinations.test {
            assertEquals(NotificationDestination.Study, awaitItem())
        }
    }

    @Test
    fun `comeback rescue link drops malformed ids and caps the session`() = runTest {
        val ids = (listOf("a", "", "7", "-1") + (100..130).map { it.toString() }).joinToString(",")
        navigator.reviewRequests.test {
            handler().onNotificationTapped(
                mapOf(NotificationTapHandler.DEEP_LINK_KEY to "vokab://review/rescue?ids=$ids&rid=42")
            )

            val source = awaitItem() as ReviewSource.ComebackRescue
            assertEquals(listOf(7L) + (100L..118L), source.wordIds)
            assertEquals(null, source.outroWin)
        }
    }

    @Test
    fun `comeback rescue link without usable words or rescue id falls back to the due review`() = runTest {
        navigator.reviewRequests.test {
            handler().onNotificationTapped(mapOf(NotificationTapHandler.DEEP_LINK_KEY to "vokab://review/rescue?ids=x&rid=42"))
            assertEquals(ReviewSource.DueCards, awaitItem())

            handler().onNotificationTapped(mapOf(NotificationTapHandler.DEEP_LINK_KEY to "vokab://review/rescue?ids=1,2"))
            assertEquals(ReviewSource.DueCards, awaitItem())
        }
    }

    @Test
    fun `word deep link with a malformed id opens the words tab`() = runTest {
        navigator.destinations.test {
            handler().onNotificationTapped(mapOf(NotificationTapHandler.DEEP_LINK_KEY to "vokab://word/abc"))

            assertEquals(NotificationDestination.Words, awaitItem())
        }
    }

    @Test
    fun `unknown deep links are ignored`() = runTest {
        navigator.destinations.test {
            handler().onNotificationTapped(mapOf(NotificationTapHandler.DEEP_LINK_KEY to "https://evil.example/review"))
            testScheduler.advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `subscription updated push force refreshes subscription state`() = runTest {
        val accessRepository = FakeSubscriptionAccessRepository()
        val subscriptionManager = FakeSubscriptionManager()
        val handler = SubscriptionUpdatedHandler(RefreshSubscriptionStateUseCase(accessRepository, subscriptionManager))

        handler.handle(emptyMap())

        assertEquals(PushTypes.SUBSCRIPTION_UPDATED, handler.type)
        assertEquals(1, accessRepository.forcedRefreshCount)
        assertEquals(1, subscriptionManager.refreshCount)
    }
}
