package io.github.abhik9.expirywatch.core.donations

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** The PayPal link that [PayPalDonations] opens; empty when the build has none. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DonationUrl

/**
 * Donations through a PayPal link, opened in the browser or the PayPal app. PayPal doesn't report
 * back, so there's no thank-you from the app.
 */
class PayPalDonations @Inject constructor(
    @DonationUrl private val url: String,
    @ApplicationContext context: Context,
    private val log: EventLog,
) : Donations {
    private val option = DonationOption(
        id = OPTION_ID,
        title = context.getString(R.string.core_donations_paypal_title),
        detail = context.getString(R.string.core_donations_paypal_detail),
    )

    override val state: StateFlow<DonationState> = MutableStateFlow(
        if (url.isBlank()) DonationState.Unavailable else DonationState.Ready(listOf(option)),
    ).asStateFlow()

    override val thanks: Flow<Unit> = emptyFlow()

    override fun refresh() = Unit

    override fun donate(activity: Activity, option: DonationOption) {
        if (url.isBlank()) return
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            log.record { "donations: opened PayPal" }
        } catch (e: ActivityNotFoundException) {
            log.record { "donations: nothing can open the PayPal link" }
        }
    }

    private companion object {
        const val OPTION_ID = "paypal"
    }
}
