package com.cockroachcommunity.app.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.CjpButton
import com.cockroachcommunity.app.core.designsystem.components.CjpOutlinedButton
import com.cockroachcommunity.app.core.designsystem.theme.Dimens

@Composable
fun LoginScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.Space24, vertical = Dimens.Space48),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🪳", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(Dimens.Space16))
        Text(
            "THE COCKROACH\nJANTA PARTY",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.Space8))
        Text(
            "Main Bhi Cockroach · मैं भी कॉकरोच",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.Space8))
        Text(
            "Five demands. Zero sponsors. One stubborn swarm.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.Space32))

        CjpButton(
            text = "Continue with Google",
            onClick = { viewModel.signInWithGoogle(context) },
            enabled = !isLoading,
            gold = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Dimens.Space12))
        CjpOutlinedButton(
            text = "Browse without an account",
            onClick = viewModel::continueAnonymously,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Dimens.Space12))
        Text(
            "Browsing is read-only. Sign in with Google to file complaints, support, comment, post and chat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        error?.let {
            Spacer(Modifier.height(Dimens.Space16))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
