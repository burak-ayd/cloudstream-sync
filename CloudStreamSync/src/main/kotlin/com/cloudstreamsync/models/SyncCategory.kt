package com.cloudstreamsync.models

enum class SyncCategory(val key: String) {
    EXTENSIONS("extensions"),
    SETTINGS("settings"),
    BOOKMARKS("bookmarks"),
    RESUME_WATCHING("resume_watching"),
    SEARCH_HISTORY("search_history")
}

enum class SettingsSubCategory {
    PLAYER,
    SUBTITLES,
    THEME,
    LAYOUT,
    DOWNLOADS,
    GENERAL
}
