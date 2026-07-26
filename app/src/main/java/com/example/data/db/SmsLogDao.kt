package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SmsLog>>

    @Query("SELECT * FROM sms_logs WHERE status = :status ORDER BY timestamp DESC")
    fun getLogsByStatus(status: String): Flow<List<SmsLog>>

    @Query("SELECT * FROM sms_logs WHERE phoneNumber LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<SmsLog>>

    @Query("SELECT * FROM sms_logs WHERE messageId = :messageId LIMIT 1")
    suspend fun getLogByMessageId(messageId: String): SmsLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmsLog): Long

    @Query("UPDATE sms_logs SET status = :status, errorMessage = :errorMessage WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: String, errorMessage: String? = null)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM sms_logs")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE status IN ('SENT', 'DELIVERED')")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE timestamp >= :startOfDayTimestamp")
    fun getTodayCount(startOfDayTimestamp: Long): Flow<Int>
}
