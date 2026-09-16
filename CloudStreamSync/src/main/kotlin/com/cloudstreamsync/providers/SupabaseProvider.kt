package com.cloudstreamsync.providers

import com.cloudstreamsync.CloudProvider
import com.cloudstreamsync.models.SyncData
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SupabaseProvider(private val config: Map<String, String>) : CloudProvider {
    private val client = OkHttpClient()
    private val gson = Gson()
    
    private val url = config["url"] ?: ""
    private val apiKey = config["apiKey"] ?: ""
    private val table = config["table"] ?: "cloudstream_sync"
    private val userId = config["userId"] ?: "default_user"
    
    override suspend fun upload(data: SyncData): Result<Unit> {
        if (!isConfigured()) return Result.failure(Exception("Supabase yapılandırılmamış"))
        
        val json = gson.toJson(data)
        val body = """{"user_id":"$userId","data":$json}""".toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$url/rest/v1/$table")
            .post(body)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Upload başarısız: ${response.code}"))
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
    
    override suspend fun download(): Result<SyncData> {
        if (!isConfigured()) return Result.failure(Exception("Supabase yapılandırılmamış"))
        
        val request = Request.Builder()
            .url("$url/rest/v1/$table?user_id=eq.$userId&select=data&order=timestamp.desc&limit=1")
            .get()
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                val data: List<Map<String, Any>> = gson.fromJson(body, listType)
                if (data.isEmpty()) Result.failure(Exception("Veri yok"))
                else {
                    val syncData = gson.fromJson(gson.toJson(data[0]["data"]), SyncData::class.java)
                    Result.success(syncData)
                }
            } else Result.failure(Exception("Download başarısız: ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun delete(): Result<Unit> {
        if (!isConfigured()) return Result.failure(Exception("Supabase yapılandırılmamış"))
        
        val request = Request.Builder()
            .url("$url/rest/v1/$table?user_id=eq.$userId")
            .delete()
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Delete başarısız: ${response.code}"))
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
    
    override fun isConfigured(): Boolean {
        return url.isNotEmpty() && apiKey.isNotEmpty()
    }
    
    override fun getName(): String = "Supabase"
}
