package com.example.test

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.test.domain.repository.ForwardRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmsForwarderApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var forwardRepository: ForwardRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "sms_forwarder_channel"
        const val CHANNEL_NAME = "SMS Forwarder"
        const val CHANNEL_DESCRIPTION = "Notifications for SMS forwarding status"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeDefaultRules()
    }
    
    private fun initializeDefaultRules() {
        applicationScope.launch {
            try {
                forwardRepository.initializeDefaultRules()
                android.util.Log.d("SmsForwarderApp", "✅ Default rules initialized")
            } catch (e: Exception) {
                android.util.Log.e("SmsForwarderApp", "❌ Failed to initialize default rules: ${e.message}", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 