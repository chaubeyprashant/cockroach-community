package com.cockroachcommunity.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.LoadingBox
import com.cockroachcommunity.app.core.designsystem.theme.CockroachCommunityTheme
import com.cockroachcommunity.app.core.session.Session
import com.cockroachcommunity.app.core.session.SessionRepository
import com.cockroachcommunity.app.feature.auth.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionRepository: SessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val session by sessionRepository.session.collectAsStateWithLifecycle(initialValue = Session.Loading)
            CockroachCommunityTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (session) {
                        Session.Loading -> LoadingBox(Modifier.statusBarsPadding())
                        Session.SignedOut -> LoginScreen()
                        else -> MainNavigation()
                    }
                }
            }
        }
    }
}
