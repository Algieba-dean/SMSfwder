# 快速测试SMS转发功能

## 🚀 测试步骤

### 步骤1：确认应用状态
1. 打开应用，进入"设置"页面
2. 检查"SMS功能状态"区域：
   - SMS权限已授予 ✅
   - SMS转发功能就绪 ✅
3. 检查"邮箱配置"状态：邮箱已配置 ✅

### 步骤2：使用模拟器测试（推荐）
1. **打开Extended Controls**：
   - 模拟器右侧点击"..."按钮
   - 或按快捷键 `Ctrl+Shift+P`

2. **发送测试短信**：
   - 选择"Phone" > "SMS"
   - 发送方：`13812345678`
   - 内容：`【工商银行】您的账户1234收到转账1000.00元`
   - 点击"Send Message"

### 步骤3：观察结果
发送短信后，应该看到：

1. **主页面变化**（5-10秒内）：
   - "今日"数字增加
   - "最近消息"区域显示新短信

2. **日志页面**：
   - 新增转发记录
   - 显示处理状态

3. **邮箱检查**：
   - 接收邮箱收到转发邮件
   - 邮件包含短信内容和发送方信息

## 🐛 如果没有反应，检查日志

在Android Studio中查看日志：
```
View > Tool Windows > Logcat
```

过滤日志查看以下标签：
- `SmsReceiver` - SMS接收日志
- `SmsForwardWorker` - 转发处理日志
- `com.example.test` - 应用所有日志

## 🔧 调试方法

### 方法1：应用内测试（Debug版本）
在设置页面底部的"调试测试"区域：
1. 点击"银行通知"按钮
2. 观察是否有反应

### 方法2：ADB命令测试
```bash
adb emu sms send 13812345678 "【工商银行】测试转账通知"
```

### 方法3：检查权限
如果仍然没有反应：
1. 进入系统设置
2. 应用管理 > SMS转发器 > 权限
3. 确保"短信"权限已开启

## 📊 预期行为

正常工作时的流程：
1. 收到SMS → SmsReceiver接收
2. 创建WorkManager任务
3. 保存短信到数据库
4. 发送邮件
5. 更新界面统计

每个步骤都会有相应的日志输出。 