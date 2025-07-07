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
import com.example.test.utils.PermissionHelper

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        const val SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"
        const val SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "============ SMS BROADCAST RECEIVED ============")
        Log.d(TAG, "Action: ${intent.action}")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString()}")
        Log.d(TAG, "Package: ${context.packageName}")
        
        // 检查权限
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "❌ SMS permissions not granted, ignoring SMS")
            return
        }
        Log.d(TAG, "✅ SMS permissions verified")

        // 检查action
        val action = intent.action
        if (action != SMS_RECEIVED_ACTION && 
            action != SMS_DELIVER_ACTION &&
            action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "❌ Not an SMS action: $action")
            return
        }
        Log.d(TAG, "✅ Valid SMS action: $action")

        try {
            // 优先处理测试SMS
            val testSender = intent.getStringExtra("sender")
            val testMessage = intent.getStringExtra("message")
            val testTimestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
            
            if (!testSender.isNullOrEmpty() && !testMessage.isNullOrEmpty()) {
                Log.d(TAG, "🧪 Processing test SMS from: $testSender")
                enqueueSmsProcessing(context, testSender, testMessage, testTimestamp)
                return
            }
            
            // 处理真实SMS
            val messages = extractSmsMessages(intent)
            if (messages.isEmpty()) {
                Log.w(TAG, "❌ No SMS messages found in intent")
                return
            }

            Log.d(TAG, "✅ Found ${messages.size} SMS message(s)")
            
            // 处理每条SMS消息
            for (smsMessage in messages) {
                processSmsMessage(context, smsMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing SMS: ${e.message}", e)
        }
        
        Log.d(TAG, "============ SMS PROCESSING COMPLETED ============")
    }

    private fun extractSmsMessages(intent: Intent): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            // 方法1：使用标准的Telephony方法
            val telephonyMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (!telephonyMessages.isNullOrEmpty()) {
                messages.addAll(telephonyMessages)
                Log.d(TAG, "Extracted ${telephonyMessages.size} messages using Telephony API")
                return messages
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract messages using Telephony API: ${e.message}")
        }

        try {
            // 方法2：手动解析PDU（兼容性方法）
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as? Array<*>
                val format = bundle.getString("format", "3gpp")
                
                if (pdus != null) {
                    Log.d(TAG, "Found ${pdus.size} PDUs to process")
                    for (pdu in pdus) {
                        try {
                            val smsMessage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                SmsMessage.createFromPdu(pdu as ByteArray, format)
                            } else {
                                @Suppress("DEPRECATION")
                                SmsMessage.createFromPdu(pdu as ByteArray)
                            }
                            if (smsMessage != null) {
                                messages.add(smsMessage)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to create SmsMessage from PDU: ${e.message}")
                        }
                    }
                    Log.d(TAG, "Extracted ${messages.size} messages using manual PDU parsing")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract messages using manual PDU parsing: ${e.message}")
        }

        return messages
    }

    private fun processSmsMessage(context: Context, smsMessage: SmsMessage) {
        val sender = smsMessage.originatingAddress ?: "Unknown"
        val content = smsMessage.messageBody ?: ""
        val timestamp = smsMessage.timestampMillis

        Log.d(TAG, "Processing SMS from: $sender, content length: ${content.length}")
        Log.d(TAG, "SMS content preview: ${content.take(50)}...")

        enqueueSmsProcessing(context, sender, content, timestamp)
    }

    private fun enqueueSmsProcessing(context: Context, sender: String, content: String, timestamp: Long) {
        Log.d(TAG, "📤 Enqueueing SMS processing:")
        Log.d(TAG, "   📞 Sender: $sender")
        Log.d(TAG, "   📝 Content length: ${content.length}")
        Log.d(TAG, "   🕐 Timestamp: $timestamp")
        
        // 使用WorkManager异步处理SMS转发
        val workRequest = OneTimeWorkRequestBuilder<SmsForwardWorker>()
            .setInputData(
                workDataOf(
                    "messageId" to System.currentTimeMillis(), // 使用当前时间作为唯一ID
                    "sender" to sender,
                    "content" to content,
                    "timestamp" to timestamp
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "✅ SMS processing work enqueued successfully for: $sender")
        Log.d(TAG, "🆔 Work ID: ${workRequest.id}")
    }
} 