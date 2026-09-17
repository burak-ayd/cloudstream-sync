# CloudStreamSync - İmplementasyon Planı

**Hedef:** sync-plugin seviyesine ulaşmak  
**Süre:** 4-6 hafta  
**Öncelik:** Critical eksiklikleri önce kapat

---

## 📅 Faz 1: Kritik Altyapı (Hafta 1-2)

### 1.1 Kategori Sistemi

**Yeni Dosya:** `SyncCategory.kt`
```kotlin
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

**Yeni Dosya:** `SyncConfig.kt`
```kotlin
data class SyncConfig(
    // Global
    val syncEnabled: Boolean = true,
    
    // Per-category backup
    val backupBookmarks: Boolean = true,
    val backupResumeWatching: Boolean = true,
    val backupSearchHistory: Boolean = true,
    val backupExtensions: Boolean = true,
    
    // Per-category restore
    val restoreBookmarks: Boolean = true,
    val restoreResumeWatching: Boolean = true,
    val restoreSearchHistory: Boolean = true,
    val restoreExtensions: Boolean = true,
    
    // Settings subcategories
    val backupPlayer: Boolean = true,
    val backupSubtitles: Boolean = true,
    val backupTheme: Boolean = true,
    val backupLayout: Boolean = true,
    val backupDownloads: Boolean = true,
    val backupGeneral: Boolean = true,
    
    val restorePlayer: Boolean = true,
    val restoreSubtitles: Boolean = true,
    val restoreTheme: Boolean = true,
    val restoreLayout: Boolean = true,
    val restoreDownloads: Boolean = true,
    val restoreGeneral: Boolean = true
)
```

**Güncelle:** `SyncData.kt`
```kotlin
// Eski: Flat structure
data class SyncData(
    val bookmarks: List<String>,
    val watchPositions: Map<String, Long>,
    val searchHistory: List<String>,
    val extensions: List<String>,
    val settings: Map<String, String>,
    val timestamp: Long
)

// Yeni: Category-based
data class SyncData(
    val categories: Map<SyncCategory, CategoryData>,
    val timestamp: Long
)

data class CategoryData(
    val hash: String,
    val timestamp: Long,
    val backup: BackupVars
)

data class BackupVars(
    val booleans: Map<String, Boolean>? = null,
    val integers: Map<String, Int>? = null,
    val strings: Map<String, String>? = null,
    val floats: Map<String, Float>? = null,
    val longs: Map<String, Long>? = null,
    val stringSets: Map<String, Set<String>>? = null
)
```

---

### 1.2 Non-Transferable Keys Filter

**Yeni Dosya:** `SyncKeyClassifier.kt`
```kotlin
object SyncKeyClassifier {
    private val nonTransferableKeys = listOf(
        // Auth tokens
        "anilist_token", "anilist_unixtime", "anilist_user",
        "mal_token", "mal_refresh_token", "mal_user",
        "simkl_token", "simkl_user",
        
        // Device-specific
        "device_id", "biometric_key",
        "download_path_key", "backup_path_key",
        "backup_dir_path_key",
        
        // Sync metadata
        "sync_token", "sync_device_id",
        "CLOUDSYNC_WATCH_SYNC_CREDS",
        "CLOUDSYNC_APP_SETTINGS_SYNC_CREDS",
        
        // Local state
        "last_opened_id", "last_click_action",
        "download_info", "download_resume",
        "FILES_TO_DELETE_KEY"
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
            "result_favorites_state_data" in lower || "result_watch_state" in lower ->
                SyncCategory.BOOKMARKS
                
            "result_resume_watching" in lower || "video_pos_dur" in lower ||
            "download_header_cache" in lower || "result_season" in lower ||
            "result_dub" in lower || "result_episode" in lower ->
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
            "player" in lower || "video" in lower || "buffer" in lower ||
            "skip" in lower || "gesture" in lower || "decoder" in lower ->
                SettingsSubCategory.PLAYER
                
            "subtitle" in lower || "sub" in lower || "caption" in lower ||
            "font" in lower ->
                SettingsSubCategory.SUBTITLES
                
            "theme" in lower || "dark" in lower || "color" in lower ||
            "accent" in lower ->
                SettingsSubCategory.THEME
                
            "layout" in lower || "view" in lower || "grid" in lower ||
            "home" in lower || "card" in lower ->
                SettingsSubCategory.LAYOUT
                
            "download" in lower || "path" in lower ->
                SettingsSubCategory.DOWNLOADS
                
            else -> SettingsSubCategory.GENERAL
        }
    }
}
```

---

### 1.3 Timestamp-Based Merge

**Güncelle:** `DataStoreHelper.kt` → `BackupManager.kt`
```kotlin
object BackupManager {
    
