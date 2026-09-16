package com.cloudstreamsync

import com.cloudstreamsync.models.SyncData

// ponytail: tek interface, GDrive/Supabase/Firebase impl'de genişlet
interface CloudProvider {
    suspend fun upload(data: SyncData): Result<Unit>
    suspend fun download(): Result<SyncData>
    suspend fun delete(): Result<Unit>
    fun isConfigured(): Boolean
    fun getName(): String
}

enum class CloudProviderType {
    GDRIVE,
    SUPABASE,
    FIREBASE
}
