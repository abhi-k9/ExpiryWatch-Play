package io.github.abhik9.expirywatch.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/** [TipStore] on Google Play's Billing Library. */
@Singleton
internal class PlayTipStore @Inject constructor(
    @ApplicationContext context: Context,
    private val log: EventLog,
) : TipStore {
    private val updates = MutableSharedFlow<List<TipPurchase>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val purchaseUpdates: Flow<List<TipPurchase>> = updates

    private val listener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            updates.tryEmit(purchases.map { it.toTipPurchase() })
        } else {
            log.record { "tips: purchase screen closed with ${result.describe()}" }
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    @Volatile
    private var details: Map<String, ProductDetails> = emptyMap()

    override suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            log.record { "tips: couldn't connect to Google Play: ${result.describe()}" }
                        }
                        if (continuation.isActive) {
                            continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
        }
    }

    override suspend fun products(ids: List<String>): List<TipProduct> {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ids.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()
        val found = suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { result, productDetails ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    log.record { "tips: couldn't get the tips: ${result.describe()}" }
                }
                continuation.resume(productDetails.productDetailsList)
            }
        }
        details = found.associateBy { it.productId }
        return found.mapNotNull { product ->
            val offer = product.oneTimePurchaseOfferDetails ?: return@mapNotNull null
            TipProduct(product.productId, product.name, offer.formattedPrice, offer.priceAmountMicros)
        }
    }

    override suspend fun unconsumedPurchases(): List<TipPurchase> {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        return suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    log.record { "tips: couldn't check for unfinished tips: ${result.describe()}" }
                }
                continuation.resume(purchases.map { it.toTipPurchase() })
            }
        }
    }

    override suspend fun consume(purchaseToken: String): Boolean {
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
        return suspendCancellableCoroutine { continuation ->
            client.consumeAsync(params) { result, _ ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    log.record { "tips: couldn't complete a tip: ${result.describe()}" }
                }
                continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
        }
    }

    override fun launch(activity: Activity, productId: String): Boolean {
        val product = details[productId] ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            log.record { "tips: couldn't open the purchase screen: ${result.describe()}" }
        }
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun Purchase.toTipPurchase() = TipPurchase(
        token = purchaseToken,
        isPaid = purchaseState == Purchase.PurchaseState.PURCHASED,
    )

    // The debug message can contain details of the account, so only the code is recorded.
    private fun BillingResult.describe(): String = "response code $responseCode"
}
