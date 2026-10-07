package io.github.abhik9.expirywatch.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.BuildConfig
import io.github.abhik9.expirywatch.core.donations.DonationUrl
import io.github.abhik9.expirywatch.core.donations.Donations
import io.github.abhik9.expirywatch.core.donations.PayPalDonations

/** The foss flavor takes donations through a PayPal link, set when it's built. */
@Module
@InstallIn(SingletonComponent::class)
abstract class FossDonationsModule {
    @Binds
    abstract fun bindsDonations(donations: PayPalDonations): Donations

    companion object {
        @Provides
        @DonationUrl
        fun providesDonationUrl(): String = BuildConfig.DONATION_URL
    }
}
