package com.cloudstreamsync.utils

import com.cloudstreamsync.models.SyncCategory
import com.cloudstreamsync.models.SettingsSubCategory

object SyncKeyClassifier {
    private val nonTransferableKeys = listOf(
        // Auth tokens
        "anilist_token", "anilist_unixtime", "anilist_user", "anilist_cached_list", "anilist_accounts", "anilist_active",
        "mal_token", "mal_refresh_token", "mal_user", "mal_cached_list", "mal_unixtime", "mal_accounts", "mal_active",
        "simkl_token", "simkl_user", "simkl_cached_list", "simkl_cached_time", "simkl_accounts", "simkl_active",
        "SIMKL_API_CACHE", "ANIWAVE_SIMKL_SYNC",
        "open_subtitles_user", "opensubtitles_accounts", "opensubtitles_active",
        "subdl_user", "subdl_accounts", "subdl_active",
        
        // Device-specific
        "device_id", "biometric_key",
        "download_path_key", "backup_path_key",
        "download_path_key_visual", "backup_dir_path_key",
        
        // Sync metadata
        "sync_token", "sync_project_num", "sync_project_id", "sync_item_id", "sync_device_id",
        "CLOUDSYNC_WATCH_SYNC_CREDS", "CLOUDSYNC_APP_SETTINGS_SYNC_CREDS",
        "last_sync_api", "last_sync_api_key",
        "restore_device", "backup_device",
        
        // Local state
        "last_opened_id", "last_click_action",
        "download_info", "download_resume", "download_q_resume",
        "download_episode_cache",
        "FILES_TO_DELETE_KEY",
        "result_resume_watching_migrated",
        "library_folder",
        
        // Other sensitive
        "nginx_user", "fshare_setup", "fshare_token", "bluphim_token",
        "jsdelivr_proxy_key",
        
        // Version/setup flags
        "VERSION_NAME", "HAS_DONE_SETUP",
        "prerelease_update",
        "data_store_helper/account_key_index",
        
        // Provider state
        "used_fstream_providers_v3", "fstream_version",
        "home_api_used", "home_api", "user_selected_homepage_api",
        "home_pref_homepage",
        
        // View state (cihaza özel)
        "viewpager_item_key",
        "library_sorting_mode", "results_sorting_mode",
        
        // Local plugins (yüklü pluginler listesi - sync edilmemeli)
        "plugins_key_local"
    )
    
    fun isTransferable(key: String): Boolean {
        val lower = key.lowercase()
        return nonTransferableKeys.none { blocked ->
            lower.contains(blocked.lowercase())
        }
    }
    
    fun classifyKey(key: String): SyncCategory? {
        if (!isTransferable(key)) return null
        
        val lower = key.lowercase()
        
        return when {
            // Bookmarks
            "result_favorites_state_data" in lower || "result_watch_state" in lower ->
                SyncCategory.BOOKMARKS
                
            // Resume watching
            "result_resume_watching" in lower || "video_pos_dur" in lower ||
            "download_header_cache" in lower || "result_season" in lower ||
            "result_dub" in lower || "result_episode" in lower ->
                SyncCategory.RESUME_WATCHING
                
            // Search history
            "search_history" in lower ->
                SyncCategory.SEARCH_HISTORY
                
            // Extensions (skip plugins_key_local - already filtered)
            "plugins_key" in lower || "repositories" in lower ->
                SyncCategory.EXTENSIONS
                
            // Everything else is settings
            else -> SyncCategory.SETTINGS
        }
    }
    
    fun classifySettingsKey(key: String): SettingsSubCategory {
        val lower = key.lowercase()
        
        return when {
            // Player
            "player" in lower || "video" in lower || "buffer" in lower ||
            "skip" in lower || "gesture" in lower || "decoder" in lower ||
            "speed" in lower || "render" in lower || "resize" in lower ||
            "volume" in lower || "brightness" in lower || "fit" in lower ||
            "aspect" in lower || "play" in lower ->
                SettingsSubCategory.PLAYER
                
            // Subtitles
            "subtitle" in lower || "sub" in lower || "caption" in lower ||
            "font" in lower || "lang" in lower ->
                SettingsSubCategory.SUBTITLES
                
            // Theme
            "theme" in lower || "dark" in lower || "color" in lower ||
            "accent" in lower || "primary" in lower || "style" in lower ->
                SettingsSubCategory.THEME
                
            // Layout
            "layout" in lower || "view" in lower || "grid" in lower ||
            "home" in lower || "card" in lower || "tab" in lower ||
            "row" in lower || "show_" in lower || "homepage" in lower ||
            "list" in lower ->
                SettingsSubCategory.LAYOUT
                
            // Downloads
            "download" in lower || "path" in lower ->
                SettingsSubCategory.DOWNLOADS
                
            // General (everything else)
            else -> SettingsSubCategory.GENERAL
        }
    }
}
