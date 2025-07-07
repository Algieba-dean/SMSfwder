package com.example.test.di

import android.content.Context
import androidx.room.Room
import com.example.test.data.database.SmsForwarderDatabase
import com.example.test.data.database.dao.*
import com.example.test.data.repository.*
import com.example.test.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmsForwarderDatabase {
        return SmsForwarderDatabase.getDatabase(context)
    }

    @Provides
    fun provideSmsMessageDao(database: SmsForwarderDatabase): SmsMessageDao {
        return database.smsMessageDao()
    }

    @Provides
    fun provideEmailConfigDao(database: SmsForwarderDatabase): EmailConfigDao {
        return database.emailConfigDao()
    }

    @Provides
    fun provideForwardRuleDao(database: SmsForwarderDatabase): ForwardRuleDao {
        return database.forwardRuleDao()
    }

    @Provides
    fun provideForwardRecordDao(database: SmsForwarderDatabase): ForwardRecordDao {
        return database.forwardRecordDao()
    }

    @Provides
    fun provideForwardStatisticsDao(database: SmsForwarderDatabase): ForwardStatisticsDao {
        return database.forwardStatisticsDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSmsRepository(
        smsRepositoryImpl: SmsRepositoryImpl
    ): SmsRepository

    @Binds
    abstract fun bindEmailRepository(
        emailRepositoryImpl: EmailRepositoryImpl
    ): EmailRepository

    @Binds
    abstract fun bindForwardRepository(
        forwardRepositoryImpl: ForwardRepositoryImpl
    ): ForwardRepository
} 