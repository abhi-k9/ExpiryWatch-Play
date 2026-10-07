package io.github.abhik9.expirywatch.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/** The parts of Google Play's billing that tips need, so the tip logic can be tested without it. */
internal interface TipStore {
    /** Purchases that just finished in the purchase screen, successfully or pending payment. */
    val purchaseUpdates: Flow<List<TipPurchase>>

    /** Connects to Google Play if needed. Returns false if billing isn't available. */
    suspend fun connect(): Boolean

    /** The tips with these product ids that Google Play offers in the user's country. */
    suspend fun products(ids: List<String>): List<TipProduct>

    /** Tips bought but not yet consumed, for example because the app closed before it could. */
    suspend fun unconsumedPurchases(): List<TipPurchase>

    /** Consumes a paid tip, so it can be bought again. Returns false if that failed. */
    suspend fun consume(purchaseToken: String): Boolean

    /** Opens Google Play's purchase screen for a product from the last [products] call. */
    fun launch(activity: Activity, productId: String): Boolean
}

internal data class TipProduct(val id: String, val name: String, val price: String, val priceMicros: Long)

/** @property isPaid false while the payment is still pending, for example cash at a shop. */
internal data class TipPurchase(val token: String, val isPaid: Boolean)
