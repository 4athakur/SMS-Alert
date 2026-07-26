package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY createdAt DESC")
    fun getAllKeys(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE key = :key AND isActive = 1 LIMIT 1")
    suspend fun getValidKey(key: String): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(keyEntity: ApiKeyEntity)

    @Query("UPDATE api_keys SET isActive = :isActive WHERE key = :key")
    suspend fun setKeyStatus(key: String, isActive: Boolean)

    @Query("UPDATE api_keys SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE key = :key")
    suspend fun recordKeyUsage(key: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM api_keys WHERE key = :key")
    suspend fun deleteKey(key: String)
}
