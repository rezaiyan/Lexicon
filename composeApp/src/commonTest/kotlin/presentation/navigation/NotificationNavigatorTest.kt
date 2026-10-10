package presentation.navigation

import app.cash.turbine.test
import domain.word.model.ReviewSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationNavigatorTest {

    @Test
    fun `openReview switches to Study and requests that source`() = runTest {
        val navigator = NotificationNavigator()
        navigator.openReview(ReviewSource.ByWords(listOf(1L, 2L)))

        navigator.destinations.test { assertEquals(NotificationDestination.Study, awaitItem()) }
        navigator.reviewRequests.test { assertEquals(ReviewSource.ByWords(listOf(1L, 2L)), awaitItem()) }
    }

    @Test
    fun `openDueReview requests due cards`() = runTest {
        val navigator = NotificationNavigator()
        navigator.openDueReview()
        navigator.reviewRequests.test { assertEquals(ReviewSource.DueCards, awaitItem()) }
    }

    @Test
    fun `openWordRush switches to Study and requests a game`() = runTest {
        val navigator = NotificationNavigator()
        navigator.openWordRush()
        navigator.destinations.test { assertEquals(NotificationDestination.Study, awaitItem()) }
        navigator.wordRushRequests.test { assertEquals(Unit, awaitItem()) }
    }
}
