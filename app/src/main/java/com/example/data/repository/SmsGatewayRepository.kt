package com.example.data.repository

import android.content.Context
import com.example.data.db.ApiKeyEntity
import com.example.data.db.AppDatabase
import com.example.data.db.ServerConfigManager
import com.example.data.db.SmsLog
import com.example.server.NetworkUtils
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SmsGatewayRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    val config = ServerConfigManager(context)

    val allLogs: Flow<List<SmsLog>> = db.smsLogDao().getAllLogs()
    val totalCount: Flow<Int> = db.smsLogDao().getTotalCount()
    val successCount: Flow<Int> = db.smsLogDao().getSuccessCount()
    val failedCount: Flow<Int> = db.smsLogDao().getFailedCount()
    val apiKeys: Flow<List<ApiKeyEntity>> = db.apiKeyDao().getAllKeys()

    suspend fun getOrCreateDefaultApiKey(): String {
        val existing = config.activeApiKey
        if (existing.isNotEmpty()) {
            val keyEntity = db.apiKeyDao().getValidKey(existing)
            if (keyEntity != null) return existing
        }

        val newKey = "gw_" + UUID.randomUUID().toString().replace("-", "").take(20)
        db.apiKeyDao().insertKey(
            ApiKeyEntity(
                key = newKey,
                label = "Primary API Key",
                isActive = true
            )
        )
        config.activeApiKey = newKey
        return newKey
    }

    suspend fun generateNewApiKey(label: String = "Generated Key"): String {
        val newKey = "gw_" + UUID.randomUUID().toString().replace("-", "").take(20)
        db.apiKeyDao().insertKey(
            ApiKeyEntity(
                key = newKey,
                label = label,
                isActive = true
            )
        )
        config.activeApiKey = newKey
        return newKey
    }

    suspend fun setApiKeyActive(key: String, isActive: Boolean) {
        db.apiKeyDao().setKeyStatus(key, isActive)
    }

    suspend fun deleteApiKey(key: String) {
        db.apiKeyDao().deleteKey(key)
        if (config.activeApiKey == key) {
            config.activeApiKey = ""
        }
    }

    suspend fun logSmsRequest(log: SmsLog): Long {
        return db.smsLogDao().insertLog(log)
    }

    suspend fun clearLogs() {
        db.smsLogDao().clearAllLogs()
    }

    suspend fun validateApiKey(key: String): Boolean {
        if (!config.requireApiKey) return true
        if (key.isBlank()) return false
        val keyEntity = db.apiKeyDao().getValidKey(key) ?: return false
        db.apiKeyDao().recordKeyUsage(key)
        return true
    }

    fun searchLogs(query: String): Flow<List<SmsLog>> {
        return if (query.isBlank()) {
            db.smsLogDao().getAllLogs()
        } else {
            db.smsLogDao().searchLogs(query)
        }
    }

    fun exportLogsToCsv(logs: List<SmsLog>): String {
        val sb = StringBuilder()
        sb.append("ID,MessageID,PhoneNumber,Message,Status,SimSlot,ClientIP,ErrorMessage,ProcessingTimeMs,Timestamp\n")
        logs.forEach { log ->
            val escapedMessage = "\"" + log.message.replace("\"", "\"\"") + "\""
            sb.append("${log.id},${log.messageId},${log.phoneNumber},$escapedMessage,${log.status},${log.simSlot},${log.clientIp},${log.errorMessage ?: ""},${log.processingTimeMs},${log.timestamp}\n")
        }
        return sb.toString()
    }

    fun exportLogsToJson(logs: List<SmsLog>): String {
        val jsonArray = JSONArray()
        logs.forEach { log ->
            val obj = JSONObject()
            obj.put("id", log.id)
            obj.put("message_id", log.messageId)
            obj.put("phone_number", log.phoneNumber)
            obj.put("message", log.message)
            obj.put("status", log.status)
            obj.put("sim_slot", log.simSlot)
            obj.put("client_ip", log.clientIp)
            obj.put("error_message", log.errorMessage ?: "")
            obj.put("processing_time_ms", log.processingTimeMs)
            obj.put("timestamp", log.timestamp)
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun generatePythonSnippet(apiKey: String, port: Int): String {
        val ip = NetworkUtils.getLocalIpAddress()
        return """
import requests

url = "http://$ip:$port/send-sms"
headers = {
    "X-API-Key": "$apiKey",
    "Content-Type": "application/json"
}
payload = {
    "phone_number": "+919876543210",
    "message": "Hello from Python SMS Gateway!",
    "sim_slot": 0
}

response = requests.post(url, headers=headers, json=payload)
print("Status Code:", response.status_code)
print("Response:", response.json())
""".trimIndent()
    }

    fun generateCurlSnippet(apiKey: String, port: Int): String {
        val ip = NetworkUtils.getLocalIpAddress()
        return """
curl -X POST "http://$ip:$port/send-sms" \
  -H "X-API-Key: $apiKey" \
  -H "Content-Type: application/json" \
  -d '{
    "phone_number": "+919876543210",
    "message": "Alert message via cURL SMS Gateway"
  }'
""".trimIndent()
    }
}
