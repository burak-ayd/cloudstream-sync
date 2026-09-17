package com.cloudstreamsync.models

import com.google.gson.annotations.SerializedName

// Yeni: Category-based sync data
data class SyncData(
    @SerializedName("categories")
    val categories: Map<String, CategoryData> = emptyMap(),
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class CategoryData(
    @SerializedName("hash")
    val hash: String,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("backup")
    val backup: BackupVars
)

data class BackupVars(
    @SerializedName("booleans")
    val booleans: Map<String, Boolean>? = null,
    
    @SerializedName("integers")
    val integers: Map<String, Int>? = null,
    
    @SerializedName("strings")
    val strings: Map<String, String>? = null,
    
    @SerializedName("floats")
    val floats: Map<String, Float>? = null,
    
    @SerializedName("longs")
    val longs: Map<String, Long>? = null,
    
    @SerializedName("stringSets")
    val stringSets: Map<String, Set<String>>? = null
)

// Legacy support - eski SyncData ile uyumluluk için
data class LegacySyncData(
    @SerializedName("bookmarks")
    val bookmarks: List<String> = emptyList(),
    
    @SerializedName("watch_positions")
    val watchPositions: Map<String, Long> = emptyMap(),
    
    @SerializedName("search_history")
    val searchHistory: List<String> = emptyList(),
    
    @SerializedName("extensions")
    val extensions: List<String> = emptyList(),
    
    @SerializedName("settings")
    val settings: Map<String, String> = emptyMap(),
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
