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
