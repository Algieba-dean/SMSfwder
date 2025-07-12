# 更新日志 (Changelog)

本文档记录了SMSforwarder项目的所有重要更改。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [未发布] - Unreleased

### 🐛 修复 (Fixed)
- **后台转发测试功能**: 修复了"测试后台转发"按钮只显示模拟结果而不实际发送邮件的问题
  - 现在点击测试按钮会真正发送一封测试邮件到配置的邮箱
  - 测试邮件包含详细的转发信息和时间戳
  - 修复了编译错误：正确使用EmailResult的message属性而不是errorMessage
- **UI兼容性**: 修复Icons.Default.Send的弃用警告，使用Icons.AutoMirrored.Filled.Send

### 🔧 技术改进 (Technical)
- 在BackgroundOptimizationViewModel中添加EmailRepository依赖注入
- 改进测试邮件内容格式，包含更多调试信息
- 增强错误处理和日志记录

### 📚 文档 (Documentation)
- 更新README.md，添加后台测试功能说明
- 创建CHANGELOG.md记录版本历史

## [v1.0.0] - 2024-01-15

### 🎉 新增 (Added)
- 初始版本发布
- 完整的SMS短信转发功能
- 邮箱配置和测试
- 现代化Material Design 3界面
- 转发日志和统计
- GitHub Actions自动构建和发布

### 🛡️ 安全 (Security)
- 本地数据存储，不上传任何敏感信息
- 邮箱配置安全存储

---

### 版本说明

- **🎉 新增 (Added)**: 新功能
- **🔧 改进 (Changed)**: 现有功能的变更
- **🗑️ 弃用 (Deprecated)**: 即将移除的功能
- **🚫 移除 (Removed)**: 已移除的功能
- **🐛 修复 (Fixed)**: Bug修复
- **🛡️ 安全 (Security)**: 安全性修复和改进 