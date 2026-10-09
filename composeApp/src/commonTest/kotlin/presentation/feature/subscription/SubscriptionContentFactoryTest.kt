package presentation.feature.subscription

import domain.auth.model.UserFeatureAccess
import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionEntitlement
import domain.subscription.model.SubscriptionPackage
import domain.subscription.model.SubscriptionProduct
import domain.subscription.model.SubscriptionStore
import feature.subscription.SubscriptionContentFactory
import feature.subscription.model.MembershipStatus
import feature.subscription.model.SubscriptionContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionContentFactoryTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_800_000_000_000L
    private val factory = SubscriptionContentFactory(
        nowMillis = { now },
        formatDate = { "D+${(it - now) / day}" },
    )

    private val annual = SubscriptionPackage(
        identifier = "annual",
        packagePeriod = PackagePeriod.ANNUAL,
        product = SubscriptionProduct("Annual", "", "$29.99", 29_990_000, "vokab_annual"),
        trialPeriodDays = 7,
        hasFreeTrial = true,
    )
    private val monthly = SubscriptionPackage(
        identifier = "monthly",
        packagePeriod = PackagePeriod.MONTHLY,
        product = SubscriptionProduct("Monthly", "", "$4.99", 4_990_000, "vokab_monthly"),
    )

    private fun store(
        expiresInDays: Long? = 30,
        willRenew: Boolean = true,
        isInTrial: Boolean = false,
        productId: String = "vokab_annual",
        unsubscribedAtMillis: Long? = null,
        boughtOn: SubscriptionStore = SubscriptionStore.PLAY_STORE,
        deviceStore: SubscriptionStore? = SubscriptionStore.PLAY_STORE,
    ) = SubscriptionCustomerInfo(
        activeEntitlements = mapOf(
            "premium" to SubscriptionEntitlement(
                identifier = "premium",
                isActive = true,
                expirationDateMillis = expiresInDays?.let { now + it * day },
                productIdentifier = productId,
                willRenew = willRenew,
                isInTrial = isInTrial,
                unsubscribeDetectedAtMillis = unsubscribedAtMillis,
                store = boughtOn,
            )
        ),
        deviceStore = deviceStore,
    )

    private fun member(customerInfo: SubscriptionCustomerInfo?, access: UserFeatureAccess = UserFeatureAccess()) =
        assertIs<SubscriptionContent.Member>(
            factory.content(listOf(annual, monthly), customerInfo, access)
        ).membership

    @Test
    fun `free user gets the paywall with trial dates`() {
        val paywall = assertIs<SubscriptionContent.Paywall>(
            factory.content(listOf(monthly, annual), customerInfo = null, access = UserFeatureAccess())
        )
        val annualPlan = paywall.plans.first()
        assertEquals("annual", annualPlan.identifier)
        assertEquals("D+6", annualPlan.trialSchedule?.lastFreeDay)
        assertEquals("D+7", annualPlan.trialSchedule?.chargeDate)
        assertNull(paywall.plans.last().trialSchedule)
    }

    @Test
    fun `renewing store plan shows next renewal and price`() {
        val membership = member(store())
        assertEquals(MembershipStatus.Renewing(renewsOn = "D+30"), membership.status)
        assertEquals("$29.99", membership.price)
        assertEquals(PackagePeriod.ANNUAL, membership.period)
        assertTrue(membership.isManageable)
        assertNull(membership.managedOn)
    }

    @Test
    fun `App Store subscription seen on Android is not manageable here and says where it is`() {
        val membership = member(store(boughtOn = SubscriptionStore.APP_STORE))

        assertEquals(MembershipStatus.Renewing(renewsOn = "D+30"), membership.status)
        assertFalse(membership.isManageable)
        assertEquals(SubscriptionStore.APP_STORE, membership.managedOn)
    }

    @Test
    fun `Google Play subscription seen on iPhone is not manageable here and says where it is`() {
        val membership = member(store(deviceStore = SubscriptionStore.APP_STORE))

        assertFalse(membership.isManageable)
        assertEquals(SubscriptionStore.PLAY_STORE, membership.managedOn)
    }

    @Test
    fun `promotional entitlement has no store to manage it in`() {
        val membership = member(store(boughtOn = SubscriptionStore.OTHER))

        assertFalse(membership.isManageable)
        assertNull(membership.managedOn)
    }

    @Test
    fun `store subscription on web is not manageable without a device store`() {
        val membership = member(store(deviceStore = null))

        assertFalse(membership.isManageable)
        assertEquals(SubscriptionStore.PLAY_STORE, membership.managedOn)
    }

    @Test
    fun `canceled but unexpired plan is Canceled with days left and remaining share`() {
        val membership = member(store(expiresInDays = 73, willRenew = false))
        val status = assertIs<MembershipStatus.Canceled>(membership.status)
        assertEquals("D+73", status.accessEndsOn)
        assertEquals(73, status.daysLeft)
        assertEquals(false, status.wasTrial)
        assertEquals(0.2f, status.remainingFraction)
    }

    @Test
    fun `canceled trial is Canceled not Trial`() {
        val status = assertIs<MembershipStatus.Canceled>(
            member(store(expiresInDays = 3, willRenew = false, isInTrial = true)).status
        )
        assertTrue(status.wasTrial)
        assertNull(status.remainingFraction)
    }

    @Test
    fun `active trial shows end date and days left`() {
        assertEquals(
            MembershipStatus.Trial(endsOn = "D+5", daysLeft = 5),
            member(store(expiresInDays = 5, isInTrial = true)).status,
        )
    }

    @Test
    fun `less than a day left counts as zero days`() {
        val info = store(willRenew = false).let { info ->
            val e = info.activeEntitlements.getValue("premium").copy(expirationDateMillis = now + day / 2)
            SubscriptionCustomerInfo(mapOf("premium" to e))
        }
        assertEquals(0, assertIs<MembershipStatus.Canceled>(member(info).status).daysLeft)
    }

    @Test
    fun `store purchase without expiry is Lifetime`() {
        assertEquals(MembershipStatus.Lifetime, member(store(expiresInDays = null)).status)
    }

    @Test
    fun `backend grant is Granted and not manageable`() {
        val membership = member(
            customerInfo = null,
            access = UserFeatureAccess(hasPremiumAccess = true, source = "GRANT", expiresAt = "2028-01-01T00:00:00Z"),
        )
        assertEquals(MembershipStatus.Granted(until = "D+350"), membership.status)
        assertEquals(false, membership.isManageable)
        assertNull(membership.price)
    }

    @Test
    fun `purchase from another platform uses the backend period and is not manageable here`() {
        val membership = member(
            customerInfo = null,
            access = UserFeatureAccess(
                hasPremiumAccess = true,
                source = "STORE",
                expiresAt = "2027-02-15T08:00:00Z",
                willRenew = false,
            ),
        )
        assertIs<MembershipStatus.Canceled>(membership.status)
        assertEquals(false, membership.isManageable)
    }

    @Test
    fun `store entitlement on this device wins over backend period`() {
        val membership = member(
            customerInfo = store(expiresInDays = 10, willRenew = false),
            access = UserFeatureAccess(hasPremiumAccess = true, source = "STORE", expiresAt = "2030-01-01T00:00:00Z", willRenew = true),
        )
        assertEquals(10, assertIs<MembershipStatus.Canceled>(membership.status).daysLeft)
    }

    @Test
    fun `unmatched product falls back to period guessed from product id`() {
        val membership = member(store(productId = "premium_monthly_v2"))
        assertEquals(PackagePeriod.MONTHLY, membership.period)
        assertNull(membership.price)
    }

    // region billing issue & pause

    private fun iso(epochMillis: Long) = kotlin.time.Instant.fromEpochMilliseconds(epochMillis).toString()

    @Test
    fun `payment problem from the store wins over canceled`() {
        val info = store(expiresInDays = 3, willRenew = false).let { info ->
            val e = info.activeEntitlements.getValue("premium").copy(billingIssueDetectedAtMillis = now - day)
            SubscriptionCustomerInfo(mapOf("premium" to e))
        }
        assertEquals(MembershipStatus.BillingIssue(accessEndsOn = "D+3", daysLeft = 3), member(info).status)
    }

    @Test
    fun `payment problem reported by the backend for a purchase made elsewhere`() {
        val membership = member(
            customerInfo = null,
            access = UserFeatureAccess(
                hasPremiumAccess = true,
                source = "STORE",
                expiresAt = iso(now + 4 * day),
                willRenew = false,
                hasBillingIssue = true,
            ),
        )
        assertEquals(MembershipStatus.BillingIssue(accessEndsOn = "D+4", daysLeft = 4), membership.status)
    }

    @Test
    fun `scheduled pause shows when premium pauses and when it resumes`() {
        val membership = member(
            customerInfo = store(expiresInDays = 10, willRenew = false),
            access = UserFeatureAccess(
                hasPremiumAccess = true,
                source = "STORE",
                expiresAt = iso(now + 10 * day),
                pauseResumesAt = iso(now + 40 * day),
            ),
        )
        assertEquals(MembershipStatus.PauseScheduled(pausesOn = "D+10", resumesOn = "D+40"), membership.status)
    }

    @Test
    fun `canceling after scheduling a pause shows Canceled not the pause`() {
        val membership = member(
            customerInfo = store(expiresInDays = 10, willRenew = false, unsubscribedAtMillis = now - day),
            access = UserFeatureAccess(
                hasPremiumAccess = true,
                source = "STORE",
                expiresAt = iso(now + 10 * day),
                pauseResumesAt = iso(now + 40 * day),
            ),
        )
        assertIs<MembershipStatus.Canceled>(membership.status)
    }

    @Test
    fun `paused subscription shows the paused state instead of the paywall`() {
        val content = factory.content(
            packages = listOf(annual, monthly),
            customerInfo = null,
            access = UserFeatureAccess(hasPremiumAccess = false, pauseResumesAt = iso(now + 20 * day)),
        )
        assertEquals(SubscriptionContent.Paused(resumesOn = "D+20", daysLeft = 20), content)
    }

    @Test
    fun `pause that already ended is ignored`() {
        val content = factory.content(
            packages = listOf(annual, monthly),
            customerInfo = null,
            access = UserFeatureAccess(hasPremiumAccess = false, pauseResumesAt = iso(now - day)),
        )
        assertIs<SubscriptionContent.Paywall>(content)
    }

    // endregion
}
