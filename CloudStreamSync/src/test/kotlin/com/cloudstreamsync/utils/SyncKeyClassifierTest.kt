package com.cloudstreamsync.utils

import org.junit.Test
import org.junit.Assert.*

class SyncKeyClassifierTest {
    
    @Test
    fun testAuthTokensBlocked() {
        // Anilist tokens
        assertFalse(SyncKeyClassifier.isTransferable("anilist_token"))
        assertFalse(SyncKeyClassifier.isTransferable("anilist_unixtime"))
        assertFalse(SyncKeyClassifier.isTransferable("anilist_user"))
        
        // MAL tokens
        assertFalse(SyncKeyClassifier.isTransferable("mal_token"))
        assertFalse(SyncKeyClassifier.isTransferable("mal_refresh_token"))
        assertFalse(SyncKeyClassifier.isTransferable("mal_user"))
        
        // Simkl tokens
        assertFalse(SyncKeyClassifier.isTransferable("simkl_token"))
        assertFalse(SyncKeyClassifier.isTransferable("simkl_user"))
    }
    
    @Test
    fun testDeviceSpecificBlocked() {
        assertFalse(SyncKeyClassifier.isTransferable("device_id"))
        assertFalse(SyncKeyClassifier.isTransferable("biometric_key"))
        assertFalse(SyncKeyClassifier.isTransferable("download_path_key"))
        assertFalse(SyncKeyClassifier.isTransferable("backup_path_key"))
    }
    
    @Test
    fun testSyncMetadataBlocked() {
        assertFalse(SyncKeyClassifier.isTransferable("sync_token"))
        assertFalse(SyncKeyClassifier.isTransferable("sync_device_id"))
        assertFalse(SyncKeyClassifier.isTransferable("CLOUDSYNC_WATCH_SYNC_CREDS"))
        assertFalse(SyncKeyClassifier.isTransferable("CLOUDSYNC_APP_SETTINGS_SYNC_CREDS"))
    }
    
    @Test
    fun testLocalStateBlocked() {
        assertFalse(SyncKeyClassifier.isTransferable("last_opened_id"))
        assertFalse(SyncKeyClassifier.isTransferable("download_info"))
        assertFalse(SyncKeyClassifier.isTransferable("FILES_TO_DELETE_KEY"))
    }
    
    @Test
    fun testLocalPluginsBlocked() {
        assertFalse(SyncKeyClassifier.isTransferable("plugins_key_local"))
    }
    
    @Test
    fun testNormalSettingsAllowed() {
        assertTrue(SyncKeyClassifier.isTransferable("player_speed"))
        assertTrue(SyncKeyClassifier.isTransferable("theme_color"))
        assertTrue(SyncKeyClassifier.isTransferable("subtitle_size"))
        assertTrue(SyncKeyClassifier.isTransferable("video_buffer_size"))
        assertTrue(SyncKeyClassifier.isTransferable("auto_play"))
    }
    
    @Test
    fun testBookmarksAllowed() {
        assertTrue(SyncKeyClassifier.isTransferable("result_favorites_state_data_123"))
        assertTrue(SyncKeyClassifier.isTransferable("result_watch_state_456"))
    }
    
    @Test
    fun testResumeWatchingAllowed() {
        assertTrue(SyncKeyClassifier.isTransferable("result_resume_watching_789"))
        assertTrue(SyncKeyClassifier.isTransferable("video_pos_dur/123/456"))
    }
    
    @Test
    fun testSearchHistoryAllowed() {
        assertTrue(SyncKeyClassifier.isTransferable("search_history"))
    }
    
    @Test
    fun testExtensionsAllowed() {
        assertTrue(SyncKeyClassifier.isTransferable("plugins_key"))
        assertTrue(SyncKeyClassifier.isTransferable("repositories"))
        assertTrue(SyncKeyClassifier.isTransferable("REPOSITORIES_KEY"))
    }
    
    @Test
    fun testCaseInsensitive() {
        // Uppercase
        assertFalse(SyncKeyClassifier.isTransferable("ANILIST_TOKEN"))
        assertFalse(SyncKeyClassifier.isTransferable("DEVICE_ID"))
        
        // Mixed case
        assertFalse(SyncKeyClassifier.isTransferable("Anilist_Token"))
        assertFalse(SyncKeyClassifier.isTransferable("Device_Id"))
        
        // Contains blocked substring
        assertFalse(SyncKeyClassifier.isTransferable("my_anilist_token_backup"))
        assertFalse(SyncKeyClassifier.isTransferable("custom_device_id_v2"))
    }
}
