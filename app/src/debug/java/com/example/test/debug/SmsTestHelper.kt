package com.example.test.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.test.service.SmsReceiver

/**
 * SMS测试辅助工具类
 * 仅在Debug版本中可用，用于模拟各种类型的短信
 */
object SmsTestHelper {
    
    private const val TAG = "SmsTestHelper"
    
    // 模拟接收到的各种测试短信
    private val testSmsMessages = listOf(
        TestSms("+8613812345678", "【工商银行】您的账户1234收到转账1000.00元，余额12345.67元"),
        TestSms("+8613987654321", "【淘宝网】验证码123456，用于登录，5分钟内有效"),
        TestSms("+8613555666777", "【京东】您的订单已发货，快递单号：SF1234567890"),
        TestSms("+8613111222333", "【微信支付】您已成功付款100.00元给商户某某超市"),
        TestSms("+8613444555666", "【建设银行】您尾号1234的卡片消费500.00元"),
        TestSms("+8613777888999", "您的手机话费余额不足，请及时充值"),
        TestSms("+8613000111222", "【支付宝】验证码：654321，用于身份验证，请勿泄露"),
        TestSms("+8613333444555", "恭喜您中奖了！请点击链接领取奖品..."), // 垃圾短信示例
    )
    
    /**
     * 模拟发送单条测试短信 - 正确模拟系统SMS广播
     */
    fun simulateIncomingSms(context: Context, sender: String, message: String) {
        try {
            Log.d(TAG, "Simulating SMS from: $sender, message: $message")
            
            // 创建模拟的SMS PDU数据
            val pduBundle = Bundle().apply {
                putString("format", "3gpp")
                
                // 创建简化的PDU数据
                // 注意：这是一个简化的实现，真实的PDU格式更复杂
                val pduData = createSimplePdu(sender, message)
                putSerializable("pdus", arrayOf(pduData))
            }
            
            // 创建SMS_RECEIVED广播
            val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
                putExtras(pduBundle)
            }
            
            // 直接调用我们的SMS接收器
            val receiver = SmsReceiver()
            receiver.onReceive(context, intent)
            
            Log.d(TAG, "SMS simulation completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error simulating SMS: ${e.message}", e)
            // 如果模拟失败，尝试简化的方法
            simulateIncomingSmsSimple(context, sender, message)
        }
    }
    
    /**
     * 简化的SMS模拟方法（备用）
     */
    private fun simulateIncomingSmsSimple(context: Context, sender: String, message: String) {
        try {
            Log.d(TAG, "Using simplified SMS simulation")
            
            val intent = Intent("android.provider.Telephony.SMS_RECEIVED").apply {
                putExtra("sender", sender)
                putExtra("message", message)
                putExtra("timestamp", System.currentTimeMillis())
            }
            
            // 直接调用我们的SMS接收器
            val receiver = SmsReceiver()
            receiver.onReceive(context, intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in simplified SMS simulation: ${e.message}", e)
        }
    }
    
    /**
     * 创建简化的PDU数据
     */
    private fun createSimplePdu(sender: String, message: String): ByteArray {
        // 这是一个非常简化的PDU创建方法
        // 在真实场景中，PDU格式会更复杂
        val timestamp = System.currentTimeMillis()
        val pdu = StringBuilder()
        
        // SMS-DELIVER PDU 的基本结构
        pdu.append("00") // SMS-DELIVER
        pdu.append("0C") // 发送方号码长度
        pdu.append("91") // 国际号码格式
        
        // 简化的号码编码
        val encodedSender = sender.replace("+86", "").reversed()
        pdu.append(encodedSender.padEnd(12, 'F'))
        
        pdu.append("00") // PID
        pdu.append("00") // DCS
        
        // 时间戳（简化）
        pdu.append("00000000000000")
        
        // 消息长度
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        pdu.append(String.format("%02X", messageBytes.size))
        
        // 消息内容
        messageBytes.forEach { byte ->
            pdu.append(String.format("%02X", byte.toInt() and 0xFF))
        }
        
        return hexStringToByteArray(pdu.toString())
    }
    
    /**
     * 十六进制字符串转字节数组
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
    
    /**
     * 发送预设的测试短信
     */
    fun sendTestSms(context: Context, index: Int) {
        if (index in testSmsMessages.indices) {
            val testSms = testSmsMessages[index]
            simulateIncomingSms(context, testSms.sender, testSms.message)
        }
    }
    
    /**
     * 发送所有预设测试短信（用于批量测试）
     */
    fun sendAllTestSms(context: Context, delayMillis: Long = 2000) {
        testSmsMessages.forEachIndexed { index, testSms ->
            // 使用Handler延迟发送，模拟真实短信间隔
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                simulateIncomingSms(context, testSms.sender, testSms.message)
            }, index * delayMillis)
        }
    }
    
    /**
     * 获取所有测试短信列表（用于UI展示）
     */
    fun getTestSmsMessages(): List<TestSms> = testSmsMessages
    
    data class TestSms(
        val sender: String,
        val message: String,
        val type: SmsType = detectSmsType(message)
    )
    
    enum class SmsType {
        BANKING,      // 银行通知
        VERIFICATION, // 验证码
        SHOPPING,     // 购物通知
        PAYMENT,      // 支付通知
        SPAM,         // 垃圾短信
        NORMAL        // 普通短信
    }
    
    private fun detectSmsType(message: String): SmsType {
        return when {
            message.contains(Regex("银行|转账|余额|消费")) -> SmsType.BANKING
            message.contains(Regex("验证码|\\d{4,6}")) -> SmsType.VERIFICATION
            message.contains(Regex("订单|发货|快递")) -> SmsType.SHOPPING
            message.contains(Regex("支付|付款|微信支付|支付宝")) -> SmsType.PAYMENT
            message.contains(Regex("中奖|链接|点击")) -> SmsType.SPAM
            else -> SmsType.NORMAL
        }
    }
} 