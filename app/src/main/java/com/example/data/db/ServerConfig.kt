package com.example.data.db

import android.content.Context
import android.content.SharedPreferences

class ServerConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sms_gateway_prefs", Context.MODE_PRIVATE)

    var port: Int
        get() = prefs.getInt(KEY_PORT, 8080)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    var autoStartOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var rateLimitPerMinute: Int
        get() = prefs.getInt(KEY_RATE_LIMIT, 30)
        set(value) = prefs.edit().putInt(KEY_RATE_LIMIT, value).apply()

    var requireApiKey: Boolean
        get() = prefs.getBoolean(KEY_REQUIRE_API_KEY, true)
        set(value) = prefs.edit().putBoolean(KEY_REQUIRE_API_KEY, value).apply()

    var defaultSimSlot: Int
        get() = prefs.getInt(KEY_DEFAULT_SIM, 0)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_SIM, value).apply()

    var cloudSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_SYNC, value).apply()

    var activeApiKey: String
        get() = prefs.getString(KEY_ACTIVE_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_API_KEY, value).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "DARK") ?: "DARK"
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var ngrokToken: String
        get() = prefs.getString(KEY_NGROK_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NGROK_TOKEN, value).apply()

    var ngrokUrl: String
        get() = prefs.getString(KEY_NGROK_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NGROK_URL, value).apply()

    var showServerNotification: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SERVER_NOTIFICATION, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SERVER_NOTIFICATION, value).apply()

    companion object {
        private const val KEY_PORT = "server_port"
        private const val KEY_AUTO_START = "auto_start_boot"
        private const val KEY_RATE_LIMIT = "rate_limit"
        private const val KEY_REQUIRE_API_KEY = "require_api_key"
        private const val KEY_DEFAULT_SIM = "default_sim"
        private const val KEY_CLOUD_SYNC = "cloud_sync"
        private const val KEY_ACTIVE_API_KEY = "active_api_key"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_NGROK_TOKEN = "ngrok_token"
        private const val KEY_NGROK_URL = "ngrok_url"
        private const val KEY_SHOW_SERVER_NOTIFICATION = "show_server_notification"
    }
}
