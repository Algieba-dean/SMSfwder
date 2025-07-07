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
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS broadcast received, action: ${intent.action}")
        
        // 检查权限
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "SMS permissions not granted, ignoring SMS")
            return
        }

        // 检查action
        val action = intent.action
        if (action != SMS_RECEIVED_ACTION && action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "Not an SMS received action: $action")
            return
        }

        try {
            // 获取SMS消息
            val messages = extractSmsMessages(intent)
            if (messages.isEmpty()) {
                // 检查是否是测试SMS
                val sender = intent.getStringExtra("sender")
                val message = intent.getStringExtra("message")
                val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
                
                if (!sender.isNullOrEmpty() && !message.isNullOrEmpty()) {
                    Log.d(TAG, "Processing test SMS directly")
                    enqueueSmsProcessing(context, sender, message, timestamp)
                    return
                }
                
                Log.w(TAG, "No SMS messages found in intent")
                return
            }

            Log.d(TAG, "Found ${messages.size} SMS message(s)")
            
            // 处理每条SMS消息
            for (smsMessage in messages) {
                processSmsMessage(context, smsMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS: ${e.message}", e)
        }
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

        // 方法3：测试模式下的简化处理
        if (messages.isEmpty()) {
            try {
                val sender = intent.getStringExtra("sender")
                val message = intent.getStringExtra("message")
                
                if (!sender.isNullOrEmpty() && !message.isNullOrEmpty()) {
                    Log.d(TAG, "Found test/debug SMS data: sender=$sender")
                    // 返回空列表，让调用者在onReceive中处理测试SMS
                    return listOf()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check for test SMS: ${e.message}")
            }
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
        Log.d(TAG, "SMS processing work enqueued for message from: $sender")
    }
} 