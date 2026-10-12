package com.cockroachcommunity.app.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CockroachCommunityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialization if needed
    }
}
