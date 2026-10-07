package io.github.abhik9.expirywatch.core.billing

import android.app.Activity
import io.github.abhik9.expirywatch.core.donations.DonationOption
import io.github.abhik9.expirywatch.core.donations.DonationState
import io.github.abhik9.expirywatch.core.testing.diagnostics.RecordingEventLog
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayBillingDonationsTest {
    private val store = FakeTipStore()
    private val log = RecordingEventLog()

    private fun TestScope.donations() = PlayBillingDonations(store, backgroundScope, log)

    private val coffee = TipProduct("tip_small", "Coffee", "€2.99", 2_990_000)
    private val lunch = TipProduct("tip_medium", "Lunch", "€7.99", 7_990_000)

    @Test
    fun offersTheTipsFromCheapest() = runTest(UnconfinedTestDispatcher()) {
        store.products = listOf(lunch, coffee, TipProduct("other", "Other", "€1", 1))
        val donations = donations()

        donations.refresh()

        assertEquals(
            DonationState.Ready(
                listOf(
                    DonationOption("tip_small", "Coffee", "€2.99"),
                    DonationOption("tip_medium", "Lunch", "€7.99"),
                ),
            ),
            donations.state.value,
        )
    }

    @Test
    fun offersNothingWithoutGooglePlay() = runTest(UnconfinedTestDispatcher()) {
        store.isAvailable = false
        val donations = donations()

        donations.refresh()

        assertEquals(DonationState.Unavailable, donations.state.value)
    }

    @Test
    fun aPaidTipIsConsumedAndThanked() = runTest(UnconfinedTestDispatcher()) {
        store.products = listOf(coffee)
        val donations = donations()
        donations.refresh()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        donations.donate(activity, DonationOption("tip_small", "Coffee", "€2.99"))
        store.updates.emit(listOf(TipPurchase("token-1", isPaid = true)))

        assertEquals(listOf("tip_small"), store.launched)
        assertEquals(listOf("token-1"), store.consumed)
        donations.thanks.first()
    }

    @Test
    fun aPendingTipIsCompletedOnceItIsPaid() = runTest(UnconfinedTestDispatcher()) {
        store.products = listOf(coffee)
        val donations = donations()

        store.updates.emit(listOf(TipPurchase("token-1", isPaid = false)))
        assertEquals(emptyList(), store.consumed)

        // Paid in the meantime, and found the next time the app connects.
        store.unconsumed = listOf(TipPurchase("token-1", isPaid = true))
        donations.refresh()
        assertEquals(listOf("token-1"), store.consumed)
        donations.thanks.first()
    }

    @Test
    fun aTipReportedTwiceIsConsumedOnce() = runTest(UnconfinedTestDispatcher()) {
        store.products = listOf(coffee)
        store.unconsumed = listOf(TipPurchase("token-1", isPaid = true))
        val donations = donations()

        store.updates.emit(listOf(TipPurchase("token-1", isPaid = true)))
        donations.refresh()

        assertEquals(listOf("token-1"), store.consumed)
    }

    private class FakeTipStore : TipStore {
        var isAvailable = true
        var products = emptyList<TipProduct>()
        var unconsumed = emptyList<TipPurchase>()
        val consumed = mutableListOf<String>()
        val launched = mutableListOf<String>()
        val updates = MutableSharedFlow<List<TipPurchase>>()

        override val purchaseUpdates = updates

        override suspend fun connect() = isAvailable

        override suspend fun products(ids: List<String>) = products.filter { it.id in ids }

        override suspend fun unconsumedPurchases() = unconsumed

        override suspend fun consume(purchaseToken: String): Boolean {
            consumed += purchaseToken
            return true
        }

        override fun launch(activity: Activity, productId: String): Boolean {
            launched += productId
            return true
        }
    }
}
