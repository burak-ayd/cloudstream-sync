# CloudStreamSync - Hızlı Başlangıç Rehberi

**Mevcut Durum:** Temel sync çalışıyor ama production-ready değil  
**Hedef:** sync-plugin seviyesine çıkmak  
**İlk Adım:** Kritik güvenlik açığını kapat

---

## 🚨 Acil: Güvenlik Açığını Kapat (Bugün)

### Problem
Mevcut kod **tüm ayarları** sync ediyor. Bu şunları içeriyor:
- `anilist_token` → Kullanıcının Anilist token'ı
- `mal_token` → MyAnimeList token'ı  
- `device_id` → Cihaza özel ID
- `download_path_key` → Lokal dosya yolu

Başka cihazda restore edilince:
- Token'lar çalınabilir (security risk)
- Device ID çakışır (crash)
- Path bulunamaz (crash)

### Çözüm: Non-Transferable Keys Filtresi

**1. Yeni dosya oluştur:** `SyncKeyClassifier.kt`

```kotlin
package com.cloudstreamsync.utils

object SyncKeyClassifier {
    private val nonTransferableKeys = listOf(
        // Auth tokens
        "anilist_token", "anilist_unixtime", "anilist_user",
        "mal_token", "mal_refresh_token", "mal_user",
        "simkl_token", "simkl_user",
        "open_subtitles", "subdl_user",
        
        // Device-specific
        "device_id", "biometric_key",
        "download_path_key", "backup_path_key",
        "download_path_key_visual", "backup_dir_path_key",
        
        // Sync metadata
        "sync_token", "sync_device_id",
        "CLOUDSYNC_WATCH_SYNC_CREDS",
        "CLOUDSYNC_APP_SETTINGS_SYNC_CREDS",
        "last_sync_api", "restore_device", "backup_device",
        
        // Local state
        "last_opened_id", "last_click_action",
        "download_info", "download_resume", "download_q_resume",
        "download_episode_cache",
        "FILES_TO_DELETE_KEY",
        
        // Other sensitive
        "nginx_user", "fshare_token", "bluphim_token"
    )
    
    fun isTransferable(key: String): Boolean {
        val lower = key.lowercase()
        return nonTransferableKeys.none { blocked ->
            lower.contains(blocked.lowercase())
        }
    }
}
```

**2. Güncelle:** `DataStoreHelper.kt`

```kotlin
// Eski getSettings fonksiyonu
private fun getSettings(context: Context): Map<String, String> {
    val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    val allPrefs = prefs.all
    val settings = mutableMapOf<String, String>()
    
    for (entry in allPrefs.entries) {
        // SORUN: Her şeyi alıyor
        settings[entry.key] = entry.value.toString()
    }
    
    return settings
}

// Yeni getSettings fonksiyonu
private fun getSettings(context: Context): Map<String, String> {
    val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    val allPrefs = prefs.all
    val settings = mutableMapOf<String, String>()
    
    for (entry in allPrefs.entries) {
        val key = entry.key
        val value = entry.value
        
        // ✅ FIX: Sadece transferable key'leri al
        if (!SyncKeyClassifier.isTransferable(key)) {
            continue
        }
        
        when (value) {
            is String -> settings[key] = value
            is Boolean -> settings[key] = value.toString()
            is Int -> settings[key] = value.toString()
            is Long -> settings[key] = value.toString()
            is Float -> settings[key] = value.toString()
        }
    }
    
    return settings
}

// Aynı fix'i bookmarks ve watchPositions için de uygula
private fun getAllBookmarksFullData(): List<String> {
    return try {
        val bookmarks = CS3DataStore.getAllBookmarkedData()
        bookmarks.map { bookmark ->
            // Bookmark'ları filtrele
            if (bookmark.id != null) {
                gson.toJson(bookmark)
            } else {
                null
            }
        }.filterNotNull()
    } catch (e: Exception) {
        emptyList()
    }
}
```

**3. Test et**

