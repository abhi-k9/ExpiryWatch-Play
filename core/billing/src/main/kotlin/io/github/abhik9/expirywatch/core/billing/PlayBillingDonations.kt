package io.github.abhik9.expirywatch.core.billing

import android.app.Activity
import io.github.abhik9.expirywatch.core.common.di.ApplicationScope
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.donations.DonationOption
import io.github.abhik9.expirywatch.core.donations.DonationState
import io.github.abhik9.expirywatch.core.donations.Donations
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tips of a few set amounts through Google Play, which its policy requires for payments in apps it
 * distributes. The tips are one-time products set up in the Play Console, with the ids in
 * [TIP_PRODUCT_IDS]; their names and prices come from there. A paid tip is consumed, so the same
 * tip can be given again, and Google Play refunds purchases that aren't within three days.
 */
@Singleton
internal class PlayBillingDonations @Inject constructor(
    private val store: TipStore,
    @ApplicationScope private val scope: CoroutineScope,
    private val log: EventLog,
) : Donations {
    private val _state = MutableStateFlow<DonationState>(DonationState.Loading)
    override val state: StateFlow<DonationState> = _state.asStateFlow()

    private val _thanks = Channel<Unit>(Channel.BUFFERED)
    override val thanks: Flow<Unit> = _thanks.receiveAsFlow()

    private var refreshing: Job? = null

    // A purchase can be reported by the purchase screen and found unconsumed at the same time.
    private val completing = Mutex()
    private val completed = mutableSetOf<String>()

    init {
        scope.launch { store.purchaseUpdates.collect { complete(it) } }
    }

    override fun refresh() {
        if (refreshing?.isActive == true) return
        refreshing = scope.launch {
            if (_state.value !is DonationState.Ready) _state.value = DonationState.Loading
            if (!store.connect()) {
                _state.value = DonationState.Unavailable
                return@launch
            }
            val tips = store.products(TIP_PRODUCT_IDS).sortedBy { it.priceMicros }
            log.record { "tips: ${tips.size} of ${TIP_PRODUCT_IDS.size} available" }
            _state.value = if (tips.isEmpty()) {
                DonationState.Unavailable
            } else {
                DonationState.Ready(tips.map { DonationOption(id = it.id, title = it.name, detail = it.price) })
            }
            // Tips paid while the app was closed, or whose pending payment went through since.
            complete(store.unconsumedPurchases())
        }
    }

    override fun donate(activity: Activity, option: DonationOption) {
        log.record { "tips: opening the purchase screen for ${option.id}" }
        store.launch(activity, option.id)
    }

    private suspend fun complete(purchases: List<TipPurchase>) = completing.withLock {
        purchases.filter { it.isPaid && it.token !in completed }.forEach { purchase ->
            if (store.consume(purchase.token)) {
                completed += purchase.token
                log.record { "tips: tip received" }
                _thanks.send(Unit)
            }
        }
    }

    companion object {
        val TIP_PRODUCT_IDS = listOf("tip_small", "tip_medium", "tip_large")
    }
}
