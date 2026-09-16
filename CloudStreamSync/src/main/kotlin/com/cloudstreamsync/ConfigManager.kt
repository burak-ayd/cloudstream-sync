package com.cloudstreamsync

import android.content.Context
import android.content.SharedPreferences

object ConfigManager {
    private const val PREFS_NAME = "CloudStreamSyncConfig"
    private const val KEY_SUPABASE_URL = "supabase_url"
    private const val KEY_SUPABASE_KEY = "supabase_key"
    private const val KEY_SUPABASE_TABLE = "supabase_table"
    private const val KEY_SUPABASE_USER = "supabase_user"
    
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
    
    fun clearConfig(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
