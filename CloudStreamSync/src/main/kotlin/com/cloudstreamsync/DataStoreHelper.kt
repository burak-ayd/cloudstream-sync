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
        // ponytail: BookmarkedData çok fazla required parametre içeriyor (name, url, type, posterUrl, year, etc.)
        // Buluttan sadece id ve bookmarkedTime geliyor, eksik verilerle obje oluşturulamıyor.
        // 
        // Çözüm yolları:
        // 1. Export'ta tam SearchResponse objelerini kaydet (çok büyük veri)
        // 2. Import'ta mevcut bookmark'u güncelle (bookmarkedTime'ı sync et)
        // 3. CloudStream'e PR gönder - sadece ID ile bookmark ekleme API'si
        //
        // Şimdilik: Import disabled, sadece export çalışıyor.
        // Kullanım senaryosu: Yedekleme ve başka cihazlarda manuel ekleme için referans
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
