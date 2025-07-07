# 📱 SMSforwarder 构建和发布指南

## 🏗️ 在Android Studio中构建

### 前置要求
- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17 或更新版本
- Android SDK 35 (Android 14)

### 方法1: 构建Debug APK (推荐测试使用)

1. **打开项目**: 在Android Studio中打开项目
2. **同步项目**: `File` → `Sync Project with Gradle Files`
3. **构建APK**: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
4. **查找APK**: 构建完成后，APK位于 `app/build/outputs/apk/debug/app-debug.apk`

**特点**:
- ✅ 构建快速
- ✅ 可以直接安装测试
- ❌ 仅用于开发和测试

### 方法2: 构建Release APK (生产环境)

#### 选项A: 未签名Release (简单)
1. **构建**: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. **选择Release**: 在Build Variants面板中选择"release"
3. **APK位置**: `app/build/outputs/apk/release/app-release-unsigned.apk`

#### 选项B: 签名Release (推荐)
1. **生成签名APK**: `Build` → `Generate Signed Bundle / APK...`
2. **选择APK类型**: 选择"APK"，点击`Next`
3. **创建或选择密钥库**:
   - **新建**: 点击`Create new...`，填写密钥信息
   - **现有**: 选择已有的`.jks`文件
4. **填写密钥信息**:
   ```
   Key store path: 选择.jks文件位置
   Key store password: 密钥库密码
   Key alias: 密钥别名
   Key password: 密钥密码
   ```
5. **选择构建类型**: 选择"release"，点击`Finish`

### 方法3: 构建Android App Bundle (Google Play)

1. **生成Bundle**: `Build` → `Generate Signed Bundle / APK...`
2. **选择AAB**: 选择"Android App Bundle"，点击`Next`
3. **签名配置**: 同上述Release APK签名步骤
4. **Bundle位置**: `app/build/outputs/bundle/release/app-release.aab`

## 🚀 使用GitHub Actions自动构建和发布

### 设置步骤

#### 1. 推送代码到GitHub
```bash
git add .
git commit -m "Add GitHub Actions workflow"
git push origin main
```

#### 2. 创建版本标签触发构建
```bash
# 创建版本标签
git tag v1.0.0
git push origin v1.0.0
```

#### 3. 或者手动触发
- 访问GitHub仓库的`Actions`页面
- 选择"Build and Release Android App"工作流
- 点击`Run workflow`

### 构建产物

每次成功构建会生成：

| 文件 | 描述 | 用途 |
|------|------|------|
| `SMSforwarder-1.0.0-debug.apk` | Debug版本APK | 开发测试 |
| `SMSforwarder-1.0.0-release-unsigned.apk` | 未签名Release APK | 生产环境(需要签名) |
| `*-checksums.txt` | SHA256校验和 | 文件完整性验证 |

### GitHub Release页面

每次构建会自动创建GitHub Release，包含：
- 📱 详细的版本信息
- 📦 下载链接
- ✨ 功能特性列表
- 📋 安装要求
- 🔒 安全说明

## 🔐 签名配置 (生产环境推荐)

### 创建签名密钥

#### 方法1: 使用Android Studio
`Build` → `Generate Signed Bundle / APK...` → `Create new...`

#### 方法2: 使用命令行
```bash
keytool -genkey -v -keystore smsforwarder-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias smsforwarder
```

### 配置签名 (app/build.gradle)

```gradle
android {
    signingConfigs {
        release {
            storeFile file('smsforwarder-key.jks')
            storePassword 'your_store_password'
            keyAlias 'smsforwarder'
            keyPassword 'your_key_password'
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 安全建议

1. **不要提交密钥文件到Git**:
   ```bash
   echo "*.jks" >> .gitignore
   echo "keystore.properties" >> .gitignore
   ```

2. **使用密钥属性文件**:
   ```properties
   # keystore.properties
   storePassword=your_store_password
   keyPassword=your_key_password
   keyAlias=smsforwarder
   storeFile=smsforwarder-key.jks
   ```

## 📦 发布渠道

### 1. GitHub Releases (推荐)
- ✅ 自动化构建
- ✅ 版本管理
- ✅ 校验和验证
- ✅ 开源透明

### 2. 直接分发
- APK文件可以直接安装
- 需要开启"允许未知来源"

### 3. Google Play Store
1. 使用签名的AAB文件
2. 遵循Google Play政策
3. 需要开发者账号

## 🛠️ 故障排除

### 构建失败
1. **检查Java版本**: 确保使用JDK 17+
2. **清理项目**: `Build` → `Clean Project`
3. **同步Gradle**: `File` → `Sync Project with Gradle Files`
4. **更新依赖**: 检查最新版本

### GitHub Actions失败
1. **检查权限**: 确保有写入权限
2. **查看日志**: 在Actions页面查看详细错误
3. **验证标签**: 确保标签格式正确 (v1.0.0)

### 安装失败
1. **启用未知来源**: 设置 → 安全 → 允许未知来源
2. **检查签名**: 确保APK已正确签名
3. **卸载旧版本**: 如果包名冲突

## 📊 版本管理建议

### 语义化版本控制
- `v1.0.0`: 主要版本.次要版本.修补版本
- `v1.0.1`: 修复bug
- `v1.1.0`: 新功能
- `v2.0.0`: 重大变更

### 发布流程
1. **开发** → `main`分支
2. **测试** → 构建debug版本
3. **准备发布** → 更新版本号
4. **发布** → 创建标签，触发自动构建
5. **分发** → 下载GitHub Release

---

🎉 **恭喜！现在你可以轻松构建和发布SMSforwarder应用了！** 📱✨ 