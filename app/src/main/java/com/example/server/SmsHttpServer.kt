package com.example.server

import android.content.Context
import com.example.data.db.SmsLog
import com.example.data.repository.SmsGatewayRepository
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SmsHttpServer(
    private val context: Context,
    private val port: Int,
    private val repository: SmsGatewayRepository
) : NanoHTTPD(port) {

    @Volatile
    var isRunning = false
        private set

    val startTime = System.currentTimeMillis()
    private val requestTimestamps = mutableListOf<Long>()

    override fun start() {
        if (isRunning) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                super@SmsHttpServer.start(SOCKET_READ_TIMEOUT, false)
                isRunning = true
            } catch (e: Exception) {
                e.printStackTrace()
                isRunning = false
            }
        }
    }

    override fun stop() {
        super.stop()
        isRunning = false
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val clientIp = session.remoteIpAddress ?: "127.0.0.1"

        return try {
            when (uri) {
                "/send-sms" -> handleSendSms(session, clientIp)
                "/status" -> handleStatus(session)
                "/sim-info" -> handleSimInfo(session)
                "/logs" -> handleLogs(session)
                "/ping" -> handlePing()
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", JSONObject().put("error", "Not Found").toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", JSONObject().put("error", "Internal error: ${e.message}").toString())
        }
    }

    private fun handleSendSms(session: IHTTPSession, clientIp: String): Response {
        val startTimeMs = System.currentTimeMillis()
        if (session.method != Method.POST) {
            return sendJsonResponse(405, JSONObject().apply {
                put("success", false)
                put("error", "Method Not Allowed. Use POST")
            })
        }

        val headers = session.headers
        val apiKeyHeader = headers["x-api-key"] ?: headers["authorization"]?.removePrefix("Bearer ")?.trim() ?: ""

        val isValidKey = runBlocking { repository.validateApiKey(apiKeyHeader) }
        if (!isValidKey) {
            return sendJsonResponse(401, JSONObject().apply {
                put("success", false)
                put("error", "Unauthorized: Invalid or missing X-API-Key")
            })
        }

        // Rate Limit Check
        val limitPerMin = repository.config.rateLimitPerMinute
        val now = System.currentTimeMillis()
        synchronized(requestTimestamps) {
            requestTimestamps.removeAll { it < now - 60000 }
            if (requestTimestamps.size >= limitPerMin) {
                return sendJsonResponse(429, JSONObject().apply {
                    put("success", false)
                    put("error", "Too Many Requests: Rate limit of $limitPerMin req/min exceeded")
                })
            }
            requestTimestamps.add(now)
        }

        val map = HashMap<String, String>()
        session.parseBody(map)
        val requestBody = map["postData"] ?: ""

        val jsonBody = try {
            JSONObject(requestBody)
        } catch (e: Exception) {
            null
        }

        if (jsonBody == null) {
            return sendJsonResponse(400, JSONObject().apply {
                put("success", false)
                put("error", "Invalid JSON payload")
            })
        }

        val phoneNumber = jsonBody.optString("phone_number", jsonBody.optString("phoneNumber", "")).trim()
        val message = jsonBody.optString("message", "").trim()
        val simSlot = jsonBody.optInt("sim_slot", jsonBody.optInt("simSlot", repository.config.defaultSimSlot))

        if (phoneNumber.isBlank() || message.isBlank()) {
            return sendJsonResponse(400, JSONObject().apply {
                put("success", false)
                put("error", "Bad Request: Both 'phone_number' and 'message' are required")
            })
        }

        val messageId = "msg_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val dispatched = SmsDispatcher.sendSms(context, phoneNumber, message, messageId, simSlot)
        val processingTimeMs = System.currentTimeMillis() - startTimeMs
        val logStatus = if (dispatched) "PENDING" else "FAILED"
        val errorMessage = if (!dispatched) "Failed to dispatch SMS via SmsManager (check permissions/SIM)" else null

        CoroutineScope(Dispatchers.IO).launch {
            repository.logSmsRequest(
                SmsLog(
                    messageId = messageId,
                    phoneNumber = phoneNumber,
                    message = message,
                    status = logStatus,
                    simSlot = simSlot,
                    clientIp = clientIp,
                    errorMessage = errorMessage,
                    processingTimeMs = processingTimeMs,
                    timestamp = startTimeMs
                )
            )
        }

        return if (dispatched) {
            sendJsonResponse(200, JSONObject().apply {
                put("success", true)
                put("message_id", messageId)
                put("phone_number", phoneNumber)
                put("status", "SMS Queued/Sent")
                put("sim_slot", simSlot)
                put("processing_time_ms", processingTimeMs)
            })
        } else {
            sendJsonResponse(500, JSONObject().apply {
                put("success", false)
                put("error", errorMessage ?: "Internal error sending SMS")
                put("processing_time_ms", processingTimeMs)
            })
        }
    }

    private fun handleStatus(session: IHTTPSession): Response {
        val ip = NetworkUtils.getLocalIpAddress()
        val network = NetworkUtils.getNetworkType(context)
        val uptimeSec = (System.currentTimeMillis() - startTime) / 1000

        val response = JSONObject().apply {
            put("success", true)
            put("status", if (isRunning) "ONLINE" else "OFFLINE")
            put("server_ip", ip)
            put("port", port)
            put("network_type", network)
            put("uptime_seconds", uptimeSec)
            put("api_endpoint", "http://$ip:$port/send-sms")
        }
        return sendJsonResponse(200, response)
    }

    private fun handleSimInfo(session: IHTTPSession): Response {
        val sims = SmsDispatcher.getAvailableSimCards(context)
        val jsonArray = JSONArray()
        sims.forEach { sim ->
            jsonArray.put(JSONObject().apply {
                put("slot_index", sim.slotIndex)
                put("carrier_name", sim.carrierName)
                put("display_name", sim.displayName)
                put("subscription_id", sim.subscriptionId)
                put("phone_number", sim.phoneNumber ?: "Unknown")
            })
        }

        val response = JSONObject().apply {
            put("success", true)
            put("sim_count", sims.size)
            put("sim_cards", jsonArray)
        }
        return sendJsonResponse(200, response)
    }

    private fun handleLogs(session: IHTTPSession): Response {
        val headers = session.headers
        val apiKeyHeader = headers["x-api-key"] ?: headers["authorization"]?.removePrefix("Bearer ")?.trim() ?: ""

        val isValidKey = runBlocking { repository.validateApiKey(apiKeyHeader) }
        if (!isValidKey) {
            return sendJsonResponse(401, JSONObject().apply {
                put("success", false)
                put("error", "Unauthorized")
            })
        }

        val response = JSONObject().apply {
            put("success", true)
            put("message", "Use the Android App UI to view full log details")
        }
        return sendJsonResponse(200, response)
    }

    private fun handlePing(): Response {
        val response = JSONObject().apply {
            put("success", true)
            put("message", "pong")
            put("timestamp", System.currentTimeMillis())
        }
        return sendJsonResponse(200, response)
    }

    private fun sendJsonResponse(statusCode: Int, jsonResponse: JSONObject): Response {
        val status = when (statusCode) {
            200 -> Response.Status.OK
            400 -> Response.Status.BAD_REQUEST
            401 -> Response.Status.UNAUTHORIZED
            405 -> Response.Status.METHOD_NOT_ALLOWED
            429 -> Response.Status.TOO_MANY_REQUESTS
            else -> Response.Status.INTERNAL_ERROR
        }
        val response = newFixedLengthResponse(status, "application/json", jsonResponse.toString(2))
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key, Authorization")
        return response
    }
}
