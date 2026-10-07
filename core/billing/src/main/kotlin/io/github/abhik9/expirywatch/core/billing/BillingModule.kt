package io.github.abhik9.expirywatch.core.billing

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.donations.Donations

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BillingModule {
    @Binds
    abstract fun bindsDonations(donations: PlayBillingDonations): Donations

    @Binds
    abstract fun bindsTipStore(store: PlayTipStore): TipStore
}