```kotlin
// Test dosyası: SyncKeyClassifierTest.kt
class SyncKeyClassifierTest {
    @Test
    fun testSecurityKeys() {
        assertFalse(SyncKeyClassifier.isTransferable("anilist_token"))
        assertFalse(SyncKeyClassifier.isTransferable("mal_refresh_token"))
        assertFalse(SyncKeyClassifier.isTransferable("device_id"))
        assertFalse(SyncKeyClassifier.isTransferable("download_path_key"))
        
        assertTrue(SyncKeyClassifier.isTransferable("player_speed"))
        assertTrue(SyncKeyClassifier.isTransferable("theme_color"))
    }
}
```

**Sonuç:** Artık tokenlar ve device-specific bilgiler sync edilmez. ✅

---

## 🎯 Bugün Yapılacaklar (2-3 saat)

### ✅ Checklist

- [ ] `SyncKeyClassifier.kt` oluştur (15 dk)
- [ ] `DataStoreHelper.kt` güncelle - `getSettings()` (15 dk)
- [ ] `DataStoreHelper.kt` güncelle - `getAllBookmarksFullData()` (10 dk)
- [ ] Test yaz - `SyncKeyClassifierTest.kt` (20 dk)
- [ ] Manuel test: 2 cihazda sync yap, token'ların sync olmadığını doğrula (30 dk)
- [ ] Commit & push (5 dk)

**Toplam:** ~1.5 saat

---

## 📅 Yarın Yapılacaklar (3-4 saat)

### Kategori Sistemi

sync-plugin'deki gibi kategoriye göre sync yapabilmek için:

**1. Enum'ları ekle**

```kotlin
// Yeni dosya: models/SyncCategory.kt
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
```

**2. SyncCategory classifier ekle**

```kotlin
// SyncKeyClassifier.kt'ye ekle
fun classifyKey(key: String): SyncCategory? {
    if (!isTransferable(key)) return null
    
    val lower = key.lowercase()
    
    return when {
        "result_favorites_state_data" in lower || "result_watch_state" in lower ->
            SyncCategory.BOOKMARKS
            
        "result_resume_watching" in lower || "video_pos_dur" in lower ->
            SyncCategory.RESUME_WATCHING
            
        "search_history" in lower ->
            SyncCategory.SEARCH_HISTORY
            
        "plugins_key" in lower || "repositories" in lower ->
            SyncCategory.EXTENSIONS
            
        else -> SyncCategory.SETTINGS
    }
}

fun classifySettingsKey(key: String): SettingsSubCategory {
    val lower = key.lowercase()
    
    return when {
        "player" in lower || "video" in lower || "buffer" in lower ->
            SettingsSubCategory.PLAYER
            
        "subtitle" in lower || "sub" in lower || "caption" in lower ->
            SettingsSubCategory.SUBTITLES
            
        "theme" in lower || "dark" in lower || "color" in lower ->
            SettingsSubCategory.THEME
            
        "layout" in lower || "view" in lower || "grid" in lower ->
            SettingsSubCategory.LAYOUT
            
        "download" in lower || "path" in lower ->
            SettingsSubCategory.DOWNLOADS
            
        else -> SettingsSubCategory.GENERAL
    }
}
```

**3. SyncData modelini güncelle**

```kotlin
// Eski
data class SyncData(
    val bookmarks: List<String> = emptyList(),
    val watchPositions: Map<String, Long> = emptyMap(),
    val searchHistory: List<String> = emptyList(),
    val extensions: List<String> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

// Yeni
data class SyncData(
    val categories: Map<String, CategoryData> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class CategoryData(
    val hash: String,
    val timestamp: Long,
    val data: Map<String, Any>  // JSON-serializable data
)
```

**4. DataStoreHelper'ı refactor et**

