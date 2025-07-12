# SMS 测试指南

## 快速测试短信转发功能

### 1. 环境准备
```bash
# 确保应用已安装并获得必要权限
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 授予短信权限
adb shell pm grant com.example.test android.permission.RECEIVE_SMS
adb shell pm grant com.example.test android.permission.READ_SMS
adb shell pm grant com.example.test android.permission.READ_PHONE_STATE
adb shell pm grant com.example.test android.permission.POST_NOTIFICATIONS

# 确认权限已授予
adb shell dumpsys package com.example.test | grep permission
```

### 2. 配置邮箱
在应用的设置界面中配置：
- 发送方邮箱：你的Gmail账号
- 接收方邮箱：接收转发短信的邮箱
- SMTP配置：Gmail的SMTP设置

### 3. 中文转发模板测试

#### 测试场景1：双卡设备SIM卡信息显示
```bash
# 模拟短信接收
adb shell am broadcast \
  -a android.provider.Telephony.SMS_RECEIVED \
  --es sender "10086" \
  --es message "您的话费余额为100元，流量剩余5GB。" \
  --el timestamp $(date +%s)000

# 预期结果：
# 邮件主题：【卡1(138****1234)】来自 10086 的短信
# 邮件内容包含：
# - 📱 接收SIM卡信息：卡槽、运营商、卡号
# - 📨 消息详情：发送方、接收时间、转发时间、消息长度
# - 📝 中文格式的消息内容
# - 📊 设备状态：Android版本、设备型号、双卡信息
```

#### 测试场景2：银行短信转发
```bash
# 模拟银行短信
adb shell am broadcast \
  -a android.provider.Telephony.SMS_RECEIVED \
  --es sender "95588" \
  --es message "【工商银行】您尾号1234的储蓄卡于12月25日15:30消费500.00元，余额2,850.00元。" \
  --el timestamp $(date +%s)000

# 预期中文模板格式：
# 📲 短信转发通知
# 
# 📱 接收SIM卡信息：
# ├ 卡槽：卡1
# ├ 运营商：中国移动  
# ├ 卡号：138****1234
# ├ 订阅ID：1
# └ 状态：默认SIM卡
# 
# 📨 消息详情：
# ├ 发送方：95588
# ├ 接收时间：2025年01月25日 15:30:25
# ├ 转发时间：2025年01月25日 15:30:26  
# ├ 消息长度：56 字符
# 
# 📝 消息内容：
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 【工商银行】您尾号1234的储蓄卡于12月25日15:30消费500.00元，余额2,850.00元。
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 
# 📊 设备状态：
# ├ 系统：Android 14 (API 34)
# ├ 设备：Google Pixel 6
# ├ 双卡设备：是
# ├ 支持卡槽：2 个
# └ 激活SIM卡：2 张
# 
# 🔧 由 SMS转发器 自动转发（全量转发模式）
```

#### 测试场景3：验证码短信
```bash
# 模拟验证码短信  
adb shell am broadcast \
  -a android.provider.Telephony.SMS_RECEIVED \
  --es sender "106902" \
  --es message "【支付宝】验证码：123456，用于登录确认，请勿泄露。如非本人操作，请忽略。" \
  --el timestamp $(date +%s)000

# 验证中文模板包含完整SIM卡信息
```

### 4. 实时日志监控
```bash
# 监控应用日志
adb logcat -s SmsReceiver SmsForwardWorker EmailSender | grep -E "(SMS|EMAIL|FORWARD)"

# 关键日志标识：
# - 📱 SIM info detected - 检测到SIM卡信息
# - 📲 短信转发通知 - 中文模板构建成功
# - ✅ Email sent! - 邮件发送成功
```

### 5. 邮件接收验证

检查接收邮箱中的转发邮件应包含：

**邮件主题格式：**
- 单卡：`来自 [发送方] 的短信`
- 双卡含号码：`【卡1(138****1234)】来自 [发送方] 的短信`
- 双卡无号码：`【卡1】来自 [发送方] 的短信`

**邮件内容特征：**
- ✅ 中文界面和标签
- ✅ SIM卡详细信息（卡槽、运营商、卡号、状态）
- ✅ 时间戳格式：`yyyy年MM月dd日 HH:mm:ss`
- ✅ 消息内容用分隔线框起
- ✅ 设备信息（Android版本、设备型号、双卡状态）
- ✅ 转发器标识

### 6. 高级测试

#### 多SIM卡场景测试
```bash
# 测试SIM卡信息检测准确性
adb shell content query --uri content://telephony/carriers

# 验证不同卡槽的消息正确标识
# 模拟来自不同卡槽的短信
```

#### 性能测试
```bash
# 连续发送多条短信测试
for i in {1..5}; do
  adb shell am broadcast \
    -a android.provider.Telephony.SMS_RECEIVED \
    --es sender "1008$i" \
    --es message "测试消息 $i：中文转发模板功能验证" \
    --el timestamp $(date +%s)000
  sleep 2
done
```

### 7. 故障排查

**如果转发邮件仍显示英文模板：**
1. 确认应用已重新安装或重启
2. 检查`SmsForwardWorker.kt`编译状态
3. 清除应用数据重新配置

**如果SIM卡信息显示"未知"：**
1. 确认READ_PHONE_STATE权限已授予
2. 检查设备是否支持双卡
3. 查看日志中的SIM卡检测信息

**模板格式异常：**
1. 检查字符编码（应为UTF-8）
2. 验证SimpleDateFormat的Locale设置
3. 确认Kotlin字符串插值正常工作 