package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.DynamicUiConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class DynamicUiManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(DynamicUiConfig::class.java)

    private val prefs = context.getSharedPreferences("dynamic_ui_prefs", Context.MODE_PRIVATE)

    private val _uiConfig = MutableStateFlow(loadCachedConfig())
    val uiConfig: StateFlow<DynamicUiConfig> = _uiConfig.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow(prefs.getLong("last_synced_time", 0L))
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    companion object {
        private const val TAG = "DynamicUiManager"
        private const val CONFIG_URL = "https://raw.githubusercontent.com/ARIF683/Hardware/main/ui_config.json"
        private const val PREF_KEY_CONFIG = "cached_ui_config"
    }

    private fun loadCachedConfig(): DynamicUiConfig {
        val cached = prefs.getString(PREF_KEY_CONFIG, null)
        if (!cached.isNullOrEmpty()) {
            try {
                adapter.fromJson(cached)?.let { return it }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing cached UI config", e)
            }
        }
        return DynamicUiConfig()
    }

    suspend fun refreshConfig(): Boolean = withContext(Dispatchers.IO) {
        _isRefreshing.value = true
        try {
            val request = Request.Builder()
                .url(CONFIG_URL)
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val parsed = adapter.fromJson(body)
                        if (parsed != null) {
                            val now = System.currentTimeMillis()
                            prefs.edit()
                                .putString(PREF_KEY_CONFIG, body)
                                .putLong("last_synced_time", now)
                                .apply()
                            _lastSyncedTime.value = now
                            _uiConfig.value = parsed
                            Log.d(TAG, "Successfully refreshed dynamic UI config from GitHub: ${parsed.theme.storeTitle}")
                            return@withContext true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch dynamic UI config (offline or network error): ${e.message}")
        } finally {
            _isRefreshing.value = false
        }
        false
    }
}
