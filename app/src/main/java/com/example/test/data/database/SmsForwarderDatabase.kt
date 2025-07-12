package com.example.test.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.test.data.database.converter.Converters
import com.example.test.data.database.dao.*
import com.example.test.data.database.entity.*

@Database(
    entities = [
        SmsMessageEntity::class,
        EmailConfigEntity::class,
        ForwardRuleEntity::class,
        ForwardRecordEntity::class,
        ForwardStatisticsEntity::class
    ],
    version = 5,  // 将版本号从4增加到5，添加SIM卡字段支持
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmsForwarderDatabase : RoomDatabase() {

    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun emailConfigDao(): EmailConfigDao
    abstract fun forwardRuleDao(): ForwardRuleDao
    abstract fun forwardRecordDao(): ForwardRecordDao
    abstract fun forwardStatisticsDao(): ForwardStatisticsDao

    companion object {
        @Volatile
        private var INSTANCE: SmsForwarderDatabase? = null

        private const val DATABASE_NAME = "sms_forwarder_database"

        fun getDatabase(context: Context): SmsForwarderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsForwarderDatabase::class.java,
                    DATABASE_NAME
                )
                    // 暂时移除复杂的migrations，让fallback处理所有情况
                    // .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()  // 数据库版本不匹配时重建
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Database created for the first time
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Database opened
            }
        }

        // Migration from version 1 to version 2
        // Added provider field to email_configs table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add provider column to email_configs table with default value
                database.execSQL(
                    "ALTER TABLE email_configs ADD COLUMN provider TEXT NOT NULL DEFAULT 'GMAIL'"
                )
            }
        }

        // Migration from version 2 to version 3
        // Schema adjustments for recent entity changes
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No specific schema changes needed between version 2 and 3
                // This migration exists to handle schema hash changes due to code refactoring
                // The fallbackToDestructiveMigration() will handle any missing migrations
            }
        }

        // Migration from version 3 to version 4
        // Added monitoring fields to forward_records table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add all missing monitoring fields to forward_records table
                database.execSQL("ALTER TABLE forward_records ADD COLUMN executionStrategy TEXT")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN messageType TEXT")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN messagePriority TEXT")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN confidenceScore REAL")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN deviceBatteryLevel INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN deviceIsCharging INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN deviceIsInDozeMode INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN networkType TEXT")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN backgroundCapabilityScore INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN failureCategory TEXT")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN executionDurationMs INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN emailSendDurationMs INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN queueWaitTimeMs INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN isAutoRetry INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN originalTimestamp INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN processingDelayMs INTEGER")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN systemLoad TEXT")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN vendorOptimizationActive INTEGER")
            }
        }

        // Migration from version 4 to version 5
        // Added SIM card fields to forward_records table
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE forward_records ADD COLUMN simSlot TEXT")
                database.execSQL("ALTER TABLE forward_records ADD COLUMN simOperator TEXT")
            }
        }
    }
} 