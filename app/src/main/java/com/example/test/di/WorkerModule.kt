package com.example.test.di

import androidx.work.WorkManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WorkerModule - 为WorkManager相关组件提供依赖注入
 * 
 * 负责：
 * - 提供WorkManager实例
 * - 配置Worker相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    /**
     * 提供WorkManager单例实例
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
} 