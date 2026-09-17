package com.cloudstreamsync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.cloudstreamsync.models.SyncCategory
import com.cloudstreamsync.utils.SyncKeyClassifier
import kotlinx.coroutines.*

object AutoSyncManager {
    private const val TAG = "AutoSyncManager"
    private const val PUSH_DEBOUNCE_MS = 2000L
    
    private var dataPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var defaultPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var pushJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun startAutoSync(context: Context) {
        Log.d(TAG, "Starting auto-sync")
        
        // DataStore listener
        val dataPrefs = context.getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        dataPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            onKeyChanged(key)
        }
        dataPrefs.registerOnSharedPreferenceChangeListener(dataPrefsListener)
        
        // Default prefs listener
        val defaultPrefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        defaultPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            onKeyChanged(key)
        }
        defaultPrefs.registerOnSharedPreferenceChangeListener(defaultPrefsListener)
        
        Log.d(TAG, "Auto-sync listeners registered")
    }
    
    fun stopAutoSync(context: Context) {
        Log.d(TAG, "Stopping auto-sync")
        
        // Unregister DataStore listener
        val dataPrefs = context.getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        dataPrefsListener?.let { dataPrefs.unregisterOnSharedPreferenceChangeListener(it) }
        
        // Unregister default prefs listener
        val defaultPrefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        defaultPrefsListener?.let { defaultPrefs.unregisterOnSharedPreferenceChangeListener(it) }
        
        // Cancel pending push
        pushJob?.cancel()
        
        Log.d(TAG, "Auto-sync stopped")
    }
    
    private fun onKeyChanged(key: String?) {
        if (key == null) return
        
        val category = SyncKeyClassifier.classifyKey(key)
        if (category == null) {
            Log.d(TAG, "Key not transferable, skipping: $key")
            return
        }
        
        Log.d(TAG, "Key changed: $key → category: $category")
        SyncManager.markDirty(category)
        schedulePush()
    }
    
    private fun schedulePush() {
        // Cancel existing job
        pushJob?.cancel()
        
        // Schedule new push
        pushJob = scope.launch {
            delay(PUSH_DEBOUNCE_MS)
            pushDirtyCategories()
        }
    }
    
    private suspend fun pushDirtyCategories() {
        val categories = SyncManager.getDirtyCategories()
        if (categories.isEmpty()) {
            Log.d(TAG, "No dirty categories to push")
            return
        }
        
        Log.d(TAG, "Pushing ${categories.size} dirty categories: $categories")
        
        categories.forEach { category ->
            val result = SyncManager.uploadCategory(category)
            result.fold(
                onSuccess = { 
                    Log.d(TAG, "Category uploaded successfully: $category")
                },
                onFailure = { 
                    Log.e(TAG, "Failed to upload category $category: ${it.message}")
                }
            )
        }
        
        SyncManager.clearDirtyCategories()
    }
}
