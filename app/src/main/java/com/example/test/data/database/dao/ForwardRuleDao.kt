package com.example.test.data.database.dao

import androidx.room.*
import com.example.test.data.database.entity.ForwardRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardRuleDao {

    @Query("SELECT * FROM forward_rules ORDER BY priority DESC, createdAt DESC")
    fun getAllRules(): Flow<List<ForwardRuleEntity>>

    @Query("SELECT * FROM forward_rules ORDER BY priority DESC, createdAt DESC")
    suspend fun getAllRulesSync(): List<ForwardRuleEntity>

    @Query("SELECT * FROM forward_rules WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledRules(): List<ForwardRuleEntity>

    @Query("SELECT * FROM forward_rules WHERE isEnabled = 1 ORDER BY priority DESC")
    fun getEnabledRulesFlow(): Flow<List<ForwardRuleEntity>>

    @Query("SELECT * FROM forward_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): ForwardRuleEntity?

    @Query("SELECT * FROM forward_rules WHERE name = :name")
    suspend fun getRuleByName(name: String): ForwardRuleEntity?

    @Insert
    suspend fun insertRule(rule: ForwardRuleEntity): Long

    @Insert
    suspend fun insertRules(rules: List<ForwardRuleEntity>)

    @Update
    suspend fun updateRule(rule: ForwardRuleEntity)

    @Delete
    suspend fun deleteRule(rule: ForwardRuleEntity)

    @Query("UPDATE forward_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun toggleRule(id: Long, enabled: Boolean)

    @Query("UPDATE forward_rules SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: Int)

    @Query("DELETE FROM forward_rules")
    suspend fun deleteAllRules()
} 