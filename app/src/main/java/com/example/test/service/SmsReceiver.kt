package com.example.test.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.test.domain.model.ExecutionStrategy
import com.example.test.utils.BackgroundReliabilityManager
import com.example.test.utils.CompatibilityChecker
import com.example.test.utils.ForegroundServiceManager
import com.example.test.utils.PermissionHelper
import com.example.test.utils.SimCardManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 暂时移除@AndroidEntryPoint以解决Hilt生成类问题
class SmsReceiver : BroadcastReceiver() {

    // 移除@Inject注解，改为手动初始化
    private lateinit var backgroundReliabilityManager: BackgroundReliabilityManager
    // ForegroundServiceManager是object单例，不需要声明实例变量

    companion object {
        private const val TAG = "SmsReceiver"
        // 定义所有支持的SMS相关action
        private val SUPPORTED_SMS_ACTIONS = setOf(
            "android.provider.Telephony.SMS_RECEIVED",
            "android.provider.Telephony.SMS_DELIVER", 
            "android.intent.action.DATA_SMS_RECEIVED",
            "android.provider.Telephony.SMS_CB_RECEIVED",
            "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED",
            "android.provider.Telephony.WAP_PUSH_RECEIVED",
            "android.provider.Telephony.WAP_PUSH_DELIVER",
            "android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED",
            "android.provider.Telephony.SMS_REJECTED",
            // 兼容旧版本
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "============ SMS BROADCAST RECEIVED ============")
        Log.d(TAG, "Action: ${intent.action}")
        Log.d(TAG, "🚀 FORWARD ALL SMS MODE - NO FILTERING")
        
        // 手动初始化依赖（暂时绕过Hilt问题）
        try {
            val preferencesManager = com.example.test.data.preferences.PreferencesManager(context)
            val permissionHelper = PermissionHelper
            val chineseOEMEnhancer = com.example.test.utils.ChineseOEMEnhancer(context)
            val compatibilityChecker = CompatibilityChecker(context, preferencesManager, chineseOEMEnhancer)
            backgroundReliabilityManager = BackgroundReliabilityManager(context, preferencesManager, permissionHelper, compatibilityChecker)
            // ForegroundServiceManager是object单例，不需要手动初始化
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize dependencies: ${e.message}", e)
            return
        }
        
        // 检查权限
        if (!PermissionHelper.hasSmsPermissions(context)) {
            Log.w(TAG, "❌ SMS permissions not granted, ignoring SMS")
            return
        }
        Log.d(TAG, "✅ SMS permissions verified")

        // 检查action
        val action = intent.action
        if (!SUPPORTED_SMS_ACTIONS.contains(action)) {
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
            val messages = extractSmsMessages(context, intent)
            if (messages.isEmpty()) {
                Log.w(TAG, "❌ No SMS messages found in intent")
                return
            }

            Log.d(TAG, "✅ Found ${messages.size} SMS message(s) - FORWARDING ALL")
            
            // 处理每条SMS消息
            for (smsMessage in messages) {
                processSmsMessage(context, smsMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing SMS: ${e.message}", e)
        }
        
        Log.d(TAG, "============ SMS PROCESSING COMPLETED ============")
    }

    private fun extractSmsMessages(context: Context, intent: Intent): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val action = intent.action
        
        try {
            when (action) {
                "android.provider.Telephony.SMS_RECEIVED",
                "android.provider.Telephony.SMS_DELIVER",
                "android.intent.action.DATA_SMS_RECEIVED",
                Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                    // 标准SMS和数据SMS处理
                    val telephonyMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    if (!telephonyMessages.isNullOrEmpty()) {
                        messages.addAll(telephonyMessages)
                        Log.d(TAG, "✅ Extracted ${telephonyMessages.size} SMS messages using Telephony API")
                        return messages
                    }
                    
                    // 备用方法：手动提取PDU
                    val bundle = intent.extras
                    if (bundle != null) {
                        val pdus = bundle.get("pdus") as? Array<*>
                        val format = bundle.getString("format")
                        
                        if (pdus != null) {
                            Log.d(TAG, "📱 Processing ${pdus.size} PDUs with format: $format")
                            for (pdu in pdus) {
                                if (pdu is ByteArray) {
                                    val smsMessage = if (format != null) {
                                        SmsMessage.createFromPdu(pdu, format)
                                    } else {
                                        SmsMessage.createFromPdu(pdu)
                                    }
                                    if (smsMessage != null) {
                                        messages.add(smsMessage)
                                    }
                                }
                            }
                        }
                    }
                }
                
                "android.provider.Telephony.SMS_CB_RECEIVED",
                "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED" -> {
                    // Cell Broadcast消息处理
                    Log.d(TAG, "📡 Processing Cell Broadcast message")
                    processCellBroadcastMessage(context, intent)
                    return messages // Cell broadcast特殊处理，不走常规SMS流程
                }
                
                "android.provider.Telephony.WAP_PUSH_RECEIVED",
                "android.provider.Telephony.WAP_PUSH_DELIVER" -> {
                    // WAP推送消息处理
                    Log.d(TAG, "📲 Processing WAP Push message")
                    processWapPushMessage(context, intent)
                    return messages // WAP push特殊处理，不走常规SMS流程
                }
                
                "android.provider.Telephony.SMS_REJECTED" -> {
                    // 被拒绝的SMS
                    Log.d(TAG, "❌ Processing rejected SMS")
                    processRejectedSms(context, intent)
                    return messages // 被拒绝SMS的特殊处理
                }
                
                "android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED" -> {
                    // 服务类别程序数据
                    Log.d(TAG, "🔧 Processing service category program data")
                    // 这类消息通常用于更新广播频道列表，不需要转发
                    return messages
                }
                
                else -> {
                    Log.w(TAG, "⚠️ Unknown SMS action: $action")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error extracting SMS messages: ${e.message}", e)
        }
        
        return messages
    }

    private fun processSmsMessage(context: Context, smsMessage: SmsMessage) {
        val sender = smsMessage.originatingAddress ?: "Unknown"
        val content = smsMessage.messageBody ?: ""
        val timestamp = smsMessage.timestampMillis

        Log.d(TAG, "📨 Processing SMS from: $sender, length: ${content.length} chars")
        Log.d(TAG, "📝 Content preview: ${content.take(50)}${if (content.length > 50) "..." else ""}")

        // Extract SIM card information
        var simSlot: String? = null
        var simOperator: String? = null
        
        try {
            val simCardInfo = SimCardManager.getSimByPhoneNumber(context, sender)
            
            if (simCardInfo != null) {
                simSlot = simCardInfo.getFriendlyName()
                simOperator = simCardInfo.carrierName
                Log.d(TAG, "📱 SIM info detected - Slot: $simSlot, Operator: $simOperator")
            } else {
                // Try to get primary SIM if phone number lookup fails
                val primarySimInfo = SimCardManager.getPrimarySimCard(context)
                if (primarySimInfo != null) {
                    simSlot = primarySimInfo.getFriendlyName()
                    simOperator = primarySimInfo.carrierName
                    Log.d(TAG, "📱 Using primary SIM info - Slot: $simSlot, Operator: $simOperator")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to extract SIM card info: ${e.message}")
        }

        enqueueSmsProcessing(context, sender, content, timestamp, simSlot, simOperator)
    }

    private fun enqueueSmsProcessing(context: Context, sender: String, content: String, timestamp: Long, simSlot: String? = null, simOperator: String? = null) {
        Log.d(TAG, "📤 Enqueueing SMS processing (SIMPLE MODE):")
        Log.d(TAG, "   📞 Sender: $sender")
        Log.d(TAG, "   📝 Content length: ${content.length}")
        Log.d(TAG, "   🕐 Timestamp: $timestamp")
        Log.d(TAG, "   📱 SIM Slot: ${simSlot ?: "Unknown"}")
        Log.d(TAG, "   📡 SIM Operator: ${simOperator ?: "Unknown"}")
        
        // 获取最优执行策略（保留基本的后台保障机制）
        val optimalStrategy = backgroundReliabilityManager.getOptimalStrategy()
        
        Log.d(TAG, "🎯 Using optimal strategy: ${optimalStrategy.getDisplayName()}")
        
        // 简化策略选择逻辑
        when (optimalStrategy) {
            ExecutionStrategy.FOREGROUND_SERVICE -> {
                enqueueForegroundServiceProcessing(context, sender, content, timestamp, simSlot, simOperator)
            }
            ExecutionStrategy.WORK_MANAGER_EXPEDITED -> {
                enqueueWorkManagerProcessing(context, sender, content, timestamp, true, simSlot, simOperator)
            }
            ExecutionStrategy.WORK_MANAGER_NORMAL -> {
                enqueueWorkManagerProcessing(context, sender, content, timestamp, false, simSlot, simOperator)
            }
            ExecutionStrategy.HYBRID_AUTO_SWITCH -> {
                // 混合策略：根据当前设备状态动态选择
                val shouldUseForegroundService = backgroundReliabilityManager.shouldUseForegroundService()
                if (shouldUseForegroundService) {
                    enqueueForegroundServiceProcessing(context, sender, content, timestamp, simSlot, simOperator)
                } else {
                    enqueueWorkManagerProcessing(context, sender, content, timestamp, true, simSlot, simOperator) // 默认使用expedited
                }
            }
        }
    }
    
    /**
     * 使用WorkManager处理短信转发 (简化版)
     */
    private fun enqueueWorkManagerProcessing(
        context: Context,
        sender: String,
        content: String,
        timestamp: Long,
        useExpedited: Boolean,
        simSlot: String? = null,
        simOperator: String? = null
    ) {
        Log.d(TAG, "⚙️ Using WorkManager for SMS processing (expedited: $useExpedited)")
        
        // 🚨 减少约束以确保任务能够执行
        val constraints = Constraints.Builder()
            // 移除网络约束 - 邮件发送时再检查网络
            // .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        Log.d(TAG, "📋 WorkManager constraints: ${if (constraints.hasContentUriTriggers()) "HAS_TRIGGERS" else "NO_CONSTRAINTS"}")
        
        // 构建WorkRequest
        val workRequestBuilder = OneTimeWorkRequestBuilder<SmsForwardWorker>()
            .setInputData(
                workDataOf(
                    "messageId" to System.currentTimeMillis(), // 使用当前时间作为唯一ID
                    "sender" to sender,
                    "content" to content,
                    "timestamp" to timestamp,
                    "originalTimestamp" to timestamp,
                    "queueTime" to System.currentTimeMillis(),
                    "executionStrategy" to if (useExpedited) ExecutionStrategy.WORK_MANAGER_EXPEDITED.name else ExecutionStrategy.WORK_MANAGER_NORMAL.name,
                    "simSlot" to (simSlot ?: ""),
                    "simOperator" to (simOperator ?: "")
                )
            )
            .setConstraints(constraints)
            .addTag("sms_forward") // 添加标签以便追踪
            .addTag("unconditional_forward") // 添加无条件转发标签
        
        // 设置expedited和退避策略
        if (useExpedited) {
            workRequestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            workRequestBuilder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS) // 减少初始退避时间
            Log.d(TAG, "⚡ Using expedited work with fast retry (15s)")
        } else {
            workRequestBuilder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS) // 减少标准退避时间
            Log.d(TAG, "📋 Using normal work with standard retry (30s)")
        }
        
        val workRequest = workRequestBuilder.build()
        
        // 使用不同的排队策略以确保执行
        val workManager = WorkManager.getInstance(context)
        val uniqueWorkName = "sms_forward_${System.currentTimeMillis()}"
        
        Log.d(TAG, "🎯 Enqueueing work with name: $uniqueWorkName")
        
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        // 🔍 添加immediate状态检查
        try {
            val workInfo = workManager.getWorkInfoById(workRequest.id).get()
            Log.d(TAG, "📊 Work info immediately after enqueue:")
            Log.d(TAG, "   📝 State: ${workInfo?.state}")
            Log.d(TAG, "   🔄 Run attempt: ${workInfo?.runAttemptCount}")
            Log.d(TAG, "   🏷️ Tags: ${workInfo?.tags}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Could not get immediate work info: ${e.message}")
        }
        
        // 🕐 添加延迟状态检查 - 5秒后检查WorkManager执行状态
        CoroutineScope(Dispatchers.IO).launch {
            try {
                kotlinx.coroutines.delay(5000) // 等待5秒
                val delayedWorkInfo = workManager.getWorkInfoById(workRequest.id).get()
                Log.i(TAG, "⏰ Work status after 5 seconds:")
                Log.i(TAG, "   📝 State: ${delayedWorkInfo?.state}")
                Log.i(TAG, "   🔄 Run attempt: ${delayedWorkInfo?.runAttemptCount}")
                Log.i(TAG, "   📊 Progress: ${delayedWorkInfo?.progress}")
                Log.i(TAG, "   ❗ Output data: ${delayedWorkInfo?.outputData?.keyValueMap}")
                
                // 如果5秒后还是ENQUEUED状态，说明可能有问题
                if (delayedWorkInfo?.state == androidx.work.WorkInfo.State.ENQUEUED) {
                    Log.w(TAG, "⚠️ Work still ENQUEUED after 5 seconds - possible execution issue")
                    println("🔥 WorkManager WARNING: Task still enqueued after 5s")
                    
                    // 检查所有相关的工作状态
                    val allSmsWork = workManager.getWorkInfosByTag("sms_forward").get()
                    Log.w(TAG, "📊 All SMS work status:")
                    allSmsWork.forEachIndexed { index, info ->
                        Log.w(TAG, "   Work $index: ${info.state} (${info.id})")
                    }
                } else if (delayedWorkInfo?.state == androidx.work.WorkInfo.State.RUNNING) {
                    Log.i(TAG, "✅ Work is RUNNING - execution successful")
                    println("🔥 WorkManager SUCCESS: Task is running")
                } else if (delayedWorkInfo?.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                    Log.i(TAG, "🎉 Work SUCCEEDED - forward completed")
                    println("🔥 WorkManager SUCCESS: Task completed")
                } else if (delayedWorkInfo?.state == androidx.work.WorkInfo.State.FAILED) {
                    Log.e(TAG, "❌ Work FAILED - check logs above")
                    println("🔥 WorkManager FAILED: Task failed")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Could not get delayed work info: ${e.message}")
            }
        }
        
        Log.d(TAG, "✅ WorkManager SMS processing enqueued successfully")
        Log.d(TAG, "   📞 Sender: $sender")
        Log.d(TAG, "   🆔 Work ID: ${workRequest.id}")
        Log.d(TAG, "   ⚡ Expedited: $useExpedited")
        Log.d(TAG, "   🏷️ Tags: sms_forward, unconditional_forward")
        Log.d(TAG, "   🎯 Unique name: $uniqueWorkName")
    }
    
    /**
     * 使用前台服务处理短信转发 (简化版)
     */
    private fun enqueueForegroundServiceProcessing(
        context: Context,
        sender: String,
        content: String,
        timestamp: Long,
        simSlot: String? = null,
        simOperator: String? = null
    ) {
        Log.d(TAG, "🔔 Using foreground service for SMS processing")
        
        try {
            // 使用协程启动suspend函数
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 使用简化的参数启动前台服务
                    ForegroundServiceManager.enqueueSmsProcessing(
                        messageId = System.currentTimeMillis().toString(),
                        sender = sender,
                        content = content,
                        timestamp = timestamp,
                        priority = com.example.test.utils.SmsAnalyzer.MessagePriority.NORMAL, // 统一优先级
                        messageType = com.example.test.utils.SmsAnalyzer.MessageType.GENERAL, // 统一类型
                        confidence = 1.0f // 固定置信度
                    )
                    
                    Log.d(TAG, "✅ Foreground service SMS processing enqueued successfully")
                    Log.d(TAG, "   📞 Sender: $sender")
                    Log.d(TAG, "   🔔 Service: Foreground EmailService")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to start foreground service in coroutine", e)
                    
                    // 前台服务启动失败，回退到WorkManager（在主线程执行）
                    CoroutineScope(Dispatchers.Main).launch {
                        enqueueWorkManagerProcessing(context, sender, content, timestamp, false)
                    }
                    
                    // 记录失败原因
                    backgroundReliabilityManager.recordExecutionResult(
                        com.example.test.domain.model.StrategyExecutionResult(
                            strategy = ExecutionStrategy.FOREGROUND_SERVICE,
                            success = false,
                            executionTimeMs = 0L,
                            errorReason = "Failed to start foreground service: ${e.message}",
                            messageType = "SMS",
                            messagePriority = "NORMAL"
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch coroutine for foreground service", e)
            
            // 协程启动失败，直接回退到WorkManager
            enqueueWorkManagerProcessing(context, sender, content, timestamp, false)
            
            // 记录失败原因
            backgroundReliabilityManager.recordExecutionResult(
                com.example.test.domain.model.StrategyExecutionResult(
                    strategy = ExecutionStrategy.FOREGROUND_SERVICE,
                    success = false,
                    executionTimeMs = 0L,
                    errorReason = "Failed to launch coroutine: ${e.message}",
                    messageType = "SMS",
                    messagePriority = "NORMAL"
                )
            )
        }
    }

    /**
     * 处理Cell Broadcast消息
     */
    private fun processCellBroadcastMessage(context: Context, intent: Intent) {
        try {
            val sender = "Cell Broadcast"
            val content = intent.extras?.getString("message") ?: "Cell Broadcast Message"
            val timestamp = System.currentTimeMillis()
            
            Log.d(TAG, "📡 Cell Broadcast received:")
            Log.d(TAG, "   📱 Service Category: ${intent.extras?.getInt("service_category", -1)}")
            Log.d(TAG, "   📝 Content: $content")
            Log.d(TAG, "   🚨 Emergency: ${intent.action?.contains("EMERGENCY") == true}")
            
            if (content.isNotEmpty()) {
                enqueueSmsProcessing(context, sender, content, timestamp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing Cell Broadcast: ${e.message}", e)
        }
    }
    
    /**
     * 处理WAP Push消息
     */
    private fun processWapPushMessage(context: Context, intent: Intent) {
        try {
            val transactionId = intent.getIntExtra("transactionId", -1)
            val pduType = intent.getIntExtra("pduType", -1)
            val header = intent.getByteArrayExtra("header")
            val data = intent.getByteArrayExtra("data")
            
            val sender = "WAP Push"
            val content = "WAP Push message received (Transaction ID: $transactionId, PDU Type: $pduType)"
            val timestamp = System.currentTimeMillis()
            
            Log.d(TAG, "📲 WAP Push received:")
            Log.d(TAG, "   🆔 Transaction ID: $transactionId")
            Log.d(TAG, "   📋 PDU Type: $pduType")
            Log.d(TAG, "   📊 Data size: ${data?.size ?: 0} bytes")
            
            enqueueSmsProcessing(context, sender, content, timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing WAP Push: ${e.message}", e)
        }
    }
    
    /**
     * 处理被拒绝的SMS
     */
    private fun processRejectedSms(context: Context, intent: Intent) {
        try {
            val result = intent.getIntExtra("result", -1)
            val sender = "System"
            val content = "SMS was rejected by telephony framework (Result code: $result)"
            val timestamp = System.currentTimeMillis()
            
            Log.d(TAG, "❌ SMS Rejected:")
            Log.d(TAG, "   📊 Result code: $result")
            
            enqueueSmsProcessing(context, sender, content, timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing rejected SMS: ${e.message}", e)
        }
    }
} 