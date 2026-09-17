package com.cloudstreamsync.models

import com.google.gson.annotations.SerializedName

data class SyncConfig(
    // Global
    @SerializedName("sync_enabled")
    val syncEnabled: Boolean = true,
    
    // Per-category backup
    @SerializedName("backup_bookmarks")
    val backupBookmarks: Boolean = true,
    
    @SerializedName("backup_resume_watching")
    val backupResumeWatching: Boolean = true,
    
    @SerializedName("backup_search_history")
    val backupSearchHistory: Boolean = true,
    
    @SerializedName("backup_extensions")
    val backupExtensions: Boolean = true,
    
    // Per-category restore
    @SerializedName("restore_bookmarks")
    val restoreBookmarks: Boolean = true,
    
    @SerializedName("restore_resume_watching")
    val restoreResumeWatching: Boolean = true,
    
    @SerializedName("restore_search_history")
    val restoreSearchHistory: Boolean = true,
    
    @SerializedName("restore_extensions")
    val restoreExtensions: Boolean = true,
    
    // Settings subcategories - backup
    @SerializedName("backup_player")
    val backupPlayer: Boolean = true,
    
    @SerializedName("backup_subtitles")
    val backupSubtitles: Boolean = true,
    
    @SerializedName("backup_theme")
    val backupTheme: Boolean = true,
    
    @SerializedName("backup_layout")
    val backupLayout: Boolean = true,
    
    @SerializedName("backup_downloads")
    val backupDownloads: Boolean = true,
    
    @SerializedName("backup_general")
    val backupGeneral: Boolean = true,
    
    // Settings subcategories - restore
    @SerializedName("restore_player")
    val restorePlayer: Boolean = true,
    
    @SerializedName("restore_subtitles")
    val restoreSubtitles: Boolean = true,
    
    @SerializedName("restore_theme")
    val restoreTheme: Boolean = true,
    
    @SerializedName("restore_layout")
    val restoreLayout: Boolean = true,
    
    @SerializedName("restore_downloads")
    val restoreDownloads: Boolean = true,
    
    @SerializedName("restore_general")
    val restoreGeneral: Boolean = true
) {
    fun isCategoryBackupEnabled(category: SyncCategory): Boolean {
        return when (category) {
            SyncCategory.BOOKMARKS -> backupBookmarks
            SyncCategory.RESUME_WATCHING -> backupResumeWatching
            SyncCategory.SEARCH_HISTORY -> backupSearchHistory
            SyncCategory.EXTENSIONS -> backupExtensions
            SyncCategory.SETTINGS -> true // Settings checked by subcategory
        }
    }
    
    fun isCategoryRestoreEnabled(category: SyncCategory): Boolean {
        return when (category) {
            SyncCategory.BOOKMARKS -> restoreBookmarks
            SyncCategory.RESUME_WATCHING -> restoreResumeWatching
            SyncCategory.SEARCH_HISTORY -> restoreSearchHistory
            SyncCategory.EXTENSIONS -> restoreExtensions
            SyncCategory.SETTINGS -> true // Settings checked by subcategory
        }
    }
    
    fun isSettingsSubcategoryBackupEnabled(subcategory: SettingsSubCategory): Boolean {
        return when (subcategory) {
            SettingsSubCategory.PLAYER -> backupPlayer
            SettingsSubCategory.SUBTITLES -> backupSubtitles
            SettingsSubCategory.THEME -> backupTheme
            SettingsSubCategory.LAYOUT -> backupLayout
            SettingsSubCategory.DOWNLOADS -> backupDownloads
            SettingsSubCategory.GENERAL -> backupGeneral
        }
    }
    
    fun isSettingsSubcategoryRestoreEnabled(subcategory: SettingsSubCategory): Boolean {
        return when (subcategory) {
            SettingsSubCategory.PLAYER -> restorePlayer
            SettingsSubCategory.SUBTITLES -> restoreSubtitles
            SettingsSubCategory.THEME -> restoreTheme
            SettingsSubCategory.LAYOUT -> restoreLayout
            SettingsSubCategory.DOWNLOADS -> restoreDownloads
            SettingsSubCategory.GENERAL -> restoreGeneral
        }
    }
}
