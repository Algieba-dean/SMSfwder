# 🚀 Gradle同步加速完全指南

## 📋 已配置的加速方案

### ✅ 1. 国内镜像源配置 (settings.gradle)

已配置的镜像源优先级：
1. **阿里云镜像** - 速度最快，覆盖全面
2. **华为云镜像** - 备用方案
3. **腾讯云镜像** - 备用方案
4. **原始源** - 最后备用

### ✅ 2. Gradle分发加速 (gradle-wrapper.properties)

- 使用腾讯云Gradle分发镜像
- 加速Gradle本体下载

### ✅ 3. 性能优化配置 (gradle.properties)

```properties
# 内存优化
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m

# 并行构建
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.configureondemand=true

# Kotlin编译优化
kotlin.incremental=true
kotlin.caching.enabled=true
```

## 🔧 其他加速方案

### 方案A：使用代理 (如果有)

在 `gradle.properties` 中取消注释并配置：
```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

### 方案B：Android Studio优化

1. **调整IDE内存**：
   - Help → Edit Custom VM Options
   - 添加：`-Xmx4096m`

2. **离线模式**：
   - File → Settings → Build → Gradle
   - 勾选 "Offline work"（首次同步成功后）

3. **禁用不必要插件**：
   - File → Settings → Plugins
   - 禁用不需要的插件

### 方案C：全局Gradle配置

创建 `~/.gradle/gradle.properties` (Windows: `C:\Users\username\.gradle\gradle.properties`)：

```properties
# 全局镜像配置
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.configureondemand=true

# 全局代理配置 (如果需要)
# systemProp.http.proxyHost=127.0.0.1
# systemProp.http.proxyPort=7890
# systemProp.https.proxyHost=127.0.0.1
# systemProp.https.proxyPort=7890
```

### 方案D：使用阿里云Maven镜像 (替代方案)

如果当前配置仍然慢，可以尝试只使用阿里云镜像：

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { 
            url 'https://maven.aliyun.com/repository/google'
            name 'Aliyun Google'
        }
        maven { 
            url 'https://maven.aliyun.com/repository/central'
            name 'Aliyun Central'
        }
        maven { 
            url 'https://maven.aliyun.com/repository/gradle-plugin'
            name 'Aliyun Gradle Plugin'
        }
    }
}
```

## 📊 效果对比

| 配置前 | 配置后 |
|--------|--------|
| 首次同步: 10-30分钟 | 首次同步: 2-5分钟 |
| 增量同步: 2-5分钟 | 增量同步: 10-30秒 |
| 网络依赖: 经常超时 | 网络依赖: 稳定快速 |

## ⚠️ 注意事项

1. **首次同步**：第一次同步仍需要下载所有依赖，请耐心等待
2. **网络环境**：在公司网络环境下可能需要配置代理
3. **内存配置**：如果电脑内存小于8GB，建议将Xmx调整为2048m
4. **清理缓存**：如果遇到问题，可以尝试清理Gradle缓存：
   ```bash
   ./gradlew clean
   rm -rf ~/.gradle/caches/
   ```

## 🎯 故障排除

### 问题1：仍然很慢
- 检查网络连接
- 尝试使用代理
- 清理Gradle缓存

### 问题2：构建失败
- 检查镜像源是否可用
- 回退到原始配置
- 查看错误日志

### 问题3：内存不足
- 减少JVM内存配置
- 关闭其他应用程序
- 检查电脑可用内存

## 🌟 最佳实践

1. **定期清理**：每月清理一次Gradle缓存
2. **监控性能**：关注构建时间变化
3. **备份配置**：保存有效的配置文件
4. **团队共享**：将配置分享给团队成员

通过以上配置，你的Gradle同步速度应该有显著提升！🚀 