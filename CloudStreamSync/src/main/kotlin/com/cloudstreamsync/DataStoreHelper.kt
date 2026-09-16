package com.cloudstreamsync

import android.content.Context
import android.content.SharedPreferences
import com.cloudstreamsync.models.SyncData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ponytail: CloudStream DataStore wrapper - gerçek API entegrasyonu için genişlet
object DataStoreHelper {
    private const val PREFS_NAME = "CloudstreamDataStore"
    private const val KEY_BOOKMARKS = "bookmarked_anime_list"
    private const val KEY_WATCH_POSITIONS = "VideoDownloadManager_resumeWatching"
    private const val KEY_SEARCH_HISTORY = "search_history"
    
    private val gson = Gson()
    
    fun collectCurrentData(context: Context): SyncData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        return SyncData(
            bookmarks = getBookmarks(prefs),
            watchPositions = getWatchPositions(prefs),
            searchHistory = getSearchHistory(prefs),
            extensions = emptyList(), // ponytail: eklenti listesi için ayrı API gerek
            settings = emptyMap(),    // ponytail: ayarlar için ayrı API gerek
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun applyData(context: Context, data: SyncData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Bookmarks
        if (data.bookmarks.isNotEmpty()) {
            editor.putString(KEY_BOOKMARKS, gson.toJson(data.bookmarks))
        }
        
        // Watch positions
        if (data.watchPositions.isNotEmpty()) {
            editor.putString(KEY_WATCH_POSITIONS, gson.toJson(data.watchPositions))
        }
        
        // Search history
        if (data.searchHistory.isNotEmpty()) {
            editor.putString(KEY_SEARCH_HISTORY, gson.toJson(data.searchHistory))
        }
        
        editor.apply()
    }
    
    private fun getBookmarks(prefs: SharedPreferences): List<String> {
        return try {
            val json = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getWatchPositions(prefs: SharedPreferences): Map<String, Long> {
        return try {
            val json = prefs.getString(KEY_WATCH_POSITIONS, null) ?: return emptyMap()
            val type = object : TypeToken<Map<String, Long>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun getSearchHistory(prefs: SharedPreferences): List<String> {
        return try {
            val json = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
