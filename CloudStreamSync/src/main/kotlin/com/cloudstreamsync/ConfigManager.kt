package com.cloudstreamsync

import android.content.Context
import android.content.SharedPreferences
import com.cloudstreamsync.models.SyncConfig
import com.google.gson.Gson

object ConfigManager {
    private const val PREFS_NAME = "CloudStreamSyncConfig"
    private const val KEY_SUPABASE_URL = "supabase_url"
    private const val KEY_SUPABASE_KEY = "supabase_key"
    private const val KEY_SUPABASE_TABLE = "supabase_table"
    private const val KEY_SUPABASE_USER = "supabase_user"
    private const val KEY_SYNC_CONFIG = "sync_config"
    
    private val gson = Gson()
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveConfig(context: Context, config: Map<String, String>) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_SUPABASE_URL, config["url"] ?: "")
        editor.putString(KEY_SUPABASE_KEY, config["apiKey"] ?: "")
        editor.putString(KEY_SUPABASE_TABLE, config["table"] ?: "cloudstream_sync")
        editor.putString(KEY_SUPABASE_USER, config["userId"] ?: "")
        editor.apply()
    }
    
    fun loadConfig(context: Context): Map<String, String> {
        val prefs = getPrefs(context)
        return mapOf(
            "url" to (prefs.getString(KEY_SUPABASE_URL, "") ?: ""),
            "apiKey" to (prefs.getString(KEY_SUPABASE_KEY, "") ?: ""),
            "table" to (prefs.getString(KEY_SUPABASE_TABLE, "cloudstream_sync") ?: "cloudstream_sync"),
            "userId" to (prefs.getString(KEY_SUPABASE_USER, "") ?: "")
        )
    }
    
    fun saveSyncConfig(context: Context, config: SyncConfig) {
        val json = gson.toJson(config)
        getPrefs(context).edit()
            .putString(KEY_SYNC_CONFIG, json)
            .apply()
    }
    
    fun loadSyncConfig(context: Context): SyncConfig {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_SYNC_CONFIG, null)
        
        return if (json != null) {
            try {
                gson.fromJson(json, SyncConfig::class.java)
            } catch (e: Exception) {
                SyncConfig() // Default config
            }
        } else {
            SyncConfig() // Default config
        }
    }
    
    fun clearConfig(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
