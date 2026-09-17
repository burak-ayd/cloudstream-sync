package com.cloudstreamsync

import android.content.Context
import android.content.SharedPreferences
import com.cloudstreamsync.models.*
import com.cloudstreamsync.utils.SyncKeyClassifier
import com.lagradost.cloudstream3.utils.DataStoreHelper as CS3DataStore
import com.google.gson.Gson
import java.security.MessageDigest

object BackupManager {
    private val gson = Gson()
    
    fun collectCategoryData(
        context: Context,
        category: SyncCategory,
        config: SyncConfig
    ): CategoryData? {
        if (!config.isCategoryBackupEnabled(category)) {
            return null
        }
        
        val backup = when (category) {
            SyncCategory.BOOKMARKS -> collectBookmarks()
            SyncCategory.RESUME_WATCHING -> collectResumeWatching()
            SyncCategory.SEARCH_HISTORY -> collectSearchHistory(context)
            SyncCategory.EXTENSIONS -> collectExtensions(context)
            SyncCategory.SETTINGS -> collectSettings(context, config)
        }
        
        if (backup == null) return null
        
        val json = gson.toJson(backup)
        val hash = computeHash(json)
        
        return CategoryData(
            hash = hash,
            timestamp = System.currentTimeMillis(),
            backup = backup
        )
    }
    
