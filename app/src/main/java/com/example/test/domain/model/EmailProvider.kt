package com.example.test.domain.model

enum class EmailProvider(
    val displayName: String,
    val smtpHost: String,
    val smtpPort: Int,
    val enableTLS: Boolean,
    val enableSSL: Boolean
) {
    GMAIL(
        displayName = "Gmail",
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        enableTLS = true,
        enableSSL = false
    ),
    OUTLOOK(
        displayName = "Outlook",
        smtpHost = "smtp-mail.outlook.com",
        smtpPort = 587,
        enableTLS = true,
        enableSSL = false
    ),
    QQ_MAIL(
        displayName = "QQ邮箱",
        smtpHost = "smtp.qq.com",
        smtpPort = 587,
        enableTLS = true,
        enableSSL = false
    ),
    NETEASE_163(
        displayName = "163邮箱",
        smtpHost = "smtp.163.com",
        smtpPort = 465,
        enableTLS = false,
        enableSSL = true
    ),
    NETEASE_126(
        displayName = "126邮箱",
        smtpHost = "smtp.126.com",
        smtpPort = 465,
        enableTLS = false,
        enableSSL = true
    ),
    CUSTOM(
        displayName = "自定义",
        smtpHost = "",
        smtpPort = 587,
        enableTLS = true,
        enableSSL = false
    )
} 