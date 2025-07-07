# SMS调试日志查看指南

## 🔍 在Android Studio中查看日志

### 1. 打开Logcat
- View → Tool Windows → Logcat
- 或使用快捷键 Alt+6 (Windows)

### 2. 设置过滤器
在Logcat的搜索框中输入以下关键词：

**查看SMS接收日志：**
```
SmsReceiver
```

**查看应用所有日志：**
```
com.example.test
```

**查看WorkManager日志：**
```
SmsForwardWorker
```

### 3. 发送测试短信后查看
发送短信后应该看到类似日志：
```
D/SmsReceiver: SMS broadcast received, action: android.provider.Telephony.SMS_RECEIVED
D/SmsReceiver: Intent extras: pdus, format, subscription
D/SmsReceiver: Found 1 SMS message(s)
D/SmsReceiver: Processing SMS from: +1234567890, content length: 50
D/SmsReceiver: SMS processing work enqueued for message from: +1234567890
```

### 4. 如果没有看到任何SmsReceiver日志
说明SMS接收器根本没有被触发，可能的原因：
- 应用没有成为默认SMS应用
- SMS接收器注册有问题
- 权限配置不正确
- 系统级别的拦截

### 5. 清理日志并重新测试
1. 点击Logcat左上角的垃圾桶图标清理日志
2. 使用模拟器Extended Controls发送测试短信
3. 立即查看是否有新的日志输出 