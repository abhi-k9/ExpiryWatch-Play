package io.github.abhik9.expirywatch

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.common.di.AppDispatcher
import io.github.abhik9.expirywatch.core.common.di.Dispatcher
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.donations.DonationOption
import io.github.abhik9.expirywatch.core.donations.DonationState
import io.github.abhik9.expirywatch.core.donations.Donations
import io.github.abhik9.expirywatch.core.donations.WelcomeMessage
import io.github.abhik9.expirywatch.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val settings: UserSettings) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsRepository: UserSettingsRepository,
    private val welcomeMessage: WelcomeMessage,
    private val donations: Donations,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = settingsRepository.settings
        .map<UserSettings, MainActivityUiState>(MainActivityUiState::Success)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MainActivityUiState.Loading)

    private val _showWelcome = MutableStateFlow<Boolean?>(null)

    /** Whether to show the welcome message, which only the first start does; `null` until known. */
    val showWelcome: StateFlow<Boolean?> = _showWelcome.asStateFlow()

    val donationState: StateFlow<DonationState> = donations.state
    val thanks: Flow<Unit> = donations.thanks

    init {
        viewModelScope.launch(ioDispatcher) { _showWelcome.value = !welcomeMessage.wasShown() }
    }

    fun onWelcomeDismissed() {
        _showWelcome.value = false
        viewModelScope.launch(ioDispatcher) { welcomeMessage.markShown() }
    }

    fun donate(activity: Activity, option: DonationOption) = donations.donate(activity, option)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