```kotlin
// Eski: collectCurrentData() -> tek SyncData
// Yeni: collectCategoryData(category: SyncCategory) -> CategoryData

fun collectCategoryData(context: Context, category: SyncCategory): CategoryData {
    val data = when (category) {
        SyncCategory.BOOKMARKS -> mapOf(
            "bookmarks" to getAllBookmarksFullData()
        )
        SyncCategory.RESUME_WATCHING -> mapOf(
            "watchPositions" to getAllWatchPositions()
        )
        SyncCategory.SEARCH_HISTORY -> mapOf(
            "searchHistory" to getSearchHistory(context)
        )
        SyncCategory.EXTENSIONS -> mapOf(
            "extensions" to getExtensions(context)
        )
        SyncCategory.SETTINGS -> mapOf(
            "settings" to getSettings(context)
        )
    }
    
    val json = Gson().toJson(data)
    val hash = computeHash(json)
    
    return CategoryData(
        hash = hash,
        timestamp = System.currentTimeMillis(),
        data = data
    )
}

private fun computeHash(data: String): String {
    val digest = MessageDigest.getInstance("MD5")
    val bytes = digest.digest(data.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
```

**Test:**
```kotlin
val bookmarksData = collectCategoryData(context, SyncCategory.BOOKMARKS)
println(bookmarksData.hash) // "a3f2e1..."
println(bookmarksData.timestamp) // 1726596537282
```

---

## 📅 Gelecek Hafta (Hafta 1)

### 1. SyncConfig (Granüler Kontrol)

Kullanıcı hangi kategorileri sync edeceğini seçebilsin:

```kotlin
data class SyncConfig(
    val backupBookmarks: Boolean = true,
    val backupResumeWatching: Boolean = true,
    val backupSearchHistory: Boolean = true,
    val backupExtensions: Boolean = true,
    
    val backupPlayer: Boolean = true,
    val backupSubtitles: Boolean = true,
    val backupTheme: Boolean = true,
    val backupLayout: Boolean = true,
    val backupDownloads: Boolean = true,
    val backupGeneral: Boolean = true,
    
    val restoreBookmarks: Boolean = true,
    val restoreResumeWatching: Boolean = true,
    // ... rest
)
```

**UI'de göster:**
```
☑ Favoriler
☑ İzleme Geçmişi
☐ Arama Geçmişi  <-- Kullanıcı istemezse sync etme
☑ Eklentiler

Ayarlar:
☑ Oynatıcı
☑ Altyazı
☐ Tema  <-- Kullanıcı istemezse sync etme
```

### 2. Timestamp-Based Merge

Her key için timestamp ekle, en yeni kazansın:

```kotlin
// Local value
{"value": "speed_1.5x", "timestamp": 1726596000000}

// Cloud value (daha yeni)
{"value": "speed_2.0x", "timestamp": 1726597000000}

// Merge result: Cloud wins
editor.putString(key, cloudValue)
```

### 3. Restore Guard

Restore sırasında değişiklikleri ignore et (sonsuz loop önleme):

```kotlin
object SyncManager {
    @Volatile private var isRestoring = false
    @Volatile private var restoringUntil = 0L
    
    fun markDirty(category: SyncCategory) {
        if (isRestoring || System.currentTimeMillis() < restoringUntil) {
            return  // Ignore
        }
        dirtyCategories.add(category)
    }
}
```

---

## 🎓 Öğrenme Kaynakları

### sync-plugin Kodu Anlamak

**En önemli dosyalar:**
1. `CloudSyncBackupUtils.java` (789 satır) - Core logic
   - `classifyKey()` - Key'leri kategorilere ayır
   - `getBackupForCategory()` - Kategori verisini topla
   - `restoreCategory()` - Merge & restore
   - `downloadAndLoadPlugins()` - Plugin download

2. `CloudSyncPlugin.java` (2195 satır) - Orchestration
   - `mergeAndSyncAllCategories()` - Diff & sync
   - `pullChangedCategories()` - Download & restore
   - `pushAllCategories()` - Upload
   - `startSseListener()` - Real-time sync

3. `AppSettingsSyncCreds.java` - Config
   - 32 boolean flag (her kategori için backup/restore)

