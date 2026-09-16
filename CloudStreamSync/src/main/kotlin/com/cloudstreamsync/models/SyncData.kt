package com.cloudstreamsync.models

import com.google.gson.annotations.SerializedName

data class SyncData(
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
