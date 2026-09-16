package com.cloudstreamsync

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class CloudStreamSyncPlugin : Plugin() {
    private var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as? AppCompatActivity
        
        // Initialize sync manager
        SyncManager.init(context)
        
        openSettings = {
            val frag = SyncSettingsFragment(this)
            activity?.let {
                frag.show(it.supportFragmentManager, "CloudStreamSync")
            }
        }
    }
}
