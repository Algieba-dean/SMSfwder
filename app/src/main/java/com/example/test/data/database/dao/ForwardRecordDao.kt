package com.example.test.data.database.dao

import androidx.room.*
import com.example.test.data.database.entity.ForwardRecordEntity
import com.example.test.domain.model.ForwardStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardRecordDao {

    @Query("SELECT * FROM forward_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ForwardRecordEntity>>

    @Query("SELECT * FROM forward_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<ForwardRecordEntity>>

    @Query("SELECT * FROM forward_records WHERE id = :id")
    suspend fun getRecordById(id: Long): ForwardRecordEntity?

    @Query("SELECT * FROM forward_records WHERE status = :status ORDER BY timestamp DESC")
    fun getRecordsByStatus(status: ForwardStatus): Flow<List<ForwardRecordEntity>>

    @Query("SELECT * FROM forward_records WHERE smsId = :smsId")
    suspend fun getRecordsBySmsId(smsId: Long): List<ForwardRecordEntity>

    @Query("SELECT * FROM forward_records WHERE DATE(timestamp/1000, 'unixepoch') = :date ORDER BY timestamp DESC")
    suspend fun getRecordsByDate(date: String): List<ForwardRecordEntity>

    @Query("SELECT COUNT(*) FROM forward_records WHERE DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodayRecordCount(): Int

    @Query("SELECT COUNT(*) FROM forward_records WHERE status = :status AND DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodayRecordCountByStatus(status: ForwardStatus): Int

    @Query("SELECT AVG(processingTime) FROM forward_records WHERE status = 'SUCCESS' AND DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodayAverageProcessingTime(): Double?

    @Insert
    suspend fun insertRecord(record: ForwardRecordEntity): Long

    @Insert
    suspend fun insertRecords(records: List<ForwardRecordEntity>)

    @Update
    suspend fun updateRecord(record: ForwardRecordEntity)

    @Delete
    suspend fun deleteRecord(record: ForwardRecordEntity)

    @Query("DELETE FROM forward_records WHERE timestamp < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long)

    @Query("UPDATE forward_records SET status = :status, errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateRecordStatus(id: Long, status: ForwardStatus, errorMessage: String?)
    
    // SIM卡相关查询方法
    @Query("SELECT * FROM forward_records WHERE simSlot = :simSlot ORDER BY timestamp DESC")
    fun getRecordsBySimSlot(simSlot: String): Flow<List<ForwardRecordEntity>>
    
    @Query("SELECT * FROM forward_records WHERE simOperator = :simOperator ORDER BY timestamp DESC")
    fun getRecordsBySimOperator(simOperator: String): Flow<List<ForwardRecordEntity>>
    
    @Query("SELECT * FROM forward_records WHERE simSlot = :simSlot AND simOperator = :simOperator ORDER BY timestamp DESC")
    fun getRecordsBySimSlotAndOperator(simSlot: String, simOperator: String): Flow<List<ForwardRecordEntity>>
    
    @Query("SELECT DISTINCT simSlot FROM forward_records WHERE simSlot IS NOT NULL ORDER BY simSlot")
    suspend fun getDistinctSimSlots(): List<String>
    
    @Query("SELECT DISTINCT simOperator FROM forward_records WHERE simOperator IS NOT NULL ORDER BY simOperator")
    suspend fun getDistinctSimOperators(): List<String>
    
    @Query("SELECT COUNT(*) FROM forward_records WHERE simSlot = :simSlot AND DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodayRecordCountBySimSlot(simSlot: String): Int
    
    @Query("SELECT COUNT(*) FROM forward_records WHERE simSlot = :simSlot AND status = :status AND DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodayRecordCountBySimSlotAndStatus(simSlot: String, status: ForwardStatus): Int
} 