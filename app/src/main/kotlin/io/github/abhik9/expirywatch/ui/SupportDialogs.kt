package io.github.abhik9.expirywatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.R
import io.github.abhik9.expirywatch.core.donations.DonationOption
import io.github.abhik9.expirywatch.core.donations.DonationState

/**
 * Greets the user the first time the app is opened: everything is free, and a donation is welcome
 * but entirely optional. Builds that can't take donations only make the promise.
 */
@Composable
internal fun WelcomeDialog(
    donationState: DonationState,
    onDonate: (DonationOption) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.VolunteerActivism, contentDescription = null) },
        title = { Text(stringResource(R.string.welcome_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.welcome_free))
                if (donationState != DonationState.Unavailable) {
                    Text(stringResource(R.string.welcome_coffee))
                    when (donationState) {
                        is DonationState.Ready -> donationState.options.forEach { option ->
                            DonationButton(option = option, onClick = { onDonate(option) })
                        }

                        DonationState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                        DonationState.Unavailable -> Unit
                    }
                    Text(
                        text = stringResource(R.string.welcome_settings_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.welcome_start)) }
        },
    )
}

@Composable
private fun DonationButton(option: DonationOption, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
        ) {
            Icon(Icons.Outlined.LocalCafe, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Column {
                Text(option.title)
                Text(option.detail, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Shown when a donation went through. */
@Composable
internal fun ThanksDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.LocalCafe, contentDescription = null) },
        title = { Text(stringResource(R.string.thanks_title)) },
        text = { Text(stringResource(R.string.thanks_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.thanks_close)) }
        },
    )
}
