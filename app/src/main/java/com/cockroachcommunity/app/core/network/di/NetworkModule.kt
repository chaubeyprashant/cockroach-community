package com.cockroachcommunity.app.core.network.di

import android.content.Context
import com.cockroachcommunity.app.core.network.NetworkTransportManager
import com.cockroachcommunity.app.core.network.bluetooth.BluetoothDiscoveryManager
import com.cockroachcommunity.app.core.network.tcp.TcpClientManager
import com.cockroachcommunity.app.core.network.tcp.TcpServerManager
import com.cockroachcommunity.app.core.network.wifidirect.WifiDirectManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideBluetoothDiscoveryManager(@ApplicationContext context: Context): BluetoothDiscoveryManager {
        return BluetoothDiscoveryManager(context)
    }

    @Provides
    @Singleton
    fun provideWifiDirectManager(@ApplicationContext context: Context): WifiDirectManager {
        return WifiDirectManager(context)
    }
    
    @Provides
    @Singleton
    fun provideTcpServerManager(): TcpServerManager {
        return TcpServerManager()
    }
    
    @Provides
    @Singleton
    fun provideTcpClientManager(): TcpClientManager {
        return TcpClientManager()
    }

    @Provides
    @Singleton
    fun provideNetworkTransportManager(
        nearbyConnectionsManager: com.cockroachcommunity.app.core.network.nearby.NearbyConnectionsManager
    ): NetworkTransportManager {
        return NetworkTransportManager(nearbyConnectionsManager)
    }
}
