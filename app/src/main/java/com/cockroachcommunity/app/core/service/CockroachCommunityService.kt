package com.cockroachcommunity.app.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cockroachcommunity.app.core.network.nearby.NearbyConnectionsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CockroachCommunityService : Service() {

    @Inject
    lateinit var nearbyConnectionsManager: NearbyConnectionsManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            nearbyConnectionsManager.startAdvertising()
        } catch (e: Exception) {
            android.util.Log.e("CockroachCommunityService", "Error starting advertising", e)
        }
        
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "${getString(com.cockroachcommunity.app.R.string.app_name)} Background Service"
            val descriptionText = "Maintains Wi-Fi Direct and Bluetooth connections in the background."
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "cockroach_community_service_channel"
        private const val NOTIFICATION_ID = 1
    }
}
