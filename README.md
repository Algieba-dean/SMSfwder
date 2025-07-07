# 📱 SMSforwarder - 智能短信转发助手

> 自动将重要短信转发到您的邮箱，让您永不错过重要消息！

[![Build Status](https://github.com/Algieba-dean/SMSfwder/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/Algieba-dean/SMSfwder/actions/workflows/build-and-release.yml)
[![Release](https://img.shields.io/github/v/release/Algieba-dean/SMSfwder)](https://github.com/Algieba-dean/SMSfwder/releases)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## ✨ 功能特性

- 🚀 **自动转发**: 智能识别验证码、银行通知等重要短信
- ⚙️ **规则配置**: 灵活的转发规则，支持关键词匹配和发送者过滤
- 📧 **多邮箱支持**: 支持Gmail、Outlook、QQ邮箱等主流邮箱服务
- 📊 **统计分析**: 详细的转发统计和成功率分析
- 📝 **日志记录**: 完整的转发历史和错误日志
- 🎨 **现代界面**: Material Design 3风格的美观界面
- 🔒 **隐私保护**: 本地处理，不上传任何个人信息

## 📱 系统要求

- Android 14+ (API 35+)
- SMS权限 (接收和读取短信)
- 网络权限 (发送邮件)
- 通知权限 (API 33+)

## 🚀 快速开始

### 下载安装

1. 前往 [Releases](https://github.com/Algieba-dean/SMSfwder/releases) 页面
2. 下载最新版本的APK文件
3. 在Android设备上启用"允许未知来源"
4. 安装APK并授予必要权限

### 配置邮箱

1. 打开应用，进入"邮箱配置"页面
2. 选择您的邮箱提供商或手动配置SMTP
3. 填写发送者和接收者邮箱地址
4. 测试连接确保配置正确

### 设置转发规则

1. 进入"设置"页面
2. 启用需要的转发规则：
   - 验证码转发
   - 银行通知转发
   - 垃圾短信过滤
3. 根据需要调整通知设置

## 🏗️ 开发构建

### 环境要求

- Android Studio Hedgehog (2023.1.1+)
- JDK 17+
- Android SDK 35

### 构建步骤

```bash
# 克隆项目
git clone https://github.com/Algieba-dean/SMSfwder.git
cd SMSfwder

# 同步依赖
./gradlew build

# 构建Debug APK
./gradlew assembleDebug

# 构建Release APK
./gradlew assembleRelease
```

详细构建说明请参考 [BUILD_AND_RELEASE_GUIDE.md](BUILD_AND_RELEASE_GUIDE.md)

## 🤖 自动化构建

本项目使用GitHub Actions进行自动化构建和发布：

- ✅ 自动触发：推送版本标签时 (如 `v1.0.0`)
- 📦 自动构建：同时生成Debug和Release APK
- 🚀 自动发布：创建GitHub Release并上传APK
- 🔐 校验和：生成SHA256校验和文件

### 发布新版本

```bash
# 更新版本号 (app/build.gradle)
# versionName "1.0.1"

# 提交更改
git add .
git commit -m "Bump version to 1.0.1"

# 创建并推送标签
git tag v1.0.1
git push origin v1.0.1
```

## 📁 项目结构

```
app/
├── src/main/java/com/example/test/
│   ├── ui/                     # UI层 (Compose界面)
│   │   ├── dashboard/          # 主页面
│   │   ├── settings/           # 设置页面
│   │   ├── email/              # 邮箱配置
│   │   ├── statistics/         # 统计页面
│   │   └── logs/               # 日志页面
│   ├── data/                   # 数据层
│   │   ├── database/           # Room数据库
│   │   ├── repository/         # 数据仓库
│   │   └── preferences/        # 偏好设置
│   ├── domain/                 # 业务逻辑层
│   │   ├── model/              # 数据模型
│   │   └── repository/         # 仓库接口
│   ├── service/                # 服务层
│   │   ├── SmsReceiver         # SMS接收器
│   │   ├── SmsForwardWorker    # 转发工作器
│   │   └── EmailService        # 邮件服务
│   └── utils/                  # 工具类
└── .github/workflows/          # GitHub Actions配置
```

## 🛠️ 技术栈

- **界面**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Repository Pattern
- **依赖注入**: Hilt
- **数据库**: Room
- **异步处理**: Kotlin Coroutines
- **后台任务**: WorkManager
- **邮件发送**: JavaMail API
- **导航**: Navigation Compose

## 🔒 隐私声明

- ✅ 所有数据均在本地设备处理
- ✅ 不上传任何短信内容到外部服务器
- ✅ 邮箱配置信息安全存储在本地
- ✅ 开源代码，透明可审计

## 🤝 贡献

欢迎提交Issue和Pull Request！

1. Fork本项目
2. 创建feature分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 支持

- 📝 [提交Issue](https://github.com/Algieba-dean/SMSfwder/issues)
- 💬 [讨论区](https://github.com/Algieba-dean/SMSfwder/discussions)
- 📧 邮件: algieba.king@gmail.com

---

<div align="center">

**🎉 感谢使用SMSforwarder！**

如果这个项目对您有帮助，请给个⭐Star支持一下！

</div> 