    fun collectCategoryData(
        context: Context,
        category: SyncCategory,
        config: SyncConfig
    ): CategoryData? {
        if (!isCategoryEnabled(category, config, isBackup = true)) {
            return null
        }
        
        val prefs = when (category) {
            SyncCategory.SETTINGS -> context.defaultSharedPreferences
            else -> context.getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        }
        
        val allKeys = prefs.all.keys
        val relevantKeys = allKeys.filter { key ->
            SyncKeyClassifier.classifyKey(key) == category &&
            isKeyEnabled(key, category, config, isBackup = true)
        }
        
        val backup = BackupVars(
            booleans = extractType<Boolean>(prefs, relevantKeys),
            integers = extractType<Int>(prefs, relevantKeys),
            strings = extractType<String>(prefs, relevantKeys),
            floats = extractType<Float>(prefs, relevantKeys),
            longs = extractType<Long>(prefs, relevantKeys),
            stringSets = extractType<Set<String>>(prefs, relevantKeys)
        )
        
        val json = Gson().toJson(backup)
        val hash = computeHash(json)
        
        return CategoryData(
            hash = hash,
            timestamp = System.currentTimeMillis(),
            backup = backup
        )
    }
    
    fun restoreCategoryData(
        context: Context,
        category: SyncCategory,
        cloudData: CategoryData,
        config: SyncConfig
    ): Boolean {
        if (!isCategoryEnabled(category, config, isBackup = false)) {
            return false
        }
        
        val prefs = when (category) {
            SyncCategory.SETTINGS -> context.defaultSharedPreferences
            else -> context.getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        }
        
        val editor = prefs.edit()
        var restoredAny = false
        
        // Merge logic with timestamp
        cloudData.backup.strings?.forEach { (key, cloudValue) ->
            if (!isKeyEnabled(key, category, config, isBackup = false)) return@forEach
            
            val localValue = prefs.getString(key, null)
            val cloudTs = extractTimestamp(cloudValue)
            val localTs = extractTimestamp(localValue)
            
            if (localValue == null || cloudTs > localTs || (cloudTs == 0L && localTs == 0L)) {
                editor.putString(key, cloudValue)
                restoredAny = true
            }
        }
        
        // Apply other types (boolean, int, float, long, stringSet)
        cloudData.backup.booleans?.forEach { (key, value) ->
            if (isKeyEnabled(key, category, config, isBackup = false)) {
                editor.putBoolean(key, value)
                restoredAny = true
            }
        }
        
        // ... repeat for other types
        
        editor.apply()
        return restoredAny
    }
    
    private fun extractTimestamp(value: String?): Long {
        if (value == null) return 0L
        
        // Regex: "timestamp":1234567890
        val match = Regex(""""timestamp"\s*:\s*(\d+)""").find(value)
        return match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }
    
