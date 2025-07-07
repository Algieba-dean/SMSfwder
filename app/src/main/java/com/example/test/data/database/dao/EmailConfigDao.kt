package com.example.test.data.database.dao

import androidx.room.*
import com.example.test.data.database.entity.EmailConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailConfigDao {

    @Query("SELECT * FROM email_configs ORDER BY createdAt DESC")
    fun getAllConfigs(): Flow<List<EmailConfigEntity>>

    @Query("SELECT * FROM email_configs WHERE id = :id")
    suspend fun getConfigById(id: Long): EmailConfigEntity?

    @Query("SELECT * FROM email_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultConfig(): EmailConfigEntity?

    @Query("SELECT * FROM email_configs WHERE isDefault = 1 LIMIT 1")
    fun getDefaultConfigFlow(): Flow<EmailConfigEntity?>

    @Query("SELECT * FROM email_configs WHERE senderEmail = :email")
    suspend fun getConfigByEmail(email: String): EmailConfigEntity?

    @Insert
    suspend fun insertConfig(config: EmailConfigEntity): Long

    @Update
    suspend fun updateConfig(config: EmailConfigEntity)

    @Delete
    suspend fun deleteConfig(config: EmailConfigEntity)

    @Query("UPDATE email_configs SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE email_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefault(id: Long)

    @Transaction
    suspend fun setDefaultConfig(id: Long) {
        clearDefaultFlags()
        setAsDefault(id)
    }
} 