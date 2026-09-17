package com.cloudstreamsync

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@CloudstreamPlugin
class CloudStreamSyncPlugin : Plugin() {
    private var activity: AppCompatActivity? = null
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    
    override fun load(context: Context) {
        activity = context as? AppCompatActivity
        
        // Initialize sync manager
        SyncManager.init(context)
        
        // Start auto-sync
        AutoSyncManager.startAutoSync(context)
        
        // Register lifecycle callbacks
        val app = context.applicationContext as Application
        lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity.javaClass.simpleName == "MainActivity") {
                    // Auto-pull when MainActivity resumes
                    GlobalScope.launch(Dispatchers.IO) {
                        val result = SyncManager.downloadAllCategories()
                        result.fold(
                            onSuccess = { results ->
                                android.util.Log.d("CloudStreamSync", "Auto-pull successful: $results")
                            },
                            onFailure = {
                                android.util.Log.e("CloudStreamSync", "Auto-pull failed: ${it.message}")
                            }
                        )
                    }
                }
            }
            
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        
        openSettings = {
            val frag = SyncSettingsFragment(this)
            activity?.let {
                frag.show(it.supportFragmentManager, "CloudStreamSync")
            }
        }
    }
}