    private fun collectBookmarks(): BackupVars? {
        return try {
            val bookmarks = CS3DataStore.getAllBookmarkedData()
            val bookmarksJson = bookmarks.map { gson.toJson(it) }
            
            if (bookmarksJson.isEmpty()) return null
            
            BackupVars(
                strings = mapOf("bookmarks_list" to gson.toJson(bookmarksJson))
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun collectResumeWatching(): BackupVars? {
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
                    // Skip
                }
            }
            
            if (positions.isEmpty()) return null
            
            BackupVars(
                strings = mapOf("watch_positions" to gson.toJson(positions))
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun collectSearchHistory(context: Context): BackupVars? {
        return try {
            val prefs = context.getSharedPreferences("rebuild_preference", Context.MODE_PRIVATE)
            val searchHistoryJson = prefs.getString("search_history", null)
            
            if (searchHistoryJson == null) return null
            
            BackupVars(
                strings = mapOf("search_history" to searchHistoryJson)
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun collectExtensions(context: Context): BackupVars? {
        return try {
            val prefs = context.getSharedPreferences("rebuild_preference", Context.MODE_PRIVATE)
            val extensionsJson = prefs.getString("user_custom_sites", null)
            
            if (extensionsJson == null) return null
            
            BackupVars(
                strings = mapOf("extensions_list" to extensionsJson)
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun collectSettings(context: Context, config: SyncConfig): BackupVars? {
        return try {
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            val allPrefs = prefs.all
            
            val booleans = mutableMapOf<String, Boolean>()
            val integers = mutableMapOf<String, Int>()
            val strings = mutableMapOf<String, String>()
            val floats = mutableMapOf<String, Float>()
            val longs = mutableMapOf<String, Long>()
            
            for (entry in allPrefs.entries) {
                val key = entry.key
                val value = entry.value
                
                if (!SyncKeyClassifier.isTransferable(key)) continue
                
                val category = SyncKeyClassifier.classifyKey(key)
                if (category != SyncCategory.SETTINGS) continue
                
                val subcategory = SyncKeyClassifier.classifySettingsKey(key)
                if (!config.isSettingsSubcategoryBackupEnabled(subcategory)) continue
                
                when (value) {
                    is Boolean -> booleans[key] = value
                    is Int -> integers[key] = value
                    is String -> strings[key] = value
                    is Float -> floats[key] = value
                    is Long -> longs[key] = value
                }
            }
            
            if (booleans.isEmpty() && integers.isEmpty() && strings.isEmpty() && 
                floats.isEmpty() && longs.isEmpty()) {
                return null
            }
            
            BackupVars(
                booleans = booleans.ifEmpty { null },
                integers = integers.ifEmpty { null },
                strings = strings.ifEmpty { null },
                floats = floats.ifEmpty { null },
                longs = longs.ifEmpty { null }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun restoreCategoryData(
        context: Context,
        category: SyncCategory,
        cloudData: CategoryData,
        config: SyncConfig
    ): Boolean {
        if (!config.isCategoryRestoreEnabled(category)) {
            return false
        }
        
        return when (category) {
            SyncCategory.BOOKMARKS -> restoreBookmarks(cloudData)
            SyncCategory.RESUME_WATCHING -> restoreResumeWatching(cloudData)
            SyncCategory.SEARCH_HISTORY -> restoreSearchHistory(context, cloudData)
            SyncCategory.EXTENSIONS -> restoreExtensions(context, cloudData)
            SyncCategory.SETTINGS -> restoreSettings(context, cloudData, config)
        }
    }
    
    private fun restoreBookmarks(cloudData: CategoryData): Boolean {
        return try {
            val bookmarksJson = cloudData.backup.strings?.get("bookmarks_list") ?: return false
            val bookmarksList: List<String> = gson.fromJson(bookmarksJson, 
                object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
            
            var restored = 0
            bookmarksList.forEach { bookmarkJson ->
                try {
                    val bookmark = gson.fromJson(bookmarkJson, CS3DataStore.BookmarkedData::class.java)
                    if (bookmark?.id != null) {
                        CS3DataStore.setBookmarkedData(bookmark.id, bookmark)
                        restored++
                    }
                } catch (e: Exception) {
                    // Skip
                }
            }
            
            restored > 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun restoreResumeWatching(cloudData: CategoryData): Boolean {
        return try {
            val positionsJson = cloudData.backup.strings?.get("watch_positions") ?: return false
            val positions: Map<String, Long> = gson.fromJson(positionsJson,
                object : com.google.gson.reflect.TypeToken<Map<String, Long>>() {}.type)
            
            var restored = 0
            positions.forEach { (idStr, position) ->
                try {
                    val id = idStr.toIntOrNull()
                    if (id != null && position > 0) {
                        CS3DataStore.setViewPos(id, position, 0L)
                        restored++
                    }
                } catch (e: Exception) {
                    // Skip
                }
            }
            
            restored > 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun restoreSearchHistory(context: Context, cloudData: CategoryData): Boolean {
        return try {
            val searchHistoryJson = cloudData.backup.strings?.get("search_history") ?: return false
            val prefs = context.getSharedPreferences("rebuild_preference", Context.MODE_PRIVATE)
            prefs.edit().putString("search_history", searchHistoryJson).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun restoreExtensions(context: Context, cloudData: CategoryData): Boolean {
        return try {
            val extensionsJson = cloudData.backup.strings?.get("extensions_list") ?: return false
            val prefs = context.getSharedPreferences("rebuild_preference", Context.MODE_PRIVATE)
            prefs.edit().putString("user_custom_sites", extensionsJson).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun restoreSettings(context: Context, cloudData: CategoryData, config: SyncConfig): Boolean {
        return try {
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            var restored = 0
            
            cloudData.backup.booleans?.forEach { (key, value) ->
                if (shouldRestoreSettingsKey(key, config)) {
                    editor.putBoolean(key, value)
                    restored++
                }
            }
            
            cloudData.backup.integers?.forEach { (key, value) ->
                if (shouldRestoreSettingsKey(key, config)) {
                    editor.putInt(key, value)
                    restored++
                }
            }
            
            cloudData.backup.strings?.forEach { (key, value) ->
                if (shouldRestoreSettingsKey(key, config)) {
                    editor.putString(key, value)
                    restored++
                }
            }
            
            cloudData.backup.floats?.forEach { (key, value) ->
                if (shouldRestoreSettingsKey(key, config)) {
                    editor.putFloat(key, value)
                    restored++
                }
            }
            
            cloudData.backup.longs?.forEach { (key, value) ->
                if (shouldRestoreSettingsKey(key, config)) {
                    editor.putLong(key, value)
                    restored++
                }
            }
            
            editor.apply()
            restored > 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun shouldRestoreSettingsKey(key: String, config: SyncConfig): Boolean {
        if (!SyncKeyClassifier.isTransferable(key)) return false
        
        val category = SyncKeyClassifier.classifyKey(key)
        if (category != SyncCategory.SETTINGS) return false
        
        val subcategory = SyncKeyClassifier.classifySettingsKey(key)
        return config.isSettingsSubcategoryRestoreEnabled(subcategory)
    }
    
    private fun computeHash(data: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(data.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
