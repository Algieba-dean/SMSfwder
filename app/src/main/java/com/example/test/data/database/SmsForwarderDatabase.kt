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
    version = 1,
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

        // Future migrations can be added here
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration logic when needed
            }
        }
    }
} 