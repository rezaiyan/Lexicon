package presentation.feature.subscription

import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionPackage
import domain.subscription.model.SubscriptionProduct
import feature.subscription.PlanPricing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanPricingTest {

    private fun pkg(
        id: String,
        period: PackagePeriod,
        price: String,
        micros: Long?,
        trialDays: Int? = null,
        productId: String = id,
    ) = SubscriptionPackage(
        identifier = id,
        packagePeriod = period,
        product = SubscriptionProduct(
            title = id,
            description = "",
            priceFormatted = price,
            priceAmountMicros = micros,
            productIdentifier = productId,
        ),
        trialPeriodDays = trialDays,
        hasFreeTrial = trialDays != null,
    )

    private val monthly = pkg("monthly", PackagePeriod.MONTHLY, "$4.99", 4_990_000)
    private val annual = pkg("annual", PackagePeriod.ANNUAL, "$29.99", 29_990_000, trialDays = 7)

    @Test
    fun `options orders annual before monthly`() {
        val options = PlanPricing.options(listOf(monthly, annual))
        assertEquals(listOf("annual", "monthly"), options.map { it.identifier })
    }

    @Test
    fun `options drops packages that are neither monthly nor annual`() {
        val lifetime = pkg("lifetime", PackagePeriod.LIFETIME, "$99.99", 99_990_000)
        val options = PlanPricing.options(listOf(monthly, lifetime))
        assertEquals(listOf("monthly"), options.map { it.identifier })
    }

    @Test
    fun `annual option shows per month price and savings against monthly`() {
        val annualOption = PlanPricing.options(listOf(monthly, annual)).first()
        assertEquals("$2.50", annualOption.perMonthPrice)
        // 12 × 4.99 = 59.88 → 29.99 is 49.9% less, shown floored
        assertEquals(49, annualOption.savingsPercent)
        assertEquals(7, annualOption.trialDays)
    }

    @Test
    fun `monthly option has no per month price or savings`() {
        val monthlyOption = PlanPricing.options(listOf(monthly, annual)).last()
        assertNull(monthlyOption.perMonthPrice)
        assertNull(monthlyOption.savingsPercent)
    }

    @Test
    fun `savings hidden without a monthly plan to compare against`() {
        val annualOption = PlanPricing.options(listOf(annual)).single()
        assertNull(annualOption.savingsPercent)
        assertEquals("$2.50", annualOption.perMonthPrice)
    }

    @Test
    fun `savings hidden when store price amounts are unknown`() {
        val options = PlanPricing.options(
            listOf(monthly.copy(product = monthly.product.copy(priceAmountMicros = null)), annual)
        )
        assertNull(options.first().savingsPercent)
    }

    @Test
    fun `savings below five percent are not advertised`() {
        val pricey = pkg("annual", PackagePeriod.ANNUAL, "$58.00", 58_000_000)
        assertNull(PlanPricing.options(listOf(monthly, pricey)).first().savingsPercent)
    }

    @Test
    fun `defaultSelection prefers annual then first option`() {
        assertEquals("annual", PlanPricing.defaultSelection(PlanPricing.options(listOf(monthly, annual))))
        assertEquals("monthly", PlanPricing.defaultSelection(PlanPricing.options(listOf(monthly))))
        assertNull(PlanPricing.defaultSelection(emptyList()))
    }

    @Test
    fun `formatLike keeps comma decimal separator and trailing currency`() {
        assertEquals("2,50 €", PlanPricing.formatLike("29,99 €", 2_500_000))
    }

    @Test
    fun `formatLike treats three trailing digits as grouping so no decimals`() {
        assertEquals("¥250", PlanPricing.formatLike("¥3,000", 250_000_000))
    }

    @Test
    fun `formatLike handles grouped amount with decimals`() {
        assertEquals("R$ 104,17", PlanPricing.formatLike("R$ 1.250,00", 104_166_666))
    }

    @Test
    fun `formatLike returns null when template has no digits`() {
        assertNull(PlanPricing.formatLike("Free", 1_000_000))
    }

    @Test
    fun `matchingOption finds the plan behind an entitlement product id`() {
        val options = PlanPricing.options(listOf(monthly, annual))
        assertEquals("annual", PlanPricing.matchingOption(options, "annual")?.identifier)
        assertNull(PlanPricing.matchingOption(options, "unknown_product"))
    }

    @Test
    fun `matchingOption matches android base plan ids by subscription id when unambiguous`() {
        val options = PlanPricing.options(
            listOf(pkg("annual", PackagePeriod.ANNUAL, "$29.99", 29_990_000, productId = "vokab_annual:yearly"))
        )
        assertEquals("annual", PlanPricing.matchingOption(options, "vokab_annual")?.identifier)
    }
}
