package domain.auth.usecase

import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetFeatureAccessUseCaseTest {

    private val repository = FakeAuthRepository()

    @Test
    fun `emits feature access response from repository`() = runTest {
        val expected = FeatureAccessResponse(
            featureFlags = FeatureFlags(pushNotificationsEnabled = true),
            userAccess = UserFeatureAccess(hasPremiumAccess = true)
        )
        repository.featureAccessFlow = flowOf(expected)
        val repoWithAccess = FakeAuthRepository().apply { featureAccessFlow = flowOf(expected) }
        val uc = GetFeatureAccessUseCase(repoWithAccess)

        val result = uc().first()

        assertEquals(expected, result)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        val expected = FeatureAccessResponse(
            featureFlags = FeatureFlags(pushNotificationsEnabled = false),
            userAccess = UserFeatureAccess(hasPremiumAccess = false)
        )
        val repo = FakeAuthRepository().apply { featureAccessFlow = flowOf(expected) }
        val uc = GetFeatureAccessUseCase(repo)

        val result = uc(Unit).first()

        assertEquals(expected, result)
    }

    @Test
    fun `premium access is false by default`() = runTest {
        val defaultAccess = FeatureAccessResponse(
            featureFlags = FeatureFlags(),
            userAccess = UserFeatureAccess()
        )
        val repo = FakeAuthRepository().apply { featureAccessFlow = flowOf(defaultAccess) }
        val uc = GetFeatureAccessUseCase(repo)

        val result = uc().first()

        assertEquals(false, result.userAccess.hasPremiumAccess)
        assertEquals(true, result.featureFlags.pushNotificationsEnabled)
    }
}