**Okuma sırası:**
1. `SyncCategory.java` - Enum'ları anla
2. `CloudSyncBackupUtils.classifyKey()` - Nasıl sınıflandırılıyor?
3. `CloudSyncBackupUtils.getBackupForCategory()` - Nasıl toplanıyor?
4. `CloudSyncBackupUtils.restoreBackupVars()` - Nasıl merge ediliyor?
5. `CloudSyncPlugin.mergeAndSyncAllCategories()` - Workflow ne?

---

## 🧪 Test Senaryoları

### Senaryo 1: Token Güvenliği
1. Cihaz A'da Anilist login yap
2. Backup al
3. Cihaz B'de restore et
4. **Beklenen:** Cihaz B'de Anilist token YOK (çünkü non-transferable)
5. **Gerçek:** ??? (Test et!)

### Senaryo 2: Kategori Bazlı Sync
1. Cihaz A'da bookmark ekle
2. "Sadece bookmarks" sync et
3. Cihaz B'de restore et
4. **Beklenen:** Sadece bookmark geldi, ayarlar değişmedi
5. **Gerçek:** ??? (Şu an all-or-nothing)

### Senaryo 3: Conflict Resolution
1. Cihaz A: player_speed = "1.5x" (timestamp: 1000)
2. Cihaz B: player_speed = "2.0x" (timestamp: 2000)
3. Cihaz A sync çeksin
4. **Beklenen:** Cihaz A'da "2.0x" olmalı (B daha yeni)
5. **Gerçek:** ??? (Şu an son yazan kazanır, timestamp yok)

---

## 📈 İlerleme Metrikleri

### Bugün
- [ ] Non-transferable keys filtred → Security fix ✅

### Bu Hafta
- [ ] Kategori sistemi → Granüler sync ✅
- [ ] SyncConfig → User control ✅
- [ ] Timestamp merge → Conflict resolution ✅

### 2 Hafta
- [ ] Auto-sync → SharedPreferences listener ✅
- [ ] Lifecycle hooks → App açılınca sync ✅
- [ ] Debouncing → Performans ✅

### 4 Hafta
- [ ] Plugin download → Extension sync ✅
- [ ] Repository merge → Repo deduplication ✅

### 6 Hafta
- [ ] sync-plugin parity → Production-ready ✅

---

## 🚀 Hemen Başla

```bash
cd D:\Projeler\CloudStream\Sync\CloudStreamSync\src\main\kotlin\com\cloudstreamsync

# 1. Yeni klasör oluştur
mkdir utils

# 2. SyncKeyClassifier.kt oluştur
# (Yukarıdaki kodu kopyala)

# 3. DataStoreHelper.kt'yi güncelle
# (getSettings fonksiyonunu güncelle)

# 4. Build & test
cd ../../../../../../../
./gradlew :CloudStreamSync:build

# 5. Emülatörde test et
```

**Sonraki adım:** IMPLEMENTATION_PLAN.md'deki Faz 1'i takip et.

---

## ❓ Sorular?

1. **"Kategorileri nereden biliyorsun?"**  
   → sync-plugin kodu inceledim, key pattern'lerini analiz ettim

2. **"Timestamp merge nasıl çalışıyor?"**  
   → Her değer `{"value": "...", "timestamp": 123}` formatında saklanıyor  
   → Restore sırasında timestamp karşılaştırılıyor

3. **"Plugin download neden önemli?"**  
   → Extension isimleri sync olur ama plugin yüklü olmaz  
   → Kullanıcı manuel yüklemek zorunda kalır  
   → sync-plugin otomatik indirir + yükler

4. **"SSE ne işe yarıyor?"**  
   → Real-time sync için  
   → Cihaz A'da değişiklik olunca cihaz B otomatik çeker  
   → Şu an manuel sync gerekiyor

**Daha fazla soru:** KARSILASTIRMA_RAPORU.md ve IMPLEMENTATION_PLAN.md'ye bak.
