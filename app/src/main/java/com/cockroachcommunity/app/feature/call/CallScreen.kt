package com.cockroachcommunity.app.feature.call

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.designsystem.components.CockroachCommunityAvatar
import com.cockroachcommunity.app.core.designsystem.theme.Dimens
import com.cockroachcommunity.app.core.network.repository.CallState
import com.cockroachcommunity.app.core.database.entity.CallHistoryEntity
import org.webrtc.SurfaceViewRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    autoStartCall: Boolean = false,
    isVideoCall: Boolean = true,
    viewModel: CallViewModel = hiltViewModel()
) {
    var hasAutoStarted by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    
    // We will conditionally render the CallHistory UI or the Active/Incoming Call UI
    if (callState == CallState.IDLE) {
        CallHistoryUI(
            viewModel = viewModel, 
            autoStartCall = autoStartCall, 
            isVideoCall = isVideoCall,
            hasAutoStarted = hasAutoStarted,
            onAutoStartCompleted = { hasAutoStarted = true }
        )
    } else {
        ActiveCallUI(viewModel, callState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryUI(
    viewModel: CallViewModel, 
    autoStartCall: Boolean = false, 
    isVideoCall: Boolean = true,
    hasAutoStarted: Boolean = false,
    onAutoStartCompleted: () -> Unit = {}
) {
    val callHistory by viewModel.callHistory.collectAsStateWithLifecycle()
    val connectedPeerIdentity by viewModel.connectedPeerIdentity.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Camera & Mic Permissions launcher for when we try to initiate a call
    val permissionsToRequest = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            if (connectedPeerIdentity == null) {
                android.widget.Toast.makeText(context, "No active connection", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                viewModel.startCall(isVideoCall)
            }
        }
    }
    
    LaunchedEffect(autoStartCall, hasAutoStarted) {
        if (autoStartCall && !hasAutoStarted) {
            onAutoStartCompleted()
            permissionLauncher.launch(permissionsToRequest)
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Calls",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(com.cockroachcommunity.app.core.designsystem.theme.OnlineEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DIRECT P2P WEBRTC VOICE & VIDEO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = com.cockroachcommunity.app.core.designsystem.theme.OnlineEmerald
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (connectedPeerIdentity != null) {
                FloatingActionButton(
                    onClick = { permissionLauncher.launch(permissionsToRequest) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.AddIcCall, contentDescription = "Start Call")
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
            if (callHistory.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Dimens.Space16))
                    Text(
                        "No call history", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (connectedPeerIdentity != null) {
                        Spacer(modifier = Modifier.height(Dimens.Space8))
                        Text(
                            "Tap the button to call ${connectedPeerIdentity}", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = Dimens.Space16, bottom = 80.dp, start = Dimens.Space16, end = Dimens.Space16),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
                ) {
                    items(callHistory, key = { it.callId }) { history ->
                        CallHistoryItem(history)
                    }
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(history: CallHistoryEntity) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(history.timestamp))
    
    // Status mapping: 0=Missed/Failed, 1=Answered, 2=Rejected
    // Direction mapping: 0=Incoming, 1=Outgoing
    
    val isMissed = history.status == 0 || history.status == 2
    
    val icon = when {
        history.direction == 0 && isMissed -> Icons.Default.CallMissed
        history.direction == 0 -> Icons.Default.CallReceived
        else -> Icons.Default.CallMade
    }
    
    val iconColor = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    val statusText = buildString {
        if (history.direction == 1) append("Outgoing") else append("Incoming")
        when (history.status) {
            0 -> append(if (history.direction == 1) " • Failed/No Answer" else " • Missed")
            1 -> {
                append(" • ")
                val mins = history.durationSeconds / 60
                val secs = history.durationSeconds % 60
                if (mins > 0) append("${mins}m ")
                append("${secs}s")
            }
            2 -> append(" • Rejected")
        }
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.Space16, vertical = Dimens.Space16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayName = com.cockroachcommunity.app.ui.main.formatPeerDisplayName(history.peerDeviceId)
            CockroachCommunityAvatar(name = displayName, size = 48.dp)
            Spacer(modifier = Modifier.width(Dimens.Space16))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isMissed && history.direction == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = iconColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = dateString,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActiveCallUI(viewModel: CallViewModel, callState: CallState) {
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsStateWithLifecycle()
    val connectedPeerIdentity by viewModel.connectedPeerIdentity.collectAsStateWithLifecycle()
    val isVideoCall by viewModel.isVideoCall.collectAsStateWithLifecycle()
    
    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(isVideoCall) }
    var isSpeakerphoneOn by remember { mutableStateOf(isVideoCall) }
    
    var callDurationSeconds by remember { mutableStateOf(0) }
    
    LaunchedEffect(callState) {
        if (callState == CallState.ACTIVE) {
            while(true) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (callState == CallState.ACTIVE) {
            // Remote Video Fullscreen
            if (isVideoCall) {
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            init(viewModel.getEglBaseContext(), null)
                            setEnableHardwareScaler(true)
                            setMirror(false)
                        }
                    },
                    update = { view ->
                        remoteVideoTrack?.addSink(view)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Audio Call Avatar
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Audio Call Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(120.dp)
                    )
                }
            }

            // Local Video Picture-in-Picture
            if (isVideoEnabled) {
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            init(viewModel.getEglBaseContext(), null)
                            setEnableHardwareScaler(true)
                            setMirror(true)
                            setZOrderMediaOverlay(true)
                        }
                    },
                    update = { view ->
                        viewModel.getLocalVideoTrack()?.addSink(view)
                    },
                    modifier = Modifier
                        .size(120.dp, 160.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = Dimens.Space48, end = Dimens.Space16)
                        .clip(RoundedCornerShape(Dimens.Space12))
                )
            }
            
            // Call header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = Dimens.Space48),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = connectedPeerIdentity ?: "Unknown",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                val mins = callDurationSeconds / 60
                val secs = callDurationSeconds % 60
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Call Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimens.Space48),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                FloatingActionButton(
                    onClick = { 
                        isMuted = !isMuted
                        viewModel.setAudioEnabled(!isMuted)
                    },
                    containerColor = if (isMuted) Color.White else Color.DarkGray,
                    contentColor = if (isMuted) Color.Black else Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute")
                }
                
                // End Call
                FloatingActionButton(
                    onClick = { viewModel.endCall() },
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(36.dp))
                }
                
                // Camera Toggle
                FloatingActionButton(
                    onClick = { 
                        isVideoEnabled = !isVideoEnabled
                        viewModel.setVideoEnabled(isVideoEnabled)
                    },
                    containerColor = if (!isVideoEnabled) Color.White else Color.DarkGray,
                    contentColor = if (!isVideoEnabled) Color.Black else Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, contentDescription = "Toggle Video")
                }

                // Speakerphone Toggle
                FloatingActionButton(
                    onClick = { 
                        isSpeakerphoneOn = !isSpeakerphoneOn
                        viewModel.setSpeakerphoneEnabled(isSpeakerphoneOn)
                    },
                    containerColor = if (isSpeakerphoneOn) Color.DarkGray else Color.White,
                    contentColor = if (isSpeakerphoneOn) Color.White else Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(if (isSpeakerphoneOn) Icons.Default.VolumeUp else Icons.Default.Phone, contentDescription = "Toggle Speaker")
                }
            }
            
            // Camera Switch
            if (isVideoEnabled) {
                IconButton(
                    onClick = { viewModel.switchCamera() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 120.dp, end = Dimens.Space24)
                        .background(Color.DarkGray.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
                }
            }
            
        } else if (callState == CallState.INCOMING) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CockroachCommunityAvatar(name = connectedPeerIdentity ?: "Unknown")
                Spacer(modifier = Modifier.height(Dimens.Space24))
                Text(
                    text = connectedPeerIdentity ?: "Unknown",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Incoming video call...",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Dimens.Space64))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.declineCall() },
                        containerColor = Color.Red,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Decline Call", modifier = Modifier.size(36.dp))
                    }
                    FloatingActionButton(
                        onClick = { 
                            // Note: Realistically should check permissions here before accepting
                            viewModel.acceptCall() 
                        },
                        containerColor = Color.Green,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept Call", modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}
