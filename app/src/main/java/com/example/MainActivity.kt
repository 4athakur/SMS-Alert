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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import com.example.data.repository.SmsGatewayRepository
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

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
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(appThemeStr = appTheme) {
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
    LOGS("SMS Logs", Icons.Default.ListAlt, "tab_logs"),
    SIM_NETWORK("SIM & Network", Icons.Default.SimCard, "tab_sim_network"),
    SETTINGS("Settings", Icons.Default.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsGatewayApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDocumentation by remember { mutableStateOf(false) }

    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val successCount by viewModel.successCount.collectAsStateWithLifecycle()
    val failedCount by viewModel.failedCount.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val apiKeys by viewModel.apiKeys.collectAsStateWithLifecycle()
    val simCards by viewModel.simCards.collectAsStateWithLifecycle()
    val selectedLog by viewModel.selectedLogForDetail.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshState(context)
    }

    val currentTab = NavigationTab.entries[selectedTab]

    var isSidebarOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = isSidebarOpen || showDocumentation) {
        if (isSidebarOpen) {
            isSidebarOpen = false
        } else if (showDocumentation) {
            showDocumentation = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .windowInsetsPadding(WindowInsets.statusBars),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "SMS GATEWAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = if (showDocumentation) "Documentation" else currentTab.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusColor = if (serverState.isRunning) Emerald500 else Rose500
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            text = if (serverState.isRunning) "SERVER LIVE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { isSidebarOpen = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            },
            bottomBar = {
                if (!showDocumentation) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp
                            ) {
                                NavigationTab.entries.forEachIndexed { index, tab ->
                                    NavigationBarItem(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                                        label = { Text(tab.title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                                        modifier = Modifier.testTag(tab.tag),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                }
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
                if (showDocumentation) {
                    DocumentationScreen(
                        viewModel = viewModel,
                        serverState = serverState,
                        onBack = { showDocumentation = false }
                    )
                } else {
                    when (currentTab) {
                        NavigationTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            serverState = serverState,
                            totalCount = totalCount,
                            successCount = successCount,
                            failedCount = failedCount
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
        
        // Right Sidebar Overlay
        if (isSidebarOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { isSidebarOpen = false }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isSidebarOpen,
            enter = androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* Consume clicks */ },
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                tonalElevation = 4.dp
            ) {
                Column {
                    Spacer(Modifier.height(32.dp).windowInsetsPadding(WindowInsets.statusBars))
                    Text(
                        "Gateway Menu",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Description, contentDescription = null) },
                        label = { Text("Documentation") },
                        selected = showDocumentation,
                        onClick = {
                            showDocumentation = true
                            isSidebarOpen = false
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    listOf("LIGHT" to "Light Mode", "DARK" to "Dark Mode", "AMOLED" to "AMOLED Mode").forEach { (themeCode, themeName) ->
                        NavigationDrawerItem(
                            icon = {
                                val icon = when (themeCode) {
                                    "LIGHT" -> Icons.Default.WbSunny
                                    "DARK" -> Icons.Default.DarkMode
                                    else -> Icons.Default.Brightness3
                                }
                                Icon(icon, contentDescription = null)
                            },
                            label = { Text(themeName) },
                            selected = appTheme == themeCode,
                            onClick = {
                                viewModel.setTheme(themeCode)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    }
}
