package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var autoStart by remember { mutableStateOf(viewModel.serverState.value.port == 8080) } // initial check
    var portText by remember { mutableStateOf(viewModel.serverState.value.port.toString()) }
    var rateLimitText by remember { mutableStateOf("30") }
    var cloudSync by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Gateway Preferences",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Boot Auto Start
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = "Auto Start on Device Boot", fontWeight = FontWeight.Bold)
                        Text(text = "Start HTTP server automatically when phone reboots", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Switch(
                    checked = autoStart,
                    onCheckedChange = {
                        autoStart = it
                        viewModel.toggleAutoStartOnBoot(it)
                    },
                    modifier = Modifier.testTag("auto_start_switch")
                )
            }
        }

        // Port & Rate Limit Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Server Network Config", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("HTTP Port (default 8080)") },
                    modifier = Modifier.fillMaxWidth().testTag("port_config_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rateLimitText,
                    onValueChange = { rateLimitText = it },
                    label = { Text("Rate Limit (Requests / min)") },
                    modifier = Modifier.fillMaxWidth().testTag("rate_limit_input"),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val newPort = portText.toIntOrNull() ?: 8080
                        val newLimit = rateLimitText.toIntOrNull() ?: 30
                        viewModel.updatePort(newPort)
                        viewModel.updateRateLimit(newLimit)
                        Toast.makeText(context, "Server Config Saved! Restart server to apply new port.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_config_btn")
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Save Configuration")
                }
            }
        }

        // Battery Optimization Helper Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Battery Optimization Exemption", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Text(
                    text = "Android OS may kill background HTTP server when screen is locked. Disable battery optimization for 24/7 continuous operation.",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("battery_exemption_btn")
                ) {
                    Text("Open Battery Saver Settings")
                }
            }
        }

        // Future Cloud SaaS Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = "Multi-Device Cloud SaaS Sync", fontWeight = FontWeight.Bold)
                        Text(text = "Sync device telemetry & remote configuration", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Switch(
                    checked = cloudSync,
                    onCheckedChange = { cloudSync = it },
                    modifier = Modifier.testTag("cloud_sync_switch")
                )
            }
        }
    }
}
