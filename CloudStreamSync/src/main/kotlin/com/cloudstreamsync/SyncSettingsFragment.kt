package com.cloudstreamsync

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.cloudstreamsync.models.SyncData
import com.lagradost.cloudstream3.plugins.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncSettingsFragment(private val plugin: Plugin) : DialogFragment() {
    
    private lateinit var providerSpinner: Spinner
    private lateinit var configContainer: LinearLayout
    private lateinit var uploadBtn: Button
    private lateinit var downloadBtn: Button
    private lateinit var deleteBtn: Button
    private lateinit var statusText: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        
        // Provider seçimi
        layout.addView(TextView(requireContext()).apply {
            text = "Bulut Servisi:"
            textSize = 16f
        })
        
        providerSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listOf("Supabase", "Google Drive (yakında)", "Firebase (yakında)")
            )
        }
        layout.addView(providerSpinner)
        
        // Config alanları
        configContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(configContainer)
        
        // Supabase config
        addConfigField("Supabase URL", "supabase_url")
        addConfigField("API Key", "supabase_key")
        addConfigField("Tablo Adı (opsiyonel)", "supabase_table")
        addConfigField("User ID", "supabase_user")
        
        // Upload butonu
        uploadBtn = Button(requireContext()).apply {
            text = "Buluta Yükle"
            setOnClickListener { uploadData() }
        }
        layout.addView(uploadBtn)
        
        // Download butonu
        downloadBtn = Button(requireContext()).apply {
            text = "Buluttan İndir"
            setOnClickListener { downloadData() }
        }
        layout.addView(downloadBtn)
        
        // Delete butonu
        deleteBtn = Button(requireContext()).apply {
            text = "Buluttan Sil"
            setOnClickListener { deleteData() }
        }
        layout.addView(deleteBtn)
        
        // Status
        statusText = TextView(requireContext()).apply {
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }
        layout.addView(statusText)
        
        return layout
    }
    
    private fun addConfigField(label: String, key: String) {
        configContainer.addView(TextView(requireContext()).apply {
            text = label
            setPadding(0, 20, 0, 5)
        })
        
        configContainer.addView(EditText(requireContext()).apply {
            hint = label
            tag = key
        })
    }
    
    private fun getConfig(): Map<String, String> {
        val config = mutableMapOf<String, String>()
        for (i in 0 until configContainer.childCount) {
            val view = configContainer.getChildAt(i)
            if (view is EditText && view.tag != null) {
                config[view.tag.toString()] = view.text.toString()
            }
        }
        return mapOf(
            "url" to (config["supabase_url"] ?: ""),
            "apiKey" to (config["supabase_key"] ?: ""),
            "table" to (config["supabase_table"]?.takeIf { it.isNotEmpty() } ?: "cloudstream_sync"),
            "userId" to (config["supabase_user"] ?: "default_user")
        )
    }
    
    private fun uploadData() {
        statusText.text = "Yükleniyor..."
        
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
                                "İzleme konumları: ${currentData.watchPositions.size}"
                        },
                        onFailure = { statusText.text = "✗ Hata: ${it.message}" }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "✗ Hata: ${e.message}"
                }
            }
        }
    }
    
    private fun downloadData() {
        statusText.text = "İndiriliyor..."
        
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
                                "Arama geçmişi: ${data.searchHistory.size}"
                        },
                        onFailure = { statusText.text = "✗ Hata: ${it.message}" }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "✗ Hata: ${e.message}"
                }
            }
        }
    }
    
    private fun deleteData() {
        statusText.text = "Siliniyor..."
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                SyncManager.setProvider(CloudProviderType.SUPABASE, getConfig())
                val result = SyncManager.deleteData()
                
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { statusText.text = "✓ Silme başarılı!" },
                        onFailure = { statusText.text = "✗ Hata: ${it.message}" }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "✗ Hata: ${e.message}"
                }
            }
        }
    }
}
