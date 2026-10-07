package io.github.abhik9.expirywatch.core.donations

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers whether the welcome message, shown when the app is first opened, was seen. */
@Singleton
class WelcomeMessage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val preferences by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    /** Reads from disk, so call it off the main thread. */
    fun wasShown(): Boolean = preferences.getBoolean(KEY_SHOWN, false)

    fun markShown() = preferences.edit { putBoolean(KEY_SHOWN, true) }

    private companion object {
        const val PREFERENCES_NAME = "welcome"
        const val KEY_SHOWN = "shown"
    }
}
