package io.github.abhik9.expirywatch.core.donations

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Ways for people to support ExpiryWatch with a donation. Every feature is free: this is only for
 * those who want to say thanks. Each build flavor brings its own way of donating.
 */
interface Donations {
    val state: StateFlow<DonationState>

    /** Emits when a donation went through, to thank the user. Not every way of donating can tell. */
    val thanks: Flow<Unit>

    /** Prepares the options, for example by asking Google Play for prices. Safe to call any time. */
    fun refresh()

    /** Starts donating [option], which must be one of [DonationState.Ready.options]. */
    fun donate(activity: Activity, option: DonationOption)
}

sealed interface DonationState {
    /** This build or device can't take donations, so none should be offered. */
    data object Unavailable : DonationState

    data object Loading : DonationState

    data class Ready(val options: List<DonationOption>) : DonationState
}

/**
 * Something to donate, such as a tip of a set amount.
 *
 * @property detail its price, or how much it can be.
 */
data class DonationOption(
    val id: String,
    val title: String,
    val detail: String,
)
