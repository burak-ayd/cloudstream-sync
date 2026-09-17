# CloudStream Sync - Karşılaştırma Raporu

**Tarih:** 2026-09-17  
**Karşılaştırma:** `sync-plugin/` (çalışan) vs `CloudStreamSync/` (geliştirme)

---

## 🎯 Özet

Çalışan kod (sync-plugin) **production-ready, enterprise-grade** bir sync sistemi. Mevcut kodumuz (CloudStreamSync) ise **MVP/prototype** seviyesinde.

---

## 📊 Kritik Eksiklikler

### 1. **Kategori Bazlı Senkronizasyon** ❌

**sync-plugin:**
```java
enum SyncCategory {
    EXTENSIONS,
    SETTINGS,
    BOOKMARKS,
    RESUME_WATCHING,
    SEARCH_HISTORY
}

enum SettingsSubCategory {
    PLAYER, SUBTITLES, THEME, LAYOUT, DOWNLOADS, GENERAL
}
```

**CloudStreamSync:**
```kotlin
// Tek bir SyncData objesi - kategori ayrımı yok
data class SyncData(
    val bookmarks: List<String>,
    val watchPositions: Map<String, Long>,
    val searchHistory: List<String>,
    val extensions: List<String>,
    val settings: Map<String, String>
)
```

**Etki:** Kullanıcı "sadece bookmarks sync et" diyemez. Her sync'te her şey gider.

---

### 2. **Granüler Backup/Restore Kontrolü** ❌

**sync-plugin:**
```java
class AppSettingsSyncCreds {
    boolean backupBookmarks;
    boolean backupResumeWatching;
    boolean backupSearchHistory;
    boolean backupExtensions;
    boolean backupPlayer;
    boolean backupSubtitles;
    boolean backupTheme;
    boolean backupLayout;
    boolean backupDownloads;
    boolean backupGeneral;
    
    boolean restoreBookmarks;
    boolean restoreResumeWatching;
    // ... her kategori için ayrı restore flag
}
```

**CloudStreamSync:**
```kotlin
// Hiç yok - all-or-nothing sync
```

**Etki:** Kullanıcı "player settings'i sync etme ama theme'yi et" diyemez.

---

### 3. **Otomatik Senkronizasyon & SSE** ❌

**sync-plugin:**
```java
class CloudSyncPlugin {
    // Real-time sync via Server-Sent Events
    private void startSseListener(Context context, SyncCategory category) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url(firebaseUrl + "/devices/" + deviceId + "/events")
            .build();
        
        sseCall = client.newCall(request);
        sseCall.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                // Parse SSE events
                // Trigger pullChangedCategories()
            }
        });
    }
    
    // Auto-push on data change
    private SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> {
        SyncCategory category = classifyKey(key);
        markDirty(category);
        schedulePush(); // Debounced
    };
}
```

**CloudStreamSync:**
```kotlin
// Hiç yok - sadece manuel sync
```

**Etki:** Kullanıcı her cihazda manuel sync yapmalı. Multi-device gerçek zamanlı sync yok.

---

### 4. **Akıllı Merge & Conflict Resolution** ❌

**sync-plugin:**
```java
class CloudSyncBackupUtils {
    // Her key için timestamp-based merge
    private void restoreBackupVars(Context context, SyncCategory category, BackupVars vars) {
        for (Entry<String, String> entry : vars.getString().entrySet()) {
            String localVal = prefs.getString(key, null);
            long cloudTs = extractTimestamp(cloudVal);
            long localTs = extractTimestamp(localVal);
            
            if (cloudTs > localTs) {
                editor.setKeyRaw(key, cloudVal); // Cloud wins
            } else {
                // Keep local
            }
        }
    }
    
    // Dynamic categories: diff-based sync
    private boolean isDynamicCategory(SyncCategory cat) {
        return cat == BOOKMARKS || cat == RESUME_WATCHING || cat == SEARCH_HISTORY;
    }
}
```

**CloudStreamSync:**
```kotlin
fun applyData(context: Context, data: SyncData) {
    // Direkt overwrite - son yazan kazanır
    data.bookmarks.forEach { bookmarkJson ->
        val bookmark = gson.fromJson(bookmarkJson, BookmarkedData::class.java)
        CS3DataStore.setBookmarkedData(bookmark.id, bookmark)
    }
}
```

**Etki:** Kullanıcı cihaz A'da bookmark eklerse, cihaz B'den sync çekince A'daki bookmark kaybolabilir.

---

### 5. **Non-Transferable Keys Filtresi** ❌

**sync-plugin:**
```java
private static final List<String> nonTransferableKeys = listOf(
    "anilist_token", "mal_token", "simkl_token",  // Auth tokens
    "device_id", "biometric_key",                  // Device-specific
    "download_path_key", "backup_path_key",        // Local paths
    "sync_token", "sync_device_id"                 // Sync metadata
);

private boolean isTransferable(String key) {
    String lower = key.toLowerCase();
    return !nonTransferableKeys.any(blocked -> lower.contains(blocked.toLowerCase()));
}
```

