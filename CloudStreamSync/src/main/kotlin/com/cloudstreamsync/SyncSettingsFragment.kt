package com.cloudstreamsync

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import com.lagradost.cloudstream3.plugins.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncSettingsFragment(private val plugin: Plugin) : DialogFragment() {
    
    private lateinit var configContainer: LinearLayout
    private lateinit var uploadBtn: Button
    private lateinit var downloadBtn: Button
    private lateinit var deleteBtn: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48)
        }
        
        // Başlık
        mainLayout.addView(TextView(requireContext()).apply {
            text = "CloudStream Sync"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })
        
        // Provider başlık
        mainLayout.addView(TextView(requireContext()).apply {
            text = "Bulut Servisi"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 8)
        })
        
        // Provider card
        mainLayout.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            
            addView(TextView(requireContext()).apply {
                text = "Supabase"
                textSize = 16f
                setTextColor(Color.parseColor("#4CAF50"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })
            
            addView(TextView(requireContext()).apply {
                text = "✓"
                textSize = 20f
                setTextColor(Color.parseColor("#4CAF50"))
            })
        })
        
        // Config başlık
        mainLayout.addView(TextView(requireContext()).apply {
            text = "Ayarlar"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 8)
        })
        
        // Config container
        configContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(24, 16, 24, 16)
        }
        mainLayout.addView(configContainer)
        
        // Config fields
        addConfigField("Supabase URL", "supabase_url", "https://xxx.supabase.co")
        addConfigField("API Key", "supabase_key", "eyJhbG...")
        addConfigField("Tablo Adı", "supabase_table", "cloudstream_sync")
        addConfigField("User ID", "supabase_user", "user_123")
        
        // Kaydedilmiş config'i yükle
        loadSavedConfig()
        
        // Progress bar
        progressBar = ProgressBar(requireContext()).apply {
            visibility = View.GONE
            setPadding(0, 24, 0, 24)
        }
        mainLayout.addView(progressBar)
        
        // İşlemler başlık
        mainLayout.addView(TextView(requireContext()).apply {
            text = "İşlemler"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 8)
        })
        
        // Upload butonu
        uploadBtn = Button(requireContext()).apply {
            text = "⬆ Buluta Yükle"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            setOnClickListener { uploadData() }
        }
        mainLayout.addView(uploadBtn)
        
        // Download butonu
        downloadBtn = Button(requireContext()).apply {
            text = "⬇ Buluttan İndir"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
            setOnClickListener { downloadData() }
        }
        mainLayout.addView(downloadBtn)
        
        // Delete butonu
        deleteBtn = Button(requireContext()).apply {
            text = "🗑 Buluttan Sil"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#F44336"))
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
            setOnClickListener { deleteData() }
        }
        mainLayout.addView(deleteBtn)
        
        // Status
        statusText = TextView(requireContext()).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        mainLayout.addView(statusText)
        
        scrollView.addView(mainLayout)
        return scrollView
    }
    
    private fun addConfigField(label: String, key: String, hint: String) {
        configContainer.addView(TextView(requireContext()).apply {
            text = label
            textSize = 14f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, if (configContainer.childCount > 0) 16 else 0, 0, 4)
        })
        
        configContainer.addView(EditText(requireContext()).apply {
            this.hint = hint
            tag = key
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#666666"))
            setBackgroundColor(Color.parseColor("#2A2A2A"))
            setPadding(16)
        })
    }
    
    private fun loadSavedConfig() {
        val savedConfig = ConfigManager.loadConfig(requireContext())
        
        for (i in 0 until configContainer.childCount) {
            val view = configContainer.getChildAt(i)
            if (view is EditText && view.tag != null) {
                when (view.tag.toString()) {
                    "supabase_url" -> view.setText(savedConfig["url"])
                    "supabase_key" -> view.setText(savedConfig["apiKey"])
                    "supabase_table" -> view.setText(savedConfig["table"])
                    "supabase_user" -> view.setText(savedConfig["userId"])
                }
            }
        }
    }
    
    private fun getConfig(): Map<String, String> {
        val config = mutableMapOf<String, String>()
        for (i in 0 until configContainer.childCount) {
            val view = configContainer.getChildAt(i)
            if (view is EditText && view.tag != null) {
                config[view.tag.toString()] = view.text.toString()
            }
        }
        
        val finalConfig = mapOf(
            "url" to (config["supabase_url"] ?: ""),
            "apiKey" to (config["supabase_key"] ?: ""),
            "table" to (config["supabase_table"]?.takeIf { it.isNotEmpty() } ?: "cloudstream_sync"),
            "userId" to (config["supabase_user"] ?: "default_user")
        )
        
        // Config'i kaydet
        ConfigManager.saveConfig(requireContext(), finalConfig)
        
        return finalConfig
    }
    
    private fun setLoading(isLoading: Boolean) {
        uploadBtn.isEnabled = !isLoading
        downloadBtn.isEnabled = !isLoading
        deleteBtn.isEnabled = !isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun uploadData() {
        statusText.text = "Yükleniyor..."
        statusText.setTextColor(Color.WHITE)
        setLoading(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                SyncManager.setProvider(CloudProviderType.SUPABASE, getConfig())
                
                // Gerçek CloudStream verisini topla
                val currentData = DataStoreHelper.collectCurrentData(requireContext())
                
                val result = SyncManager.uploadData(currentData)
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { 
                            statusText.text = "✓ Yükleme başarılı!\n" +
                                "Favoriler: ${currentData.bookmarks.size}\n" +
                                "İzleme konumları: ${currentData.watchPositions.size}\n" +
                                "Arama geçmişi: ${currentData.searchHistory.size}\n" +
                                "Eklentiler: ${currentData.extensions.size}\n" +
                                "Ayarlar: ${currentData.settings.size}"
                            statusText.setTextColor(Color.parseColor("#4CAF50"))
                        },
                        onFailure = { 
                            statusText.text = "✗ Hata: ${it.message}"
                            statusText.setTextColor(Color.parseColor("#F44336"))
                        }
                    )
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "✗ Hata: ${e.message}"
                    statusText.setTextColor(Color.parseColor("#F44336"))
                    setLoading(false)
                }
            }
        }
    }
    
    private fun downloadData() {
        statusText.text = "İndiriliyor..."
        statusText.setTextColor(Color.WHITE)
        setLoading(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                SyncManager.setProvider(CloudProviderType.SUPABASE, getConfig())
                val result = SyncManager.downloadData()
                
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { data ->
                            // Veriyi CloudStream'e uygula
                            DataStoreHelper.applyData(requireContext(), data)
                            
                            statusText.text = "✓ İndirme başarılı!\n" +
                                "Favoriler: ${data.bookmarks.size}\n" +
                                "İzleme konumları: ${data.watchPositions.size}\n" +
                                "Arama geçmişi: ${data.searchHistory.size}\n" +
                                "Eklentiler: ${data.extensions.size}\n" +
                                "Ayarlar: ${data.settings.size}\n\n" +
                                "Uygulamayı yeniden başlatın!"
                            statusText.setTextColor(Color.parseColor("#4CAF50"))
                        },
                        onFailure = { 
                            statusText.text = "✗ Hata: ${it.message}"
                            statusText.setTextColor(Color.parseColor("#F44336"))
                        }
                    )
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "✗ Hata: ${e.message}"
                    statusText.setTextColor(Color.parseColor("#F44336"))
                    setLoading(false)
                }
            }
        }
    }
    
    private fun deleteData() {
        statusText.text = "Siliniyor..."
        statusText.setTextColor(Color.WHITE)
        setLoading(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                SyncManager.setProvider(CloudProviderType.SUPABASE, getConfig())
                val result = SyncManager.deleteData()
                
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { 
                            statusText.text = "✓ Silme başarılı!"
                            statusText.setTextColor(Color.parseColor("#4CAF50"))
                        },
                        onFailure = { 
                            statusText.text = "✗ Hata: ${it.message}"
                            statusText.setTextColor(Color.parseColor("#F44336"))
                        }
                    )
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "✗ Hata: ${e.message}"
                    statusText.setTextColor(Color.parseColor("#F44336"))
                    setLoading(false)
                }
            }
        }
    }
}
