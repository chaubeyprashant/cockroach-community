package com.cockroachcommunity.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.datastore.AppTheme
import com.cockroachcommunity.app.core.datastore.SettingsManager
import com.cockroachcommunity.app.core.designsystem.theme.CockroachCommunityTheme
import com.cockroachcommunity.app.ui.permissions.PermissionsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var settingsManager: SettingsManager

  @Inject
  lateinit var identityManager: com.cockroachcommunity.app.core.security.IdentityManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleAuthIntent(intent)

    enableEdgeToEdge()
    setContent {
      val themeState by settingsManager.themeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
      val darkTheme = when (themeState) {
          AppTheme.LIGHT -> false
          AppTheme.DARK -> true
          AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
      }
      val isSignedInState by identityManager.isSignedInFlow.collectAsStateWithLifecycle(initialValue = null)

      CockroachCommunityTheme(darkTheme = darkTheme) { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
            var permissionsGranted by remember { mutableStateOf(checkAllPermissions()) }

            LaunchedEffect(permissionsGranted) {
                if (permissionsGranted) {
                    try {
                        val intent = android.content.Intent(this@MainActivity, com.cockroachcommunity.app.core.service.CockroachCommunityService::class.java)
                        startService(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to start service", e)
                    }
                }
            }

            if (permissionsGranted) {
                if (isSignedInState != null) {
                    MainNavigation(isSignedIn = isSignedInState == true)
                }
            } else {
                PermissionsScreen(
                    onAllPermissionsGranted = {
                        permissionsGranted = true
                    }
                )
            }
        } 
      }
    }
  }

  private fun checkAllPermissions(): Boolean {
      val requiredPermissions = mutableListOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.CAMERA,
          Manifest.permission.RECORD_AUDIO
      ).apply {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              add(Manifest.permission.POST_NOTIFICATIONS)
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              add(Manifest.permission.BLUETOOTH_SCAN)
              add(Manifest.permission.BLUETOOTH_CONNECT)
              add(Manifest.permission.BLUETOOTH_ADVERTISE)
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              add(Manifest.permission.NEARBY_WIFI_DEVICES)
          }
      }
      return requiredPermissions.all {
          ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
      }
  }

  override fun onNewIntent(intent: android.content.Intent) {
      super.onNewIntent(intent)
      handleAuthIntent(intent)
  }

  private fun handleAuthIntent(intent: android.content.Intent?) {
      val regName = intent?.getStringExtra("auto_register_name")
      val regHandle = intent?.getStringExtra("auto_register_handle")
      val regEmoji = intent?.getStringExtra("auto_register_emoji") ?: "🚀"
      val regBio = intent?.getStringExtra("auto_register_bio") ?: "Vibing on offline mesh 🚀"
      if (!regName.isNullOrBlank() && !regHandle.isNullOrBlank()) {
          lifecycleScope.launch {
              identityManager.signUp(
                  displayName = regName,
                  handle = regHandle,
                  bio = regBio,
                  avatarEmoji = regEmoji,
                  password = ""
              )
          }
      }
  }
}