**CloudStreamSync:**
```kotlin
// Hiç yok - tüm settings sync olur
```

**Etki:** Token'lar, device ID'ler, local path'ler sync olabilir → güvenlik riski & crash.

---

### 6. **Plugin Download & Load** ❌

**sync-plugin:**
```java
class CloudSyncBackupUtils {
    // Extensions restore'da otomatik plugin download
    suspend fun restoreExtensionsCategory(Context context, BackupFile backupFile) {
        mergeRepositories(context, backupFile);
        downloadAndLoadPlugins(context, backupFile); // <- Kritik
    }
    
    private suspend fun downloadAndLoadPlugins(Context context, BackupFile backupFile) {
        val pluginsList = parsePluginsJson(backupFile);
        val allOnlinePlugins = RepositoryManager.getAvailablePlugins();
        
        pluginsList.forEach { pluginName ->
            val onlinePlugin = allOnlinePlugins.find { it.name == pluginName };
            if (onlinePlugin != null) {
                downloadPlugin(onlinePlugin.url);
                loadPlugin(onlinePlugin);
            }
        }
    }
}
```

**CloudStreamSync:**
```kotlin
// Sadece extension isimleri sync olur - download yok
private fun setExtensions(context: Context, extensions: List<String>) {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(USER_PROVIDER_API, gson.toJson(extensions)).apply()
}
```

**Etki:** Extension listesi sync olur ama pluginler yüklü olmaz. Kullanıcı manuel yüklemeli.

---

### 7. **Restore Guard (Race Condition Önleme)** ❌

**sync-plugin:**
```java
class CloudSyncPlugin {
    private volatile boolean isRestoring;
    private volatile long restoringUntil;
    private final long RESTORE_GUARD_MS = 5000;
    
    private void markDirty(SyncCategory category) {
        if (isRestoring || System.currentTimeMillis() < restoringUntil) {
            Log.d(TAG, "Ignoring change during restore: " + category);
            return; // Restore sırasında local change'leri ignore et
        }
        dirtyCategories.add(category);
    }
}
```

**CloudStreamSync:**
```kotlin
// Hiç yok
```

**Etki:** Restore sırasında local değişiklikler dirty olarak işaretlenir → restore sonrası hemen push → sonsuz loop riski.

---

### 8. **Lifecycle-Aware Sync** ❌

**sync-plugin:**
```java
class CloudSyncPlugin extends Plugin {
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    
    @Override
    public void load(Context context) {
        // Activity lifecycle'a bağla
        Application app = (Application) context.getApplicationContext();
        lifecycleCallbacks = new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                if (activity instanceof MainActivity) {
                    pullChangedCategories(context, false); // Uygulama açılınca auto-pull
                }
            }
        };
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }
}
```

**CloudStreamSync:**
```kotlin
// Lifecycle tracking yok - sadece Fragment açılınca çalışır
```

**Etki:** Uygulama açılınca otomatik sync olmaz. Kullanıcı manual tetiklemeli.

---

### 9. **WebView Bazlı Settings UI** ❌

**sync-plugin:**
```java
class CloudSyncSettings {
    fun show(context: Context) {
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(object {
                @JavascriptInterface
                fun syncNow() { /* trigger sync */ }
                
                @JavascriptInterface
                fun fetchCloudPreview() { /* fetch data */ }
            }, "Android")
        }
        
        webView.loadDataWithBaseURL(null, generateHtml(), "text/html", "UTF-8", null)
    }
    
    private fun generateHtml(): String {
        // Embedded HTML/CSS/JS for rich UI
        // Checkboxes for each category
        // Real-time status updates
    }
}
```

**CloudStreamSync:**
```kotlin
class SyncSettingsFragment : DialogFragment() {
    // Native Android UI - basit ama sınırlı
    override fun onCreateView(...): View {
        val mainLayout = LinearLayout(requireContext()).apply {
            // Hardcoded UI elements
        }
    }
}
```

**Etki:** UI değişiklikleri için recompile gerekir. WebView ile HTML/CSS güncellemeleri daha esnek.

---

### 10. **Firebase Integration** ❌

**sync-plugin:**
```java
data class FirebaseSharedData(
    val devices: Map<String, FirebaseDevice>,
    val manifest: SyncManifest
)

data class FirebaseDevice(
    val id: String,
    val name: String,
    val lastSeen: Long
)

data class SyncManifest(
    val categories: Map<String, SyncCategoryMeta>
)

data class SyncCategoryMeta(
    val hash: String,
    val timestamp: Long,
    val device: String
)
```

**CloudStreamSync:**
```kotlin
// Sadece Supabase - Firebase yok
```

**Etki:** Multi-device sync, device listesi, per-device manifest yok.

---

## 🛠 Teknik Eksiklikler

### 11. **Debouncing & Throttling** ❌

