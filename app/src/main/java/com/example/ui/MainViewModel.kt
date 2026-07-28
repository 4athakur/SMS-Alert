package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.ApiKeyEntity
import com.example.data.db.SmsLog
import com.example.data.repository.SmsGatewayRepository
import com.example.server.NetworkUtils
import com.example.server.SimCardInfo
import com.example.server.SmsDispatcher
import com.example.service.SmsGatewayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServerUiState(
    val isRunning: Boolean = false,
    val ipAddress: String = "127.0.0.1",
    val port: Int = 8080,
    val networkType: String = "Unknown",
    val activeApiKey: String = "",
    val ngrokUrl: String = "",
    val ngrokToken: String = ""
)

class MainViewModel(private val repository: SmsGatewayRepository) : ViewModel() {

    private val _serverState = MutableStateFlow(ServerUiState(
        port = repository.config.port,
        activeApiKey = repository.config.activeApiKey,
        ngrokUrl = repository.config.ngrokUrl,
        ngrokToken = repository.config.ngrokToken
    ))
    val serverState: StateFlow<ServerUiState> = _serverState.asStateFlow()

    private val _appTheme = MutableStateFlow(repository.config.appTheme)
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _simCards = MutableStateFlow<List<SimCardInfo>>(emptyList())
    val simCards: StateFlow<List<SimCardInfo>> = _simCards.asStateFlow()

    private val _selectedLogForDetail = MutableStateFlow<SmsLog?>(null)
    val selectedLogForDetail: StateFlow<SmsLog?> = _selectedLogForDetail.asStateFlow()

    val logs: StateFlow<List<SmsLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalCount: StateFlow<Int> = repository.totalCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val successCount: StateFlow<Int> = repository.successCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val failedCount: StateFlow<Int> = repository.failedCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val apiKeys: StateFlow<List<ApiKeyEntity>> = repository.apiKeys.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshState()
    }

    fun refreshState(context: Context? = null) {
        viewModelScope.launch {
            val key = repository.getOrCreateDefaultApiKey()
            val ip = NetworkUtils.getLocalIpAddress()
            val net = context?.let { NetworkUtils.getNetworkType(it) } ?: "Connected"
            val port = repository.config.port

            _serverState.value = _serverState.value.copy(
                isRunning = SmsGatewayService.isRunning,
                ipAddress = ip,
                port = port,
                networkType = net,
                activeApiKey = key
            )

            context?.let { ctx ->
                _simCards.value = SmsDispatcher.getAvailableSimCards(ctx)
            }
        }
    }

    fun toggleServer(context: Context) {
        val newState = !_serverState.value.isRunning
        val intent = Intent(context, SmsGatewayService::class.java).apply {
            action = if (newState) SmsGatewayService.ACTION_START else SmsGatewayService.ACTION_STOP
        }
        if (newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
        _serverState.value = _serverState.value.copy(isRunning = newState)
    }

    fun generateNewApiKey(label: String = "New Key") {
        viewModelScope.launch {
            val newKey = repository.generateNewApiKey(label)
            _serverState.value = _serverState.value.copy(activeApiKey = newKey)
        }
    }

    fun toggleApiKeyActive(key: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.setApiKeyActive(key, isActive)
        }
    }

    fun updateNgrokConfig(context: Context, token: String, onComplete: (Boolean, String?) -> Unit) {
        repository.config.ngrokToken = token
        _serverState.value = _serverState.value.copy(
            ngrokToken = token
        )
        viewModelScope.launch {
            com.example.server.NgrokManager.start(context, repository.config.port, token) { url ->
                repository.config.ngrokUrl = url
                _serverState.value = _serverState.value.copy(ngrokUrl = url)
                onComplete(true, url)
            }
        }
    }

    fun deleteApiKey(key: String) {
        viewModelScope.launch {
            repository.deleteApiKey(key)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectLogDetail(log: SmsLog?) {
        _selectedLogForDetail.value = log
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun sendTestSms(context: Context, phoneNumber: String, message: String, simSlot: Int, onResult: (Boolean, String) -> Unit) {
        val msgId = "test_" + System.currentTimeMillis()
        val success = SmsDispatcher.sendSms(context, phoneNumber, message, msgId, simSlot)
        if (success) {
            viewModelScope.launch {
                repository.logSmsRequest(
                    SmsLog(
                        messageId = msgId,
                        phoneNumber = phoneNumber,
                        message = message,
                        status = "SENT",
                        simSlot = simSlot,
                        clientIp = "127.0.0.1 (In-App Test)"
                    )
                )
            }
            onResult(true, "Test SMS dispatched successfully!")
        } else {
            onResult(false, "Failed to send SMS. Ensure SEND_SMS permission is granted and SIM card is active.")
        }
    }

    fun getPythonSnippet(): String {
        return repository.generatePythonSnippet(_serverState.value.activeApiKey, _serverState.value.port)
    }

    fun getCurlSnippet(): String {
        return repository.generateCurlSnippet(_serverState.value.activeApiKey, _serverState.value.port)
    }

    fun exportLogsCsv(): String {
        return repository.exportLogsToCsv(logs.value)
    }

    fun exportLogsJson(): String {
        return repository.exportLogsToJson(logs.value)
    }

    fun updatePort(newPort: Int) {
        repository.config.port = newPort
        _serverState.value = _serverState.value.copy(port = newPort)
    }

    fun updateRateLimit(rateLimit: Int) {
        repository.config.rateLimitPerMinute = rateLimit
    }

    fun toggleAutoStartOnBoot(enabled: Boolean) {
        repository.config.autoStartOnBoot = enabled
    }

    fun setTheme(theme: String) {
        repository.config.appTheme = theme
        _appTheme.value = theme
    }

    class Factory(private val repository: SmsGatewayRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
