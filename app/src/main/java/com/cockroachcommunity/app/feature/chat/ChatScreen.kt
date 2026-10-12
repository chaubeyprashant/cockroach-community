package com.cockroachcommunity.app.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.common.SocialPersonaManager
import com.cockroachcommunity.app.core.database.entity.TransferEntity
import com.cockroachcommunity.app.core.designsystem.components.EmptyState
import com.cockroachcommunity.app.core.designsystem.components.MessageBubble
import com.cockroachcommunity.app.core.designsystem.components.CockroachCommunityAvatar
import com.cockroachcommunity.app.core.designsystem.components.CockroachCommunityTextField
import com.cockroachcommunity.app.core.designsystem.theme.BrandAccent
import com.cockroachcommunity.app.core.designsystem.theme.BrandPrimary
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.designsystem.theme.OnlineEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUICK_SPARKS = listOf(
    "👋 Hey there!",
    "⚡ What's up?",
    "🎧 Favorite songs right now?",
    "📸 Share a photo or snap!",
    "✨ Free to chat?",
    "☕ Coffee or tea?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerId: String,
    onNavigateToContactDetails: (String) -> Unit = {},
    onNavigateToCall: (Boolean) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val messageInput by viewModel.messageInput.collectAsStateWithLifecycle()
    val connectedPeerIdentity by viewModel.connectedPeerIdentity.collectAsStateWithLifecycle()
    val peerProfile by viewModel.peerProfile.collectAsStateWithLifecycle()
    val myId by viewModel.myId.collectAsStateWithLifecycle()

    val isOnline = connectedPeerIdentity != null

    val persona = remember(peerId) { SocialPersonaManager.getPersonaForId(peerId) }
    val displayName = peerProfile?.displayName?.ifBlank { persona.displayName } ?: persona.displayName
    val handle = peerProfile?.handle?.ifBlank { persona.handle } ?: persona.handle
    val bio = peerProfile?.bio?.ifBlank { persona.bio } ?: persona.bio
    val avatarEmoji = peerProfile?.avatarEmoji?.ifBlank { persona.avatarEmoji } ?: persona.avatarEmoji

    var showProfileSheet by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendFile(it) }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    var isSearchMode by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchMode) {
                        CockroachCommunityTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            placeholder = "Search messages...",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProfileSheet = true }
                                .padding(vertical = Dimens.Space4)
                        ) {
                            CockroachCommunityAvatar(
                                name = displayName,
                                size = 42.dp,
                                isOnline = isOnline,
                                badgeEmoji = avatarEmoji
                            )
                            Spacer(modifier = Modifier.width(Dimens.Space12))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = handle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) OnlineEmerald else Color(0xFF94A3B8))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isOnline) "Direct P2P Link Active ⚡" else "Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOnline) OnlineEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (!isSearchMode) {
                        val context = LocalContext.current
                        IconButton(onClick = { 
                            if (isOnline) onNavigateToCall(false) 
                            else android.widget.Toast.makeText(context, "Peer is not currently reachable", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { 
                            if (isOnline) onNavigateToCall(true) 
                            else android.widget.Toast.makeText(context, "Peer is not currently reachable", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.VideoCall, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { 
                        isSearchMode = !isSearchMode 
                        if (!isSearchMode) viewModel.updateSearchQuery("")
                    }) {
                        Icon(
                            imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchMode) "Close search" else "Search messages"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Interactive Spark Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QUICK_SPARKS.forEach { spark ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable { viewModel.sendQuickMessage(spark) }
                        ) {
                            Text(
                                text = spark,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach File")
                        }
                        
                        CockroachCommunityTextField(
                            value = messageInput,
                            onValueChange = viewModel::updateMessageInput,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            placeholder = "Message $displayName...",
                            singleLine = false
                        )
                        
                        val canSend = messageInput.isNotBlank()
                        val sendBg = if (canSend) {
                            Brush.horizontalGradient(listOf(BrandPrimary, BrandAccent))
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(sendBg)
                                .clickable(enabled = canSend) { viewModel.sendMessage() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val chatItems by viewModel.chatItems.collectAsStateWithLifecycle()
            if (chatItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CockroachCommunityAvatar(
                        name = displayName,
                        size = 80.dp,
                        isOnline = isOnline,
                        badgeEmoji = avatarEmoji
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "❝ $bio ❞",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        modifier = Modifier.clickable {
                            viewModel.sendQuickMessage("Hey $displayName! 👋 Sending a friendly wave!")
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Brush.horizontalGradient(listOf(BrandPrimary, BrandAccent)))
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WavingHand, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send a Wave 👋",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = Dimens.Space16, vertical = Dimens.Space16)
                ) {
                    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    
                    val groupedItems = chatItems.groupBy { 
                        dateFormat.format(Date(it.timestamp)) 
                    }
                    
                    groupedItems.entries.toList().forEach { (dateStr, itemsForDay) ->
                        items(itemsForDay, key = { it.id }) { item ->
                            val timeString = timeFormat.format(Date(item.timestamp))
                            
                            when (item) {
                                is ChatItem.MessageItem -> {
                                    val msg = item.message
                                    val isMe = msg.senderId == myId
                                    val statusText = when (msg.deliveryStatus) {
                                        0 -> "Sending..."
                                        1 -> "Sent"
                                        2 -> "Delivered"
                                        3 -> "Read"
                                        4 -> "Failed"
                                        else -> ""
                                    }
                                    
                                    MessageBubble(
                                        text = msg.content,
                                        isMine = isMe,
                                        timestamp = timeString,
                                        status = if (isMe) statusText else null,
                                        reaction = msg.reaction,
                                        onReact = { emoji -> viewModel.reactToMessage(msg.messageId, emoji) },
                                        onRetry = if (isMe && msg.deliveryStatus == 4) { { viewModel.retryMessage(msg) } } else null
                                    )
                                }
                                is ChatItem.TransferItem -> {
                                    val tr = item.transfer
                                    val isMe = tr.direction == 1 // 1 = Outgoing
                                    TransferBubble(
                                        transfer = tr,
                                        isMine = isMe,
                                        timestamp = timeString,
                                        onRetry = if (isMe && tr.status == 3) { { /* Optional retry logic */ } } else null
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Dimens.Space8))
                        }
                        
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimens.Space16),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space4)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Peer Social Profile Bottom Sheet
    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CockroachCommunityAvatar(
                    name = displayName,
                    size = 90.dp,
                    isOnline = isOnline,
                    badgeEmoji = avatarEmoji
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bio Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "❝ $bio ❞",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Security & P2P Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = OnlineEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Direct P2P Encrypted Mesh · 0 Relays",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnlineEmerald
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.sendQuickMessage("Hey $displayName! 👋 Sending a friendly wave!")
                                showProfileSheet = false
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WavingHand, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wave 👋",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showProfileSheet = false
                                onNavigateToContactDetails(peerId)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Details",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransferBubble(
    transfer: TransferEntity,
    isMine: Boolean,
    timestamp: String,
    onRetry: (() -> Unit)? = null
) {
    val progress = if (transfer.totalSize > 0) transfer.bytesTransferred.toFloat() / transfer.totalSize.toFloat() else 0f
    
    val statusText = when (transfer.status) {
        0 -> "Pending..."
        1 -> "Transferring... ${(progress * 100).toInt()}%"
        2 -> "Completed"
        3 -> "Failed"
        4 -> "Cancelled"
        else -> ""
    }
    
    val sizeText = formatFileSize(transfer.totalSize)
    
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val onBubbleColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    val shape = if (isMine) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space4),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(min = 200.dp, max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = onBubbleColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transfer.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onBubbleColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = onBubbleColor.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (transfer.status == 1 || transfer.status == 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = onBubbleColor.copy(alpha = 0.2f),
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = onBubbleColor.copy(alpha = 0.8f)
                    )
                    Row {
                        Text(
                            text = timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = onBubbleColor.copy(alpha = 0.6f)
                        )
                        if (onRetry != null && transfer.status == 3) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { onRetry() }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
