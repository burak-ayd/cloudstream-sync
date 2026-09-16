dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

version = 6

cloudstream {
    description = "Multi-cloud sync for bookmarks, watch history, search history, extensions & settings"
    authors = listOf("CloudStreamSync")
    status = 3
    requiresResources = true
    language = "tr"
    iconUrl = "https://raw.githubusercontent.com/recloudstream/cloudstream/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}