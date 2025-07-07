# 📱 SMSforwarder 图标替换指南

## 概述
本指南将帮助您将自定义的 `smsfwder.jpg` 图片替换为应用图标。

## 🎯 准备工作

### 1. 图片要求
- **格式**: JPG/PNG (推荐PNG，支持透明背景)
- **尺寸**: 建议 512x512px 或更高
- **设计**: 简洁清晰，适合小尺寸显示

### 2. 需要的工具
- **在线工具** (推荐): [App Icon Generator](https://appicon.co/) 或 [Icon Kitchen](https://icon.kitchen/)
- **本地工具**: Android Studio 的 Image Asset Studio

## 📐 方案一：使用在线工具 (推荐)

### 步骤1: 生成图标包
1. 访问 [App Icon Generator](https://appicon.co/)
2. 上传您的 `smsfwder.jpg` 文件
3. 选择 "Android" 平台
4. 下载生成的图标包

### 步骤2: 替换项目文件
将下载的图标文件替换到对应目录：

```
app/src/main/res/
├── mipmap-mdpi/
│   ├── ic_launcher.png (48x48)
│   └── ic_launcher_round.png (48x48)
├── mipmap-hdpi/
│   ├── ic_launcher.png (72x72)
│   └── ic_launcher_round.png (72x72)
├── mipmap-xhdpi/
│   ├── ic_launcher.png (96x96)
│   └── ic_launcher_round.png (96x96)
├── mipmap-xxhdpi/
│   ├── ic_launcher.png (144x144)
│   └── ic_launcher_round.png (144x144)
└── mipmap-xxxhdpi/
    ├── ic_launcher.png (192x192)
    └── ic_launcher_round.png (192x192)
```

## 🛠️ 方案二：使用Android Studio

### 步骤1: 打开Image Asset Studio
1. 在Android Studio中右键点击 `app` 模块
2. 选择 `New > Image Asset`
3. 选择 `Launcher Icons (Adaptive and Legacy)`

### 步骤2: 配置图标
1. **Icon Type**: 选择 "Launcher Icons (Adaptive and Legacy)"
2. **Foreground Layer**: 
   - 选择 "Image"
   - 点击文件夹图标，选择您的 `smsfwder.jpg`
   - 调整 "Resize" 滑块来合适显示
3. **Background Layer**: 
   - 选择 "Color"
   - 设置背景颜色 (建议: #4CAF50 绿色)

### 步骤3: 预览和生成
1. 预览不同尺寸的效果
2. 点击 "Next" → "Finish"

## 🎨 手动方案：使用当前向量图标

如果您想基于当前的向量图标进行调整：

### 修改前景图标
编辑文件: `app/src/main/res/drawable/ic_launcher_foreground.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108"
    android:tint="#FFFFFF">
    
    <!-- 在这里添加您的自定义路径 -->
    <!-- 可以从SVG转换为vector drawable -->
    
</vector>
```

### 修改背景颜色
编辑文件: `app/src/main/res/drawable/ic_launcher_background.xml`

## ✅ 验证结果

### 1. 清理和重新构建
```bash
./gradlew clean
./gradlew build
```

### 2. 检查效果
- 在设备上安装应用
- 查看桌面图标显示效果
- 在应用抽屉中查看图标
- 测试圆形图标显示

## 📱 当前状态

目前应用已经配置了：
- ✅ **应用名称**: SMSforwarder
- ✅ **版本信息**: 1.0.0
- ✅ **临时图标**: 绿色主题，SMS转发图标
- ✅ **版本显示**: 在设置页面的"关于"部分

## 🔧 故障排除

### 图标不显示
1. 确保文件名正确: `ic_launcher.png` 和 `ic_launcher_round.png`
2. 检查文件路径是否正确
3. 清理项目: `Build > Clean Project`
4. 重新构建: `Build > Rebuild Project`

### 图标模糊
- 确保提供高分辨率的原始图片
- 检查各个密度文件夹的图标尺寸是否正确

### 图标背景异常
- 检查 `ic_launcher_background.xml` 背景设置
- 确保前景图标有适当的内边距

## 💡 建议

1. **保持简洁**: 图标在小尺寸下要清晰可见
2. **一致性**: 确保圆形和方形图标视觉一致
3. **测试**: 在不同设备和启动器上测试效果
4. **备份**: 保存原始的高分辨率图片文件

---

如果您需要帮助处理具体的图片文件，请提供图片或告诉我图片的具体位置！ 