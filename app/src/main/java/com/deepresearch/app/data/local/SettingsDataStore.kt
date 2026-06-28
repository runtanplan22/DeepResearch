package com.deepresearch.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Settings DataStore using EncryptedSharedPreferences for secure API key storage
 * and regular SharedPreferences for other settings.
 */
class SettingsDataStore(context: Context) {

    // Encrypted storage for API keys
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "deep_research_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Regular preferences for non-sensitive settings
    private val prefs = context.getSharedPreferences("deep_research_prefs", Context.MODE_PRIVATE)

    // Observable settings as StateFlows
    private val _deepSeekBaseUrl = MutableStateFlow(getDeepSeekBaseUrl())
    val deepSeekBaseUrl: StateFlow<String> = _deepSeekBaseUrl.asStateFlow()

    private val _searxngBaseUrl = MutableStateFlow(getSearxngBaseUrl())
    val searxngBaseUrl: StateFlow<String> = _searxngBaseUrl.asStateFlow()

    private val _selectedModel = MutableStateFlow(getSelectedModel())
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    // DeepSeek settings
    fun getDeepSeekBaseUrl(): String =
        prefs.getString("deepseek_base_url", "https://api.deepseek.com") ?: "https://api.deepseek.com"

    fun setDeepSeekBaseUrl(url: String) {
        prefs.edit().putString("deepseek_base_url", url).apply()
        _deepSeekBaseUrl.value = url
    }

    fun getDeepSeekApiKey(): String =
        encryptedPrefs.getString("deepseek_api_key", "") ?: ""

    fun setDeepSeekApiKey(key: String) {
        encryptedPrefs.edit().putString("deepseek_api_key", key).apply()
    }

    fun getSelectedModel(): String =
        prefs.getString("selected_model", "deepseek-chat") ?: "deepseek-chat"

    fun setSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
        _selectedModel.value = model
    }

    // SearXNG settings
    fun getSearxngBaseUrl(): String =
        prefs.getString("searxng_base_url", "http://10.0.2.2:8888") ?: "http://10.0.2.2:8888"

    fun setSearxngBaseUrl(url: String) {
        prefs.edit().putString("searxng_base_url", url).apply()
        _searxngBaseUrl.value = url
    }

    fun getSearxngApiKey(): String =
        encryptedPrefs.getString("searxng_api_key", "") ?: ""

    fun setSearxngApiKey(key: String) {
        encryptedPrefs.edit().putString("searxng_api_key", key).apply()
    }

    // Research settings
    fun getDefaultReportLength(): String =
        prefs.getString("default_report_length", "MEDIUM") ?: "MEDIUM"

    fun setDefaultReportLength(length: String) {
        prefs.edit().putString("default_report_length", length).apply()
    }

    fun getDefaultIterationMode(): String =
        prefs.getString("default_iteration_mode", "AUTO") ?: "AUTO"

    fun setDefaultIterationMode(mode: String) {
        prefs.edit().putString("default_iteration_mode", mode).apply()
    }

    fun getManualIterationCount(): Int =
        prefs.getInt("manual_iteration_count", 5)

    fun setManualIterationCount(count: Int) {
        prefs.edit().putInt("manual_iteration_count", count).apply()
    }
}
