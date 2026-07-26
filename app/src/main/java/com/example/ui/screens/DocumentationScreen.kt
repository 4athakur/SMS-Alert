package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.ServerUiState
import com.example.ui.theme.Blue400
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSlate400
import com.example.ui.theme.TextWhite

@Composable
fun DocumentationScreen(
    viewModel: MainViewModel,
    serverState: ServerUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "API Documentation",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Integrate seamlessly with the SMS Gateway Server. All endpoints are accessible when the server is running on the local network.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSlate400
            )
        }

        item {
            ApiSection(
                title = "Authentication",
                description = "All requests must include your API Key in the headers. You can pass the key using one of the following headers:",
                code = "X-API-Key: ${serverState.activeApiKey}\nAuthorization: Bearer ${serverState.activeApiKey}"
            )
        }

        item {
            ApiEndpoint(
                method = "POST",
                path = "/send-sms",
                description = "Send a new SMS message. Both phone_number and message are required. Optional sim_slot (0 or 1).",
                requestBody = """
{
  "phone_number": "+1234567890",
  "message": "Hello World",
  "sim_slot": 0
}
                """.trimIndent(),
                responseBody = """
{
  "success": true,
  "message_id": "msg_1234567890",
  "phone_number": "+1234567890",
  "status": "SMS Queued/Sent",
  "sim_slot": 0,
  "processing_time_ms": 42
}
                """.trimIndent()
            )
        }

        item {
            ApiEndpoint(
                method = "GET",
                path = "/status",
                description = "Get the current status and uptime of the SMS server.",
                requestBody = null,
                responseBody = """
{
  "success": true,
  "status": "ONLINE",
  "server_ip": "192.168.1.100",
  "port": 8080,
  "network_type": "Wi-Fi",
  "uptime_seconds": 3600,
  "api_endpoint": "http://192.168.1.100:8080/send-sms"
}
                """.trimIndent()
            )
        }

        item {
            ApiEndpoint(
                method = "GET",
                path = "/sim-info",
                description = "List all available SIM cards and their slots on the device.",
                requestBody = null,
                responseBody = """
{
  "success": true,
  "sim_count": 2,
  "sim_cards": [
    {
      "slot_index": 0,
      "carrier_name": "Carrier A",
      "display_name": "Carrier A",
      "subscription_id": 1,
      "phone_number": "+1234567890"
    }
  ]
}
                """.trimIndent()
            )
        }
        
        item {
            ApiSection(
                title = "Rate Limits & Errors",
                description = "The API enforces rate limiting based on your settings. Exceeding limits will return a 429 status code.",
                code = "400 Bad Request - Missing required fields or invalid JSON\n401 Unauthorized - Missing or invalid API key\n405 Method Not Allowed - Only POST is allowed for /send-sms\n429 Too Many Requests - Rate limit exceeded\n500 Internal Server Error - SMS dispatch failed"
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ApiSection(title: String, description: String, code: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSlate400,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    color = Blue400,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ApiEndpoint(
    method: String,
    path: String,
    description: String,
    requestBody: String?,
    responseBody: String
) {
    val methodColor = if (method == "POST") Color(0xFF10B981) else Color(0xFF3B82F6)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Surface(
                    color = methodColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = method,
                        color = methodColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = path,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSlate400,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (requestBody != null) {
                Text(
                    text = "Request Body",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextWhite,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = requestBody,
                        color = TextSlate400,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Response",
                style = MaterialTheme.typography.labelMedium,
                color = TextWhite,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = responseBody,
                    color = TextSlate400,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}
