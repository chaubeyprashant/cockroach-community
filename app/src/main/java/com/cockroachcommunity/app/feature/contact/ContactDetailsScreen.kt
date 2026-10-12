package com.cockroachcommunity.app.feature.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.CockroachCommunityAvatar
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: ContactDetailsViewModel = hiltViewModel()
) {
    val trustedPeer by viewModel.trustedPeer.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val messageCount by viewModel.messageCount.collectAsStateWithLifecycle()
    val peerId = viewModel.peerId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Profile Section
            val persona = com.cockroachcommunity.app.core.common.SocialPersonaManager.getPersonaForId(peerId)
            val resolvedName = trustedPeer?.displayName?.ifBlank { persona.displayName } ?: persona.displayName
            val resolvedHandle = trustedPeer?.handle?.ifBlank { persona.handle } ?: persona.handle
            val resolvedBio = trustedPeer?.bio?.ifBlank { persona.bio } ?: persona.bio
            val resolvedEmoji = trustedPeer?.avatarEmoji?.ifBlank { persona.avatarEmoji } ?: persona.avatarEmoji

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.Space32),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CockroachCommunityAvatar(
                    name = resolvedName, 
                    modifier = Modifier.size(100.dp),
                    isOnline = isConnected,
                    badgeEmoji = resolvedEmoji
                )
                Spacer(modifier = Modifier.height(Dimens.Space16))
                Text(
                    text = resolvedName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = resolvedHandle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "❝ $resolvedBio ❞",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (isConnected) "● Direct P2P Active" else "Offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space4)
                    )
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space24),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionItem(
                    icon = Icons.Default.Message,
                    label = "Message",
                    onClick = { onNavigateToChat(peerId) }
                )
                // Calls not available natively unless connected, but we could just trigger the action if we had a dialer.
                // For now, it's just a non-functional action as requested if call isn't directly supported here.
                QuickActionItem(
                    icon = Icons.Default.Call,
                    label = "Call",
                    onClick = { /* Implement call flow later */ },
                    enabled = isConnected
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Space32))
            HorizontalDivider()

            // Info Section
            SettingsSectionTitle(title = "Information")
            
            InfoRow(icon = Icons.Default.Person, title = "Device Identifier", subtitle = peerId)
            
            val trustStatus = if (trustedPeer?.isTrusted == true) "Verified & Trusted" else "Not Trusted"
            InfoRow(
                icon = if (trustedPeer?.isTrusted == true) Icons.Default.Verified else Icons.Default.Security, 
                title = "Trust Status", 
                subtitle = trustStatus
            )

            if (trustedPeer != null) {
                val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                val lastSeen = dateFormat.format(Date(trustedPeer!!.lastSeenTimestamp))
                InfoRow(icon = Icons.Default.Info, title = "Last Seen Nearby", subtitle = lastSeen)
            }

            HorizontalDivider()

            SettingsSectionTitle(title = "Data & Storage")
            InfoRow(icon = Icons.Default.Message, title = "Messages Exchanged", subtitle = messageCount.toString())
            
            Spacer(modifier = Modifier.height(Dimens.Space32))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Dimens.Space16, top = Dimens.Space16, bottom = Dimens.Space8, end = Dimens.Space16)
    )
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(Dimens.Space16))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(Dimens.Space8))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
