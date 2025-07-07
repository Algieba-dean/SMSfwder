package com.example.test.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "sms_forwarder_prefs", 
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_FORWARD_SUCCESS_NOTIFICATION = "forward_success_notification"
        private const val KEY_FORWARD_FAILURE_NOTIFICATION = "forward_failure_notification"
        private const val KEY_SOUND_ALERT = "sound_alert"
    }

    var forwardSuccessNotificationEnabled: Boolean
        get() = preferences.getBoolean(KEY_FORWARD_SUCCESS_NOTIFICATION, true)
        set(value) = preferences.edit().putBoolean(KEY_FORWARD_SUCCESS_NOTIFICATION, value).apply()

    var forwardFailureNotificationEnabled: Boolean
        get() = preferences.getBoolean(KEY_FORWARD_FAILURE_NOTIFICATION, true)
        set(value) = preferences.edit().putBoolean(KEY_FORWARD_FAILURE_NOTIFICATION, value).apply()

    var soundAlertEnabled: Boolean
        get() = preferences.getBoolean(KEY_SOUND_ALERT, false)
        set(value) = preferences.edit().putBoolean(KEY_SOUND_ALERT, value).apply()
} 