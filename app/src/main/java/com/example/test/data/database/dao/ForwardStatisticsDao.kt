package com.example.test.data.database.dao

import androidx.room.*
import com.example.test.data.database.entity.ForwardStatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardStatisticsDao {

    @Query("SELECT * FROM forward_statistics ORDER BY date DESC")
    fun getAllStatistics(): Flow<List<ForwardStatisticsEntity>>

    @Query("SELECT * FROM forward_statistics WHERE date = :date")
    suspend fun getStatisticsByDate(date: String): ForwardStatisticsEntity?

    @Query("SELECT * FROM forward_statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getStatisticsByDateRange(startDate: String, endDate: String): List<ForwardStatisticsEntity>

    @Query("SELECT * FROM forward_statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getStatisticsByDateRangeFlow(startDate: String, endDate: String): Flow<List<ForwardStatisticsEntity>>

    @Query("SELECT * FROM forward_statistics ORDER BY date DESC LIMIT :limit")
    fun getRecentStatistics(limit: Int): Flow<List<ForwardStatisticsEntity>>

    @Query("SELECT SUM(totalReceived) FROM forward_statistics WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalReceivedInRange(startDate: String, endDate: String): Int?

    @Query("SELECT SUM(totalForwarded) FROM forward_statistics WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalForwardedInRange(startDate: String, endDate: String): Int?

    @Query("SELECT AVG(successRate) FROM forward_statistics WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageSuccessRateInRange(startDate: String, endDate: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStatistics(statistics: ForwardStatisticsEntity)

    @Insert
    suspend fun insertStatistics(statistics: List<ForwardStatisticsEntity>)

    @Update
    suspend fun updateStatistics(statistics: ForwardStatisticsEntity)

    @Delete
    suspend fun deleteStatistics(statistics: ForwardStatisticsEntity)

    @Query("DELETE FROM forward_statistics WHERE date < :date")
    suspend fun deleteOldStatistics(date: String)

    @Query("DELETE FROM forward_statistics")
    suspend fun deleteAllStatistics()
} 