    private fun computeHash(data: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(data.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

---

### 1.4 Restore Guard

**Güncelle:** `SyncManager.kt`
```kotlin
object SyncManager {
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
    
    fun markDirty(category: SyncCategory) {
        synchronized(dirtyCategoriesLock) {
            if (isRestoring || System.currentTimeMillis() < restoringUntil) {
                Log.d("SyncManager", "Ignoring change during restore: $category")
                return
            }
            dirtyCategories.add(category)
        }
    }
    
    suspend fun uploadCategory(category: SyncCategory): Result<Unit> = withContext(Dispatchers.IO) {
        val config = ConfigManager.loadSyncConfig(context!!)
        val categoryData = BackupManager.collectCategoryData(context!!, category, config)
            ?: return@withContext Result.failure(Exception("Category disabled or empty"))
        
        val syncData = SyncData(
            categories = mapOf(category to categoryData),
            timestamp = System.currentTimeMillis()
        )
        
        provider?.upload(syncData) ?: Result.failure(Exception("Provider not set"))
    }
    
    suspend fun downloadCategory(category: SyncCategory): Result<Unit> = withContext(Dispatchers.IO) {
        isRestoring = true
        restoringUntil = System.currentTimeMillis() + RESTORE_GUARD_MS
        
        try {
            val result = provider?.download() ?: return@withContext Result.failure(Exception("Provider not set"))
            
            result.fold(
                onSuccess = { syncData ->
                    val categoryData = syncData.categories[category]
                        ?: return@fold Result.failure<Unit>(Exception("Category not found"))
                    
                    val config = ConfigManager.loadSyncConfig(context!!)
                    val restored = BackupManager.restoreCategoryData(context!!, category, categoryData, config)
                    
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
}
```

---

## 📅 Faz 2: Auto-Sync (Hafta 3)

### 2.1 SharedPreferences Listener

**Yeni Dosya:** `AutoSyncManager.kt`
```kotlin
object AutoSyncManager {
    private var dataPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var defaultPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    
    private var pushJob: Job? = null
    private const val PUSH_DEBOUNCE_MS = 2000L
    
    fun startAutoSync(context: Context) {
        // DataStore listener
        val dataPrefs = context.getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        dataPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            onKeyChanged(key)
        }
        dataPrefs.registerOnSharedPreferenceChangeListener(dataPrefsListener)
        
        // Default prefs listener
        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        defaultPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            onKeyChanged(key)
        }
        defaultPrefs.registerOnSharedPreferenceChangeListener(defaultPrefsListener)
    }
    
    fun stopAutoSync(context: Context) {
        val dataPrefs = context.getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        dataPrefsListener?.let { dataPrefs.unregisterOnSharedPreferenceChangeListener(it) }
        
        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        defaultPrefsListener?.let { defaultPrefs.unregisterOnSharedPreferenceChangeListener(it) }
        
        pushJob?.cancel()
    }
    
    private fun onKeyChanged(key: String?) {
        if (key == null) return
        
        val category = SyncKeyClassifier.classifyKey(key) ?: return
        SyncManager.markDirty(category)
        
        schedulePush()
    }
    
    private fun schedulePush() {
        pushJob?.cancel()
        pushJob = GlobalScope.launch(Dispatchers.IO) {
            delay(PUSH_DEBOUNCE_MS)
            pushDirtyCategories()
        }
    }
    
    private suspend fun pushDirtyCategories() {
        val categories = synchronized(SyncManager.dirtyCategoriesLock) {
            val copy = SyncManager.dirtyCategories.toList()
            SyncManager.dirtyCategories.clear()
            copy
        }
        
        categories.forEach { category ->
            SyncManager.uploadCategory(category)
        }
    }
}
```

**Güncelle:** `CloudStreamSyncPlugin.kt`
```kotlin
@CloudstreamPlugin
class CloudStreamSyncPlugin : Plugin() {
    private var activity: AppCompatActivity? = null
    
    override fun load(context: Context) {
        activity = context as? AppCompatActivity
        
        SyncManager.init(context)
        AutoSyncManager.startAutoSync(context)
        
        openSettings = {
            val frag = SyncSettingsFragment(this)
            activity?.let {
                frag.show(it.supportFragmentManager, "CloudStreamSync")
            }
        }
    }
    
    override fun unload() {
        AutoSyncManager.stopAutoSync(context)
    }
}
```

---

### 2.2 Lifecycle-Aware Sync

**Güncelle:** `CloudStreamSyncPlugin.kt`
```kotlin
@CloudstreamPlugin
class CloudStreamSyncPlugin : Plugin() {
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    
    override fun load(context: Context) {
        // ... existing code
        
        val app = context.applicationContext as Application
        lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity.javaClass.simpleName == "MainActivity") {
                    GlobalScope.launch(Dispatchers.IO) {
                        SyncManager.downloadAllCategories()
                    }
                }
            }
            
            // Other lifecycle methods...
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }
    
    override fun unload() {
        lifecycleCallbacks?.let {
            val app = context.applicationContext as Application
            app.unregisterActivityLifecycleCallbacks(it)
        }
    }
}
```

---

## 📅 Faz 3: Plugin Management (Hafta 4)

### 3.1 Repository Merge

**Yeni Dosya:** `RepositoryManager.kt`
```kotlin
object RepositoryManager {
    
    suspend fun mergeRepositories(context: Context, cloudRepos: String) {
        try {
            val incomingRepos = Gson().fromJson<Array<RepositoryData>>(
                cloudRepos,
                Array<RepositoryData>::class.java
            )
            
            val currentRepos = getCurrentRepositories(context)
            
            // Merge & deduplicate
            val merged = (currentRepos + incomingRepos)
                .distinctBy { it.url.trim().lowercase() }
            
            // Save
            val json = Gson().toJson(merged)
            val prefs = context.defaultSharedPreferences
            prefs.edit()
                .putString("REPOSITORIES_KEY", json)
                .putString("plugins_repositories", json)
                .apply()
                
        } catch (e: Exception) {
            Log.e("RepositoryManager", "Merge failed: ${e.message}")
        }
    }
    
    private fun getCurrentRepositories(context: Context): List<RepositoryData> {
        val prefs = context.defaultSharedPreferences
        val json = prefs.getString("REPOSITORIES_KEY", null) ?: return emptyList()
        
        return try {
            Gson().fromJson(json, Array<RepositoryData>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class RepositoryData(
    val name: String,
    val url: String
)
```

---

### 3.2 Plugin Download

**Yeni Dosya:** `PluginDownloader.kt`
```kotlin
object PluginDownloader {
    
    suspend fun downloadAndLoadPlugins(context: Context, pluginNames: List<String>) = withContext(Dispatchers.IO) {
        val availablePlugins = getAvailablePlugins(context)
        
        pluginNames.forEach { name ->
            val plugin = availablePlugins.find { it.name == name }
            if (plugin != null) {
                downloadPlugin(context, plugin)
            }
        }
    }
    
    private fun getAvailablePlugins(context: Context): List<PluginData> {
        // Parse repositories and get plugin list
        // This requires integration with CloudStream's plugin system
        return emptyList() // placeholder
    }
    
    private suspend fun downloadPlugin(context: Context, plugin: PluginData) {
        // Download plugin file
        // Install to plugins directory
        // Load plugin
        // This requires CloudStream plugin manager access
    }
}

data class PluginData(
    val name: String,
    val url: String,
    val version: String
)
```

---

## 📅 Faz 4: UI İyileştirmeleri (Hafta 5-6)

### 4.1 Category Selection UI

**Güncelle:** `SyncSettingsFragment.kt`
```kotlin
class SyncSettingsFragment(private val plugin: Plugin) : DialogFragment() {
    
    private fun addCategoryCheckboxes(container: LinearLayout) {
        // Backup section
        addSectionHeader(container, "Yedeklenecek Kategoriler")
        
        addCheckbox(container, "bookmarks", "Favoriler", true)
        addCheckbox(container, "resume", "İzleme Geçmişi", true)
        addCheckbox(container, "search", "Arama Geçmişi", true)
        addCheckbox(container, "extensions", "Eklentiler", true)
        addCheckbox(container, "settings_player", "Ayarlar: Oynatıcı", true)
        addCheckbox(container, "settings_subtitles", "Ayarlar: Altyazı", true)
        addCheckbox(container, "settings_theme", "Ayarlar: Tema", true)
        
        // Restore section
        addSectionHeader(container, "Geri Yüklenecek Kategoriler")
        
        addCheckbox(container, "restore_bookmarks", "Favoriler", true)
        addCheckbox(container, "restore_resume", "İzleme Geçmişi", true)
        // ... repeat for restore
    }
    
    private fun addCheckbox(
        container: LinearLayout,
        key: String,
        label: String,
        defaultValue: Boolean
    ) {
        val checkbox = CheckBox(requireContext()).apply {
            text = label
            isChecked = loadCheckboxState(key, defaultValue)
            setOnCheckedChangeListener { _, isChecked ->
                saveCheckboxState(key, isChecked)
            }
        }
        container.addView(checkbox)
    }
}
```

---

## 🎯 Test Planı

### Birim Testler
```kotlin
class SyncKeyClassifierTest {
    @Test
    fun testNonTransferableKeys() {
        assertFalse(SyncKeyClassifier.isTransferable("anilist_token"))
        assertFalse(SyncKeyClassifier.isTransferable("device_id"))
        assertTrue(SyncKeyClassifier.isTransferable("player_speed"))
    }
    
    @Test
    fun testCategoryClassification() {
        assertEquals(
            SyncCategory.BOOKMARKS,
            SyncKeyClassifier.classifyKey("result_favorites_state_data_123")
        )
        assertEquals(
            SyncCategory.RESUME_WATCHING,
            SyncKeyClassifier.classifyKey("video_pos_dur/456/789")
        )
    }
}
```

### Entegrasyon Testleri
```kotlin
class BackupManagerTest {
    @Test
    fun testCategoryDataCollection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SyncConfig()
        
        val data = BackupManager.collectCategoryData(
            context,
            SyncCategory.BOOKMARKS,
            config
        )
        
        assertNotNull(data)
        assertNotNull(data?.hash)
        assertTrue(data!!.timestamp > 0)
    }
    
    @Test
    fun testTimestampMerge() {
        // Create old local value
        val prefs = context.getSharedPreferences("test", Context.MODE_PRIVATE)
        prefs.edit().putString("test_key", """{"value":"old","timestamp":1000}""").apply()
        
        // Restore newer cloud value
        val cloudData = CategoryData(
            hash = "abc",
            timestamp = 2000,
            backup = BackupVars(strings = mapOf("test_key" to """{"value":"new","timestamp":2000}"""))
        )
        
        BackupManager.restoreCategoryData(context, SyncCategory.SETTINGS, cloudData, SyncConfig())
        
        // Cloud should win
        assertEquals("""{"value":"new","timestamp":2000}""", prefs.getString("test_key", null))
    }
}
```

---

## 📊 İlerleme Takibi

| Faz | Özellik | Durum | Tarih |
|-----|---------|-------|-------|
| 1 | Kategori sistemi | ⏳ | - |
| 1 | SyncConfig | ⏳ | - |
| 1 | Non-transferable keys | ⏳ | - |
| 1 | Timestamp merge | ⏳ | - |
| 1 | Restore guard | ⏳ | - |
| 2 | SharedPreferences listener | ⏳ | - |
| 2 | Debouncing | ⏳ | - |
| 2 | Lifecycle hooks | ⏳ | - |
| 3 | Repository merge | ⏳ | - |
| 3 | Plugin download | ⏳ | - |
| 4 | Category selection UI | ⏳ | - |
| 4 | Progress tracking | ⏳ | - |

---

## ✅ Definition of Done

Bir özellik tamamlanmış sayılır:
- [ ] Kod yazıldı ve review edildi
- [ ] Birim testler %80+ coverage
- [ ] Entegrasyon testleri geçti
- [ ] Manuel test yapıldı (2 cihaz sync)
- [ ] Dokümantasyon güncellendi
- [ ] sync-plugin ile karşılaştırıldı ve aynı davranışı gösterdiği doğrulandı

---

## 🚀 Sonraki Adımlar

1. **Faz 1'i başlat** - Kategori sistemi ve non-transferable keys
2. **Test ortamı kur** - 2 emülatör + Supabase test DB
3. **Her özelliği test et** - sync-plugin davranışıyla karşılaştır
4. **İlerle** - Bir faz bittikten sonra diğerine geç

**Tahmini Süre:** 4-6 hafta (günde 3-4 saat)
