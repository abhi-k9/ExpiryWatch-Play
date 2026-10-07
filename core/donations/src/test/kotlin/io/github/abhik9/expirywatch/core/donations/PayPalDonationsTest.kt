package io.github.abhik9.expirywatch.core.donations

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.testing.diagnostics.RecordingEventLog
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PayPalDonationsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val log = RecordingEventLog()

    @Test
    fun opensTheLinkOfThisBuild() {
        val donations = PayPalDonations("https://example.com/donate", context, log)
        val option = assertIs<DonationState.Ready>(donations.state.value).options.single()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        donations.donate(activity, option)

        val opened = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, opened.action)
        assertEquals("https://example.com/donate", opened.dataString)
        // The link itself isn't recorded.
        assertEquals(listOf("donations: opened PayPal"), log.messages)
    }

    @Test
    fun aBuildWithoutALinkOffersNothing() {
        val donations = PayPalDonations("", context, log)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        assertEquals(DonationState.Unavailable, donations.state.value)
        donations.donate(activity, DonationOption("paypal", "Buy me a coffee", "Any amount"))
        assertNull(shadowOf(activity).nextStartedActivity)
    }
}
