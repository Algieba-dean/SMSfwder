package com.example.test.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        const val SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received, action: ${intent.action}")

        if (intent.action != SMS_RECEIVED_ACTION) {
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.w(TAG, "No SMS messages found in intent")
                return
            }

            for (smsMessage in messages) {
                processSmsMessage(context, smsMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS: ${e.message}", e)
        }
    }

    private fun processSmsMessage(context: Context, smsMessage: SmsMessage) {
        val sender = smsMessage.originatingAddress ?: "Unknown"
        val content = smsMessage.messageBody ?: ""
        val timestamp = smsMessage.timestampMillis

        Log.d(TAG, "Processing SMS from: $sender, content length: ${content.length}")

        // Trigger SMS processing via WorkManager
        // The worker will handle database operations and email forwarding
        val workRequest = OneTimeWorkRequestBuilder<SmsForwardWorker>()
            .setInputData(
                workDataOf(
                    "messageId" to System.currentTimeMillis(), // Use timestamp as unique ID
                    "sender" to sender,
                    "content" to content,
                    "timestamp" to timestamp
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "SMS processing work enqueued for message from: $sender")
    }
} 