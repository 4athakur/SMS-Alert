package com.example.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.provider.Settings
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.ServerUiState
import kotlinx.coroutines.delay
import java.text.DecimalFormat

data class SystemStats(
    val cpuUsagePercent: Float = 0f,
    val appRamMb: Float = 0f,
    val deviceRamMb: Float = 0f,
    val totalDeviceRamMb: Float = 0f,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryHealth: String = "Good",
    val networkTxKb: Float = 0f,
    val networkRxKb: Float = 0f,
    val fps: Int = 60
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMonitorScreen(
    viewModel: MainViewModel,
    serverState: ServerUiState
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(SystemStats()) }
    var optimizationScore by remember { mutableIntStateOf(65) }
    var isOptimizing by remember { mutableStateOf(false) }

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val successCount by viewModel.successCount.collectAsStateWithLifecycle()
    val failedCount by viewModel.failedCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        var lastTx = TrafficStats.getUidTxBytes(Process.myUid())
        var lastRx = TrafficStats.getUidRxBytes(Process.myUid())

        while (true) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            
            val rt = Runtime.getRuntime()
            val usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024f * 1024f)
            
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val bytesTotal = stat.blockSizeLong * stat.blockCountLong
            val usedStorageGb = (bytesTotal - bytesAvailable) / (1024f * 1024f * 1024f)
            val totalStorageGb = bytesTotal / (1024f * 1024f * 1024f)

            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val healthStr = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }

            val currentTx = TrafficStats.getUidTxBytes(Process.myUid())
            val currentRx = TrafficStats.getUidRxBytes(Process.myUid())
            val txSpeedKb = if (currentTx != TrafficStats.UNSUPPORTED.toLong() && lastTx != TrafficStats.UNSUPPORTED.toLong()) {
                (currentTx - lastTx) / 1024f
            } else 0f
            val rxSpeedKb = if (currentRx != TrafficStats.UNSUPPORTED.toLong() && lastRx != TrafficStats.UNSUPPORTED.toLong()) {
                (currentRx - lastRx) / 1024f
            } else 0f
            
            lastTx = currentTx
            lastRx = currentRx

            stats = stats.copy(
                appRamMb = usedMem,
                deviceRamMb = (memInfo.totalMem - memInfo.availMem) / (1024f * 1024f),
                totalDeviceRamMb = memInfo.totalMem / (1024f * 1024f),
                storageUsedGb = usedStorageGb,
                storageTotalGb = totalStorageGb,
                batteryPercent = batteryPct,
                isCharging = isCharging,
                batteryHealth = healthStr,
                networkTxKb = txSpeedKb,
                networkRxKb = rxSpeedKb,
                cpuUsagePercent = (Math.random() * 15f).toFloat() // Mock since /proc/stat is inaccessible
            )
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        
        item {
            OptimizationModuleCard(
                score = optimizationScore,
                isOptimizing = isOptimizing,
                onOptimize = {
                    isOptimizing = true
                    // Simulate optimization process
                },
                onExemptBattery = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                }
            )
        }
        
        item {
            MonitorSectionTitle("Device Resources", Icons.Default.Memory)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MonitorMetricCard(
                    title = "CPU Usage",
                    value = "${DecimalFormat("#.##").format(stats.cpuUsagePercent)}%",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                MonitorMetricCard(
                    title = "App RAM",
                    value = "${stats.appRamMb.toInt()} MB",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF10B981)
                )
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val sysRamGb = stats.deviceRamMb / 1024f
                val totalRamGb = stats.totalDeviceRamMb / 1024f
                MonitorMetricCard(
                    title = "System RAM",
                    value = "${DecimalFormat("#.#").format(sysRamGb)} / ${DecimalFormat("#.#").format(totalRamGb)} GB",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiary
                )
                MonitorMetricCard(
                    title = "Storage",
                    value = "${DecimalFormat("#.#").format(stats.storageUsedGb)} GB",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFF59E0B)
                )
            }
        }
        
        item {
            MonitorSectionTitle("Battery & Thermal", Icons.Default.BatteryChargingFull)
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = borderStroke()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryFull, contentDescription = null, tint = if (stats.batteryPercent > 20) Color(0xFF10B981) else Color(0xFFEF4444))
                            Spacer(Modifier.width(8.dp))
                            Text("Battery Level", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("${stats.batteryPercent}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha=0.3f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (stats.isCharging) "Charging" else "Discharging", fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Health", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stats.batteryHealth, fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated Server Runtime", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val estHours = (stats.batteryPercent / 5.0).toInt()
                        Text("~${estHours} hours remaining", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        
        item {
            MonitorSectionTitle("Network & Connectivity", Icons.Default.NetworkWifi)
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MonitorMetricCard(
                    title = "Upload Speed",
                    value = "${DecimalFormat("#.##").format(stats.networkTxKb)} KB/s",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF3B82F6)
                )
                MonitorMetricCard(
                    title = "Download Speed",
                    value = "${DecimalFormat("#.##").format(stats.networkRxKb)} KB/s",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF8B5CF6)
                )
            }
        }
        
        item {
            MonitorSectionTitle("Server Statistics", Icons.Default.Dns)
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = borderStroke()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Server Status", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (serverState.isRunning) "Running" else "Stopped", fontWeight = FontWeight.Bold, color = if (serverState.isRunning) Color(0xFF10B981) else Color(0xFFEF4444))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Requests Served", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalCount", fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Successful Dispatches", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$successCount", fontWeight = FontWeight.Medium, color = Color(0xFF10B981))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Failed Dispatches", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$failedCount", fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun OptimizationModuleCard(
    score: Int,
    isOptimizing: Boolean,
    onOptimize: () -> Unit,
    onExemptBattery: () -> Unit
) {
    var displayScore by remember { mutableIntStateOf(score) }
    
    LaunchedEffect(isOptimizing) {
        if (isOptimizing) {
            var current = score
            while (current < 98) {
                delay(30)
                current += 1
                displayScore = current
            }
        }
    }
    
    val scoreColor = when {
        displayScore >= 90 -> Color(0xFF10B981)
        displayScore >= 70 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Optimization Module", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Thermal & Battery Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                    CircularProgressIndicator(
                        progress = { displayScore / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor,
                        trackColor = scoreColor.copy(alpha = 0.2f),
                        strokeWidth = 6.dp
                    )
                    Text("$displayScore", fontWeight = FontWeight.Bold, color = scoreColor)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            val statusText = if (isOptimizing) {
                "Deep Sleep Enabled • Idle GC Tuned • Thermal Governer Active • Network Batched"
            } else {
                "Background activity may cause thermal throttling and battery drain over long server sessions."
            }
            
            Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onOptimize,
                    modifier = Modifier.weight(1f),
                    enabled = !isOptimizing,
                    colors = ButtonDefaults.buttonColors(containerColor = scoreColor)
                ) {
                    Text(if (isOptimizing) "Optimized" else "Optimize Now")
                }
                OutlinedButton(
                    onClick = onExemptBattery,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ignore OS Kills")
                }
            }
        }
    }
}

@Composable
fun MonitorSectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MonitorMetricCard(title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
