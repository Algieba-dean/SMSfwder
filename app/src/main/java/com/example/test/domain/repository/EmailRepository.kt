package com.example.test.domain.repository

import com.example.test.domain.model.EmailConfig
import kotlinx.coroutines.flow.Flow

interface EmailRepository {

    fun getAllConfigs(): Flow<List<EmailConfig>>

    suspend fun getConfigById(id: Long): EmailConfig?

    suspend fun getDefaultConfig(): EmailConfig?

    fun getDefaultConfigFlow(): Flow<EmailConfig?>

    suspend fun getConfigByEmail(email: String): EmailConfig?

    suspend fun insertConfig(config: EmailConfig): Long

    suspend fun updateConfig(config: EmailConfig)

    suspend fun deleteConfig(config: EmailConfig)

    suspend fun setDefaultConfig(id: Long)

    suspend fun testEmailConnection(config: EmailConfig): Result<String>

    suspend fun sendTestEmail(config: EmailConfig, toEmail: String): Result<String>
} 