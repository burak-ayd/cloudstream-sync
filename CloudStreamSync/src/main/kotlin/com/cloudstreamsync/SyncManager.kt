package com.cloudstreamsync

import android.content.Context
import android.util.Log
import com.cloudstreamsync.models.SyncCategory
import com.cloudstreamsync.models.SyncData
import com.cloudstreamsync.providers.SupabaseProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncManager {
    private const val TAG = "SyncManager"
    private var context: Context? = null
    private val gson = Gson()
    private var provider: CloudProvider? = null
    
    // Restore guard
    @Volatile private var isRestoring = false
    @Volatile private var restoringUntil = 0L
    private const val RESTORE_GUARD_MS = 5000L
    
    // Dirty tracking
    private val dirtyCategories = mutableSetOf<SyncCategory>()
    private val dirtyCategoriesLock = Any()
    
    fun init(ctx: Context) {
        context = ctx
    }
    
    fun setProvider(type: CloudProviderType, config: Map<String, String>) {
        provider = when(type) {
            CloudProviderType.SUPABASE -> SupabaseProvider(config)
            CloudProviderType.GDRIVE -> throw NotImplementedError("GDrive yakında")
        }
    }
    
    fun markDirty(category: SyncCategory) {
        synchronized(dirtyCategoriesLock) {
            if (isRestoring || System.currentTimeMillis() < restoringUntil) {
                Log.d(TAG, "Ignoring change during restore: $category")
                return
            }
            dirtyCategories.add(category)
        }
    }
    
    fun getDirtyCategories(): Set<SyncCategory> {
        synchronized(dirtyCategoriesLock) {
            return dirtyCategories.toSet()
        }
    }
    
    fun clearDirtyCategories() {
        synchronized(dirtyCategoriesLock) {
            dirtyCategories.clear()
        }
    }
    
    suspend fun uploadCategory(category: SyncCategory): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ctx = context ?: return@withContext Result.failure(Exception("Context yok"))
            val config = ConfigManager.loadSyncConfig(ctx)
            
            val categoryData = BackupManager.collectCategoryData(ctx, category, config)
                ?: return@withContext Result.failure(Exception("Category disabled or empty: $category"))
            
            val syncData = SyncData(
                categories = mapOf(category.key to categoryData),
                timestamp = System.currentTimeMillis()
            )
            
            provider?.upload(syncData) ?: Result.failure(Exception("Provider yok"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadAllCategories(): Result<Map<SyncCategory, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val ctx = context ?: return@withContext Result.failure(Exception("Context yok"))
            val config = ConfigManager.loadSyncConfig(ctx)
            
            val categories = mutableMapOf<String, com.cloudstreamsync.models.CategoryData>()
            val results = mutableMapOf<SyncCategory, Boolean>()
            
            SyncCategory.values().forEach { category ->
                val categoryData = BackupManager.collectCategoryData(ctx, category, config)
                if (categoryData != null) {
                    categories[category.key] = categoryData
                    results[category] = true
                } else {
                    results[category] = false
                }
            }
            
            if (categories.isEmpty()) {
                return@withContext Result.failure(Exception("No data to upload"))
            }
            
            val syncData = SyncData(
                categories = categories,
                timestamp = System.currentTimeMillis()
            )
            
            provider?.upload(syncData)?.fold(
                onSuccess = { Result.success(results) },
                onFailure = { Result.failure(it) }
            ) ?: Result.failure(Exception("Provider yok"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun downloadCategory(category: SyncCategory): Result<Unit> = withContext(Dispatchers.IO) {
        isRestoring = true
        restoringUntil = System.currentTimeMillis() + RESTORE_GUARD_MS
        
        try {
            val ctx = context ?: return@withContext Result.failure(Exception("Context yok"))
            val result = provider?.download() ?: return@withContext Result.failure(Exception("Provider yok"))
            
            result.fold(
                onSuccess = { syncData ->
                    val categoryData = syncData.categories[category.key]
                        ?: return@fold Result.failure<Unit>(Exception("Category not found: $category"))
                    
                    val config = ConfigManager.loadSyncConfig(ctx)
                    val restored = BackupManager.restoreCategoryData(ctx, category, categoryData, config)
                    
                    if (restored) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Nothing to restore"))
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } finally {
            isRestoring = false
        }
    }
    
    suspend fun downloadAllCategories(): Result<Map<SyncCategory, Boolean>> = withContext(Dispatchers.IO) {
        isRestoring = true
        restoringUntil = System.currentTimeMillis() + RESTORE_GUARD_MS
        
        try {
            val ctx = context ?: return@withContext Result.failure(Exception("Context yok"))
            val result = provider?.download() ?: return@withContext Result.failure(Exception("Provider yok"))
            
            result.fold(
                onSuccess = { syncData ->
                    val config = ConfigManager.loadSyncConfig(ctx)
                    val results = mutableMapOf<SyncCategory, Boolean>()
                    
                    SyncCategory.values().forEach { category ->
                        val categoryData = syncData.categories[category.key]
                        if (categoryData != null) {
                            val restored = BackupManager.restoreCategoryData(ctx, category, categoryData, config)
                            results[category] = restored
                        } else {
                            results[category] = false
                        }
                    }
                    
                    Result.success(results)
                },
                onFailure = { Result.failure(it) }
            )
        } finally {
            isRestoring = false
        }
    }
    
    // Legacy support
    suspend fun uploadData(data: com.cloudstreamsync.models.LegacySyncData): Result<Unit> = withContext(Dispatchers.IO) {
        provider?.upload(convertLegacyToNew(data)) ?: Result.failure(Exception("Provider yok"))
    }
    
    suspend fun downloadData(): Result<com.cloudstreamsync.models.LegacySyncData> = withContext(Dispatchers.IO) {
        val result = provider?.download() ?: return@withContext Result.failure(Exception("Provider yok"))
        result.fold(
            onSuccess = { syncData -> Result.success(convertNewToLegacy(syncData)) },
            onFailure = { Result.failure(it) }
        )
    }
    
    suspend fun deleteData(): Result<Unit> = withContext(Dispatchers.IO) {
        provider?.delete() ?: Result.failure(Exception("Provider yok"))
    }
    
    private fun convertLegacyToNew(legacy: com.cloudstreamsync.models.LegacySyncData): SyncData {
        // Legacy format'ı yeni format'a çevir
        val categories = mutableMapOf<String, com.cloudstreamsync.models.CategoryData>()
        
        // TODO: Proper legacy conversion - şimdilik basit wrapper
        // Bu geçici bir çözüm, ileride düzgün kategori bazlı conversion yapılacak
        
        return SyncData(
            categories = categories,
            timestamp = legacy.timestamp
        )
    }
    
    private fun convertNewToLegacy(newData: SyncData): com.cloudstreamsync.models.LegacySyncData {
        // Yeni format'ı legacy format'a çevir
        // TODO: Proper conversion - şimdilik boş data
        return com.cloudstreamsync.models.LegacySyncData(
            bookmarks = emptyList(),
            watchPositions = emptyMap(),
            searchHistory = emptyList(),
            extensions = emptyList(),
            settings = emptyMap(),
            timestamp = newData.timestamp
        )
    }
}
