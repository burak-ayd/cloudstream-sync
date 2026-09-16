package com.cloudstreamsync

import android.content.Context
import com.cloudstreamsync.models.SyncData
import com.lagradost.cloudstream3.utils.DataStoreHelper as CS3DataStore
import com.google.gson.Gson

// CloudStream DataStore wrapper - DataStoreHelper'ın public fonksiyonlarını kullanıyor
object DataStoreHelper {
    private val gson = Gson()
    
    fun collectCurrentData(context: Context): SyncData {
        return SyncData(
            bookmarks = getAllBookmarks(),
            watchPositions = getAllWatchPositions(),
            searchHistory = emptyList(), // ponytail: search history ayrı API gerek
            extensions = emptyList(),     // ponytail: eklenti listesi için ayrı API gerek
            settings = emptyMap(),        // ponytail: ayarlar için ayrı API gerek
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun applyData(context: Context, data: SyncData) {
        // ponytail: CloudStream'in setBookmarkedData/setViewPosAndResume fonksiyonları 
        // inline olduğu için JVM target uyumsuzluğu var. Import/export için 
        // DataStoreHelper'ın public API'lerini kullanmak gerek.
        // Şimdilik veri sadece export ediliyor, import disabled.
    }
    
    private fun getAllBookmarks(): List<String> {
        return try {
            val bookmarks = CS3DataStore.getAllBookmarkedData()
            bookmarks.map { bookmark ->
                gson.toJson(mapOf(
                    "id" to bookmark.id,
                    "name" to (bookmark.apiName ?: ""),
                    "bookmarkedTime" to bookmark.bookmarkedTime
                ))
            }
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
                        // Resume sadece metadata, gerçek position VIDEO_POS_DUR'da
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
