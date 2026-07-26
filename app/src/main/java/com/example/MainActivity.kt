package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SmsGatewayRepository
import com.example.ui.MainViewModel
import com.example.ui.screens.ApiKeysScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentationScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimNetworkScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = SmsGatewayRepository(applicationContext)
        val viewModel: MainViewModel by viewModels { MainViewModel.Factory(repository) }

        requestGatewayPermissions()

        setContent {
            MyApplicationTheme {
                SmsGatewayApp(viewModel = viewModel)
            }
        }
    }

    private fun requestGatewayPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}

enum class NavigationTab(
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    API_KEYS("API & Keys", Icons.Default.Code, "tab_api_keys"),
    DOCS("Documentation", Icons.Default.Description, "tab_docs"),
    LOGS("SMS Logs", Icons.Default.ListAlt, "tab_logs"),
    SIM_NETWORK("SIM & Network", Icons.Default.SimCard, "tab_sim_network"),
    SETTINGS("Settings", Icons.Default.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsGatewayApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val successCount by viewModel.successCount.collectAsStateWithLifecycle()
    val failedCount by viewModel.failedCount.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val apiKeys by viewModel.apiKeys.collectAsStateWithLifecycle()
    val simCards by viewModel.simCards.collectAsStateWithLifecycle()
    val selectedLog by viewModel.selectedLogForDetail.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshState(context)
    }

    val currentTab = NavigationTab.entries[selectedTab]

    Scaffold(
        topBar = {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = "SMS GATEWAY",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = com.example.ui.theme.Blue400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = currentTab.title,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        color = com.example.ui.theme.TextWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor = if (serverState.isRunning) com.example.ui.theme.Emerald500 else com.example.ui.theme.Rose500
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = if (serverState.isRunning) "SERVER LIVE" else "OFFLINE",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                }
            }
        },
        bottomBar = {
            androidx.compose.material3.Surface(
                color = com.example.ui.theme.BottomNavBg,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Divider(color = androidx.compose.material3.MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        NavigationTab.entries.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                                modifier = Modifier.testTag(tab.tag),
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    selectedIconColor = com.example.ui.theme.Blue400,
                                    selectedTextColor = com.example.ui.theme.Blue400,
                                    unselectedIconColor = com.example.ui.theme.TextSlate500,
                                    unselectedTextColor = com.example.ui.theme.TextSlate500,
                                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    serverState = serverState,
                    totalCount = totalCount,
                    successCount = successCount,
                    failedCount = failedCount
                )
                NavigationTab.DOCS -> DocumentationScreen(
                    viewModel = viewModel,
                    serverState = serverState
                )
                NavigationTab.API_KEYS -> ApiKeysScreen(
                    viewModel = viewModel,
                    apiKeys = apiKeys,
                    activeKey = serverState.activeApiKey
                )
                NavigationTab.LOGS -> LogsScreen(
                    viewModel = viewModel,
                    logs = logs,
                    selectedLog = selectedLog
                )
                NavigationTab.SIM_NETWORK -> SimNetworkScreen(
                    viewModel = viewModel,
                    simCards = simCards
                )
                NavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
