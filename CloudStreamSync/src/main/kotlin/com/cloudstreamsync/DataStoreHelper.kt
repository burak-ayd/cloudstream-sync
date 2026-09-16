package com.cloudstreamsync

import android.content.Context
import com.cloudstreamsync.models.SyncData
import com.lagradost.cloudstream3.utils.DataStoreHelper as CS3DataStore
import com.google.gson.Gson

// CloudStream DataStore wrapper - Full obje sync
object DataStoreHelper {
    private val gson = Gson()
    
    fun collectCurrentData(context: Context): SyncData {
        return SyncData(
            bookmarks = getAllBookmarksFullData(),
            watchPositions = getAllWatchPositions(),
            searchHistory = emptyList(), // ponytail: search history ayrı API gerek
            extensions = emptyList(),     // ponytail: eklenti listesi için ayrı API gerek
            settings = emptyMap(),        // ponytail: ayarlar için ayrı API gerek
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun applyData(context: Context, data: SyncData) {
        // Bookmarks import et - tam obje
        data.bookmarks.forEach { bookmarkJson ->
            try {
                val bookmarkedData = gson.fromJson(bookmarkJson, CS3DataStore.BookmarkedData::class.java)
                if (bookmarkedData != null && bookmarkedData.id != null) {
                    CS3DataStore.setBookmarkedData(bookmarkedData.id, bookmarkedData)
                }
            } catch (e: Exception) {
                // Skip invalid entries
            }
        }
        
        // Watch positions import et
        data.watchPositions.forEach { (idStr, position) ->
            try {
                val id = idStr.toIntOrNull()
                if (id != null && position > 0) {
                    CS3DataStore.setViewPos(id, position, 0L)
                }
            } catch (e: Exception) {
                // Skip invalid entries
            }
        }
    }
    
    private fun getAllBookmarksFullData(): List<String> {
        return try {
            val bookmarks = CS3DataStore.getAllBookmarkedData()
            // Tam BookmarkedData objesini JSON olarak kaydet
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
}
