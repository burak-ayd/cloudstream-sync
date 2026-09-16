package com.cloudstreamsync

import android.content.Context
import com.cloudstreamsync.models.SyncData
import com.cloudstreamsync.providers.SupabaseProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncManager {
    private var context: Context? = null
    private val gson = Gson()
    private var provider: CloudProvider? = null
    
    fun init(ctx: Context) {
        context = ctx
    }
    
    fun setProvider(type: CloudProviderType, config: Map<String, String>) {
        provider = when(type) {
            CloudProviderType.SUPABASE -> SupabaseProvider(config)
            CloudProviderType.GDRIVE -> throw NotImplementedError("GDrive yakında")
            CloudProviderType.FIREBASE -> throw NotImplementedError("Firebase yakında")
        }
    }
    
    suspend fun uploadData(data: SyncData): Result<Unit> = withContext(Dispatchers.IO) {
        provider?.upload(data) ?: Result.failure(Exception("Provider yok"))
    }
    
    suspend fun downloadData(): Result<SyncData> = withContext(Dispatchers.IO) {
        provider?.download() ?: Result.failure(Exception("Provider yok"))
    }
    
    suspend fun deleteData(): Result<Unit> = withContext(Dispatchers.IO) {
        provider?.delete() ?: Result.failure(Exception("Provider yok"))
    }
}