**sync-plugin:**
```java
private Job pushJob;
private final long PUSH_DEBOUNCE_MS = 2000;

private void schedulePush() {
    if (pushJob != null) {
        pushJob.cancel();
    }
    
    pushJob = pluginScope.launch {
        delay(PUSH_DEBOUNCE_MS);
        pushAllCategories(context);
    };
}
```

**CloudStreamSync:** Yok

---

### 12. **Mutex & Concurrency Control** ❌

**sync-plugin:**
```java
private final Mutex syncMutex = Mutex();
private final Mutex pullMutex = Mutex();

suspend fun mergeAndSyncAllCategories(Context context) {
    syncMutex.withLock {
        // Simultaneous sync'leri engelle
    }
}
```

**CloudStreamSync:** Yok

---

### 13. **Retry Logic & Exponential Backoff** ❌

**sync-plugin:**
```java
private volatile int sseRetryCount;
private final long SSE_BASE_DELAY_MS = 5000;
private final long SSE_MAX_BACKOFF_MS = 60000;

private void startSseListener() {
    try {
        // Connect to SSE
    } catch (Exception e) {
        long delay = Math.min(SSE_BASE_DELAY_MS * (1 << sseRetryCount), SSE_MAX_BACKOFF_MS);
        sseRetryCount++;
        delay(delay);
        startSseListener(); // Retry
    }
}
```

**CloudStreamSync:** Yok

---

### 14. **Type-Safe SharedPreferences** ❌

**sync-plugin:**
```java
class CloudSyncEditor {
    fun <T> setKeyRaw(path: String, value: T) {
        when (value) {
            is Boolean -> editor.putBoolean(path, value)
            is Int -> editor.putInt(path, value)
            is String -> editor.putString(path, value)
            is Float -> editor.putFloat(path, value)
            is Long -> editor.putLong(path, value)
            is Set<*> -> editor.putStringSet(path, value as Set<String>)
        }
    }
}
```

**CloudStreamSync:**
```kotlin
// Settings'leri String'e çevirerek kaydediyor - tip bilgisi kayboluyor
settings[key] = value.toString()
```

---

### 15. **Migration Support** ❌

**sync-plugin:**
```java
suspend fun migrateFromV1(Context context) {
    val oldData = DataStore.getKey("CLOUDSYNC_OLD_DATA");
    if (oldData != null) {
        // V1 formatından V2'ye migrate et
        val newFormat = convertV1ToV2(oldData);
        saveV2Data(newFormat);
    }
}
```

**CloudStreamSync:** Yok

---

## 📋 Eksik Özellikler Listesi

| Özellik | sync-plugin | CloudStreamSync | Öncelik |
|---------|-------------|-----------------|---------|
| Kategori bazlı sync | ✅ | ❌ | 🔴 Critical |
| Granüler backup/restore | ✅ | ❌ | 🔴 Critical |
| Otomatik sync (SSE) | ✅ | ❌ | 🟡 High |
| Timestamp-based merge | ✅ | ❌ | 🔴 Critical |
| Non-transferable keys | ✅ | ❌ | 🔴 Critical |
| Plugin download/load | ✅ | ❌ | 🟡 High |
| Restore guard | ✅ | ❌ | 🟡 High |
| Lifecycle-aware sync | ✅ | ❌ | 🟢 Medium |
| WebView UI | ✅ | ❌ | 🟢 Medium |
| Firebase support | ✅ | ❌ | 🟢 Medium |
| Debouncing | ✅ | ❌ | 🟡 High |
| Mutex/concurrency | ✅ | ❌ | 🟡 High |
| Retry/backoff | ✅ | ❌ | 🟡 High |
| Type-safe prefs | ✅ | ❌ | 🟢 Medium |
| Migration | ✅ | ❌ | 🟢 Medium |

---

## 🎯 Öneriler

### Kısa Vadeli (1-2 hafta)
1. **SyncCategory enum ekle** - Kategoriye göre sync
2. **AppSettingsSyncCreds ekle** - Granüler kontrol
3. **nonTransferableKeys filtresi** - Güvenlik
4. **Timestamp-based merge** - Conflict resolution
5. **Restore guard** - Race condition önleme

### Orta Vadeli (2-4 hafta)
6. Plugin download/load desteği
7. SharedPreferences listener + auto-sync
8. Lifecycle-aware sync
9. Debouncing & mutex
10. Retry logic

### Uzun Vadeli (1-2 ay)
11. Firebase integration
12. SSE real-time sync
13. WebView UI
14. Migration system

---

## 💡 Sonuç

Mevcut `CloudStreamSync` kodu **proof-of-concept** olarak çalışıyor ama **production'a hazır değil**. 

`sync-plugin` kodu **years of battle-testing** görmüş, edge case'leri handle eden, enterprise-grade bir sistem.

**Öneri:** `sync-plugin` mimarisini kopyala, eksik özellikleri bire bir implement et.
