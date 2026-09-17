package com.cloudstreamsync

import android.content.Context
import com.cloudstreamsync.models.LegacySyncData
import com.cloudstreamsync.utils.SyncKeyClassifier
import com.lagradost.cloudstream3.utils.DataStoreHelper as CS3DataStore
import com.google.gson.Gson

// CloudStream DataStore wrapper - Legacy sync support
object DataStoreHelper {
    private val gson = Gson()
    private const val PREFERENCES_NAME = "rebuild_preference"
    private const val USER_PROVIDER_API = "user_custom_sites"
    
    fun collectCurrentData(context: Context): LegacySyncData {
        return LegacySyncData(
            bookmarks = getAllBookmarksFullData(),
            watchPositions = getAllWatchPositions(),
            searchHistory = getSearchHistory(context),
            extensions = getExtensions(context),
            settings = getSettings(context),
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun applyData(context: Context, data: LegacySyncData) {
        // Bookmarks import et
        var successfulBookmarks = 0
        data.bookmarks.forEach { bookmarkJson ->
            try {
                val bookmarkedData = gson.fromJson(bookmarkJson, CS3DataStore.BookmarkedData::class.java)
                if (bookmarkedData != null && bookmarkedData.id != null) {
                    CS3DataStore.setBookmarkedData(bookmarkedData.id, bookmarkedData)
                    successfulBookmarks++
                }
            } catch (e: Exception) {
                // Skip invalid entries
                android.util.Log.e("CloudStreamSync", "Bookmark import error: ${e.message}")
            }
        }
        android.util.Log.d("CloudStreamSync", "Imported $successfulBookmarks/${data.bookmarks.size} bookmarks")
        
        // Watch positions import et
        var successfulPositions = 0
        data.watchPositions.forEach { (idStr, position) ->
            try {
                val id = idStr.toIntOrNull()
                if (id != null && position > 0) {
                    CS3DataStore.setViewPos(id, position, 0L)
                    successfulPositions++
                }
            } catch (e: Exception) {
                android.util.Log.e("CloudStreamSync", "Watch position import error: ${e.message}")
            }
        }
        android.util.Log.d("CloudStreamSync", "Imported $successfulPositions/${data.watchPositions.size} watch positions")
        
        // Search history import et
        if (data.searchHistory.isNotEmpty()) {
            setSearchHistory(context, data.searchHistory)
        }
        
        // Extensions import et
        if (data.extensions.isNotEmpty()) {
            setExtensions(context, data.extensions)
        }
        
        // Settings import et
        if (data.settings.isNotEmpty()) {
            setSettings(context, data.settings)
        }
    }
    
    private fun getAllBookmarksFullData(): List<String> {
        return try {
            val bookmarks = CS3DataStore.getAllBookmarkedData()
            bookmarks.map { gson.toJson(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getAllWatchPositions(): Map<String, Long> {
        return try {
            val resumeList = CS3DataStore.getAllResumeStateIds()
            val positions = mutableMapOf<String, Long>()
            
            resumeList?.forEach { id ->
                try {
                    val resume = CS3DataStore.getLastWatched(id)
                    if (resume != null) {
                        val viewPos = CS3DataStore.getViewPos(resume.episodeId ?: id)
                        if (viewPos != null) {
                            positions[id.toString()] = viewPos.position
                        }
                    }
                } catch (e: Exception) {
                    // Skip invalid entries
                }
            }
            
            positions
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun getSearchHistory(context: Context): List<String> {
        return try {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val searchHistoryJson = prefs.getString("search_history", null)
            if (searchHistoryJson != null) {
                gson.fromJson(searchHistoryJson, Array<String>::class.java)?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun setSearchHistory(context: Context, history: List<String>) {
        try {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("search_history", gson.toJson(history)).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    private fun getExtensions(context: Context): List<String> {
        return try {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val extensionsJson = prefs.getString(USER_PROVIDER_API, null)
            if (extensionsJson != null) {
                gson.fromJson(extensionsJson, Array<String>::class.java)?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun setExtensions(context: Context, extensions: List<String>) {
        try {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(USER_PROVIDER_API, gson.toJson(extensions)).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    private fun getSettings(context: Context): Map<String, String> {
        return try {
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            val allPrefs = prefs.all
            val settings = mutableMapOf<String, String>()
            
            for (entry in allPrefs.entries) {
                val key = entry.key
                val value = entry.value
                
                // ✅ FIX: Sadece transferable key'leri sync et
                if (!SyncKeyClassifier.isTransferable(key)) {
                    android.util.Log.d("CloudStreamSync", "Skipping non-transferable key: $key")
                    continue
                }
                
                when (value) {
                    is String -> settings[key] = value
                    is Boolean -> settings[key] = value.toString()
                    is Int -> settings[key] = value.toString()
                    is Long -> settings[key] = value.toString()
                    is Float -> settings[key] = value.toString()
                }
            }
            
            settings
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun setSettings(context: Context, settings: Map<String, String>) {
        try {
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            for (entry in settings.entries) {
                val key = entry.key
                val value = entry.value
                
                // ✅ FIX: Sadece transferable key'leri restore et
                if (!SyncKeyClassifier.isTransferable(key)) {
                    android.util.Log.d("CloudStreamSync", "Skipping non-transferable key on restore: $key")
                    continue
                }
                
                // Type'a göre doğru şekilde kaydet
                try {
                    when {
                        value == "true" || value == "false" -> editor.putBoolean(key, value.toBoolean())
                        value.toIntOrNull() != null -> editor.putInt(key, value.toInt())
                        value.toLongOrNull() != null -> editor.putLong(key, value.toLong())
                        value.toFloatOrNull() != null -> editor.putFloat(key, value.toFloat())
                        else -> editor.putString(key, value)
                    }
                } catch (e: Exception) {
                    // Fallback: String olarak kaydet
                    editor.putString(key, value)
                }
            }
            
            editor.apply()